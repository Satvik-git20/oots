# OOTS — Object-Oriented Traffic System

**Smart Traffic Monitoring and Management System** — a Java SE prototype built for a Problem-Based
Learning (PBL) course. Fixed-timer signals ignore waiting traffic; this prototype gives each of three
simulated junctions a controller thread that every cycle favours the lane with the highest
priority-weighted demand, so queued lanes get green and **ambulances go first** (weight 10 vs. car 1).
Lane densities are then sent over TCP to a central report server.

Aligned with **SDG 11** (Sustainable Cities and Communities, Target 11.2) and **SDG 9**
(Industry, Innovation and Infrastructure, Target 9.1).

## How it works

```
loop sensors ──▶ Vehicle objects (Car / Bus / Ambulance)
      │                (Modules 1–2: classes, inheritance, polymorphism)
      ▼
Junction: thread-safe waiting list, jagged density array, sensor faults logged
      │                (Module 3: packages, custom exceptions, strings)
      ▼
SignalController threads pick the green lane by weighted demand, release cars
      │                (Module 4: multithreading, synchronization)
      ▼
TCP clients send "J1:4,5" style reports ▶ central server stores them in a TreeMap
                       (Modules 4–5: sockets, I/O streams, generics, collections)
```

Every 12 Readings per junction, about 1 in 10 fails; the sensing loop catches the custom
`SensorFailureException`, logs a `WARN` and keeps running — a sensor fault is never fatal.

## Build & run

Requires JDK 11 or later (tested on Temurin JDK 21).

```bash
javac -Xlint:all -d out SmartTrafficSystem.java    # compile (must produce zero warnings)
java -cp out SmartTrafficSystem 3                  # run with 3 signal cycles (default)
```

One-command scripts (compile + run):

| Windows PowerShell | Windows CMD   |
|--------------------|---------------|
| `powershell -ExecutionPolicy Bypass -File scripts\run.ps1 3` | `scripts\run.bat 3` |

> Windows' default execution policy (`Restricted`) blocks `.ps1` files with *"running scripts is
> disabled on this system"*. Either use the `.bat`, prefix as shown above, or permanently allow
> your own scripts with `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

The number of cycles is a command-line argument (default `3`).

## Verified run (Temurin JDK 21.0.13, Windows 11, cycles = 3, fixed seeds 42–44)

Compiled with `javac -Xlint:all` → **zero warnings**, and executed repeatedly: the derived content
(faults, densities, server report, priority) is identical every run — only the *line order* of the
three controllers' output can vary with thread interleaving.

| Check | Expected | Observed |
|-------|----------|----------|
| T1 — three signal lines per junction | 9 `cycle:` lines (J1/J2/J3 × cycles 1–3) | ✅ 9 of 9 |
| T2 — sensor faults | warnings logged, loop continues, 36 readings | ✅ 5 WARNs (J1 ×3, J2 ×1, J3 ×1), readings = 36 |
| T3 — emergency priority | J3 cycle 1 → green lane 3 with the ambulance | ✅ `Ambulance(V22,L3)` first of 5 released on lane 3 |
| T4 — socket reporting | server receives all 3 densities | ✅ `Server received: {J1=4,5, J2=5,2,4, J3=1,2,3,5}` matches printed arrays |
| T5 — vehicle conservation | arrivals = released + still waiting | ✅ 31 = 30 released + 1 waiting (J3 lane 0, never green) |
| T6 — generic total priority | Car + Bus + Ambulance = 1 + 2 + 10 | ✅ 13 |

Console output of one complete run (fixes seed 42+j; see `javac`/`java` commands above to reproduce):

```
WARN J1: loop fault on lane 0
WARN J1: loop fault on lane 0
WARN J1: loop fault on lane 0
WARN J2: loop fault on lane 2
WARN J3: loop fault on lane 2
Sensor readings processed: 36
J1 lane density [4, 5]
J2 lane density [5, 2, 4]
J3 lane density [1, 2, 3, 5]
J1 cycle 1: green on lane 0, released [Bus(V4,L0), Car(V5,L0), Bus(V8,L0), Car(V9,L0)]
J3 cycle 1: green on lane 3, released [Ambulance(V22,L3), Bus(V23,L3), Car(V25,L3), Car(V27,L3), Car(V28,L3)]
J2 cycle 1: green on lane 0, released [Car(V10,L0), Car(V13,L0), Ambulance(V15,L0), Ambulance(V17,L0), Car(V20,L0)]
J3 cycle 2: green on lane 2, released [Bus(V21,L2), Bus(V24,L2), Car(V31,L2)]
J2 cycle 2: green on lane 1, released [Bus(V16,L1), Ambulance(V18,L1)]
J1 cycle 2: green on lane 1, released [Car(V1,L1), Car(V2,L1), Bus(V3,L1), Car(V6,L1), Car(V7,L1)]
J1 cycle 3: green on lane 0, released []
J2 cycle 3: green on lane 2, released [Car(V11,L2), Bus(V12,L2), Car(V14,L2), Car(V19,L2)]
J3 cycle 3: green on lane 1, released [Bus(V29,L1), Bus(V30,L1)]
Server received: {J1=4,5, J2=5,2,4, J3=1,2,3,5}
Total priority of sample vehicles: 13
```

## Course concept map

| Module | Concepts applied | Where in the code |
|--------|------------------|-------------------|
| 1 | Classes/objects, `this`, control statements, CLI args, garbage collection | `Vehicle`, `Junction`, `main` |
| 2 | Inheritance, overriding, lambdas, jagged arrays | `Car`/`Bus`/`Ambulance`, `Comparator.comparingInt(i -> demand[i])`, `int[][] density` |
| 3 | Packages plan, custom checked exceptions, strings, `StringBuilder` | `SensorFailureException`, `SimulatedSensor`, `sendReport` |
| 4 | Threads, `Runnable`, synchronization, join, daemon, TCP sockets, streams | `SignalController`, `Junction`, `TrafficServer` |
| 5 | Generics with bounded types, collections, iterators, GUI plan | `Sensor<T>`, `totalPriority`, `ArrayList`/`Iterator`, `TreeMap` |

## Repository

- Full design/console evidence: see the PBL final report (`Final_Report_Smart_Traffic_Monitoring.docx`).
- Repository URL: **[insert GitHub repository URL]** — add this after publishing.

## Future work (from the report, Section 15)

- Swing dashboard with `JTable` and manual green-phase override buttons
- Fixed-timer baseline to *measure* (not just expect) waiting-time improvement
- `enum` for signal states, split into `traffic.model / traffic.control / traffic.net / traffic.gui`
- UDP channel for emergency-vehicle beacons; real sensor feeds
- `ExecutorService` thread pool for scaling to many junctions

> Validation covers the simulation only; real traffic improvement is not measured.
