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
| `.\scripts\run.ps1 3` | `scripts\run.bat 3` |

The number of cycles is a command-line argument (default `3`).

## Verified run (Temurin JDK 21, cycles = 3, fixed seeds 42–44)

The simulation is deterministic for a fixed cycle count (per-junction seeds `42 + j`); only the
*line order* of the three controllers' console output varies with thread interleaving.

| Check | Expected | Observed |
|-------|----------|----------|
| T1 — three signal lines per junction | 9 `cycle:` lines | ✅ Pass |
| T2 — sensor faults | warnings logged, loop continues, 36 readings | ✅ Pass |
| T3 — emergency priority | J3 cycle 1 → green lane 3 (contains ambulance) | ✅ Pass |
| T4 — socket reporting | server receives all 3 densities, matching printed arrays | ✅ Pass |
| T5 — vehicle conservation | arrivals = released + still waiting (31 = 30 + 1) | ✅ Pass |
| T6 — generic total priority | Car + Bus + Ambulance = 1 + 2 + 10 = 13 | ✅ Pass |

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
