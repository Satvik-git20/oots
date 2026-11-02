import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.IntStream;

// ---- Module 2: abstraction, inheritance, polymorphism ----
abstract class Vehicle {
    private static int created = 0;                       // static counter
    protected final String id;
    protected final int lane;
    Vehicle(int lane) { this.id = "V" + (++created); this.lane = lane; }   // this
    abstract int priority();                              // overridden per type
    int getLane() { return lane; }
    @Override public String toString() {
        return getClass().getSimpleName() + "(" + id + ",L" + lane + ")";
    }
}

class Car extends Vehicle {
    Car(int lane) { super(lane); }                        // super constructor
    @Override int priority() { return 1; }
}

class Bus extends Vehicle {
    Bus(int lane) { super(lane); }
    @Override int priority() { return 2; }
}

class Ambulance extends Vehicle {
    Ambulance(int lane) { super(lane); }
    @Override int priority() { return 10; }               // emergency weight
}

// ---- Module 3: custom exception and generic sensor interface ----
class SensorFailureException extends Exception {
    private static final long serialVersionUID = 1L;
    SensorFailureException(String message) { super(message); }
}

interface Sensor<T> {
    T read() throws SensorFailureException;
}

class SimulatedSensor implements Sensor<Vehicle> {
    private final Random rnd;
    private final int lanes;
    SimulatedSensor(Random rnd, int lanes) { this.rnd = rnd; this.lanes = lanes; }
    @Override public Vehicle read() throws SensorFailureException {
        int lane = rnd.nextInt(lanes);
        if (rnd.nextInt(10) == 0) {                       // about 10% of readings fail
            throw new SensorFailureException("loop fault on lane " + lane);
        }
        int kind = rnd.nextInt(10);
        if (kind == 0) return new Ambulance(lane);
        if (kind <= 3) return new Bus(lane);
        return new Car(lane);
    }
}

// ---- Module 4: shared junction state and signal threads ----
class Junction {
    private final String name;
    private final int lanes;
    private final List<Vehicle> waiting = new ArrayList<>();
    Junction(String name, int lanes) { this.name = name; this.lanes = lanes; }
    String getName() { return name; }
    int getLanes() { return lanes; }
    synchronized void arrive(Vehicle v) { waiting.add(v); }
    synchronized int[] laneDemand() {
        int[] demand = new int[lanes];
        for (Vehicle v : waiting) demand[v.getLane()] += v.priority();
        return demand;
    }
    synchronized List<Vehicle> releaseGreen(int lane) {   // Iterator removes served vehicles
        List<Vehicle> released = new ArrayList<>();
        Iterator<Vehicle> it = waiting.iterator();
        while (it.hasNext()) {
            Vehicle v = it.next();
            if (v.getLane() == lane) { released.add(v); it.remove(); }
        }
        return released;
    }
}

class SignalController implements Runnable {
    private final Junction junction;
    private final int cycles;
    SignalController(Junction junction, int cycles) { this.junction = junction; this.cycles = cycles; }
    @Override public void run() {
        try {
            for (int c = 1; c <= cycles; c++) {
                int[] demand = junction.laneDemand();
                int green = IntStream.range(0, demand.length).boxed()   // lambda
                        .max(Comparator.comparingInt(i -> demand[i])).orElse(0);
                List<Vehicle> released = junction.releaseGreen(green);
                System.out.println(junction.getName() + " cycle " + c + ": green on lane "
                        + green + ", released " + released);
                Thread.sleep(20);                             // simulated green phase
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// ---- Module 4: TCP server (character streams over sockets) ----
class TrafficServer implements Runnable {
    private final ServerSocket socket;
    private final int expected;
    private final Map<String, String> reports = new TreeMap<>();   // collection
    TrafficServer(ServerSocket socket, int expected) { this.socket = socket; this.expected = expected; }
    @Override public void run() {
        for (int k = 0; k < expected; k++) {
            try (Socket s = socket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                String[] parts = in.readLine().split(":");    // String handling
                reports.put(parts[0], parts[1]);
            } catch (IOException e) {
                System.out.println("server error: " + e.getMessage());
            }
        }
    }
    Map<String, String> getReports() { return reports; }
}

public class SmartTrafficSystem {
    // Generic method with an upper-bounded type parameter
    static <T extends Vehicle> int totalPriority(Collection<T> vehicles) {
        int sum = 0;
        for (T v : vehicles) sum += v.priority();
        return sum;
    }

    static void sendReport(int port, String junction, int[] density) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < density.length; i++) sb.append(i > 0 ? "," : "").append(density[i]);
        try (Socket s = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            out.println(junction + ":" + sb);
        }
    }

    public static void main(String[] args) throws Exception {
        int cycles = args.length > 0 ? Integer.parseInt(args[0]) : 3;   // command-line argument
        int[][] density = { new int[2], new int[3], new int[4] };      // jagged array
        Junction[] junctions = new Junction[density.length];
        for (int j = 0; j < junctions.length; j++) {
            junctions[j] = new Junction("J" + (j + 1), density[j].length);
        }
        Thread monitor = new Thread(() -> {                           // daemon thread
            try { while (true) Thread.sleep(1000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "monitor");
        monitor.setDaemon(true);
        monitor.setPriority(Thread.MIN_PRIORITY);
        monitor.start();
        ServerSocket serverSocket = new ServerSocket(0);              // ephemeral port
        TrafficServer server = new TrafficServer(serverSocket, junctions.length);
        Thread serverThread = new Thread(server, "server");
        serverThread.start();
        int readings = 0;                                             // sensing phase
        for (int j = 0; j < junctions.length; j++) {
            SimulatedSensor sensor = new SimulatedSensor(new Random(42 + j), junctions[j].getLanes());
            for (int n = 0; n < 12; n++) {
                try {
                    Vehicle v = sensor.read();
                    junctions[j].arrive(v);
                    density[j][v.getLane()]++;
                } catch (SensorFailureException e) {
                    System.out.println("WARN " + junctions[j].getName() + ": " + e.getMessage());
                } finally {
                    readings++;
                }
            }
        }
        System.out.println("Sensor readings processed: " + readings);
        for (int j = 0; j < junctions.length; j++) {
            System.out.println(junctions[j].getName() + " lane density " + Arrays.toString(density[j]));
        }
        List<Thread> controllers = new ArrayList<>();                 // one thread per junction
        for (Junction j : junctions) {
            Thread t = new Thread(new SignalController(j, cycles), "ctrl-" + j.getName());
            controllers.add(t);
            t.start();
        }
        for (Thread t : controllers) t.join();
        for (int j = 0; j < junctions.length; j++) {                  // socket clients
            sendReport(serverSocket.getLocalPort(), junctions[j].getName(), density[j]);
        }
        serverThread.join();
        serverSocket.close();
        System.out.println("Server received: " + server.getReports());
        List<Vehicle> sample = List.of(new Car(0), new Bus(1), new Ambulance(2));
        System.out.println("Total priority of sample vehicles: " + totalPriority(sample));
    }
}
