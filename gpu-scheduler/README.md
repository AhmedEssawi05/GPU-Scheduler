# GPU Job Scheduler

A distributed GPU job scheduling system built with Spring Boot. It simulates a cluster of GPU nodes and schedules workloads across them using a **bin-packing** algorithm with **priority-based preemption**.

## Architecture

```
src/main/java/com/essawi/gpuscheduler/
├── GpuSchedulerApplication.java   # Entry point + seeds 4 GPU nodes on startup
├── model/
│   ├── GpuNode.java               # GPU node (name, total/allocated GPUs & memory)
│   └── Job.java                   # Job (name, GPU/memory request, priority, status)
├── repository/
│   ├── GpuNodeRepository.java
│   └── JobRepository.java
├── scheduler/
│   ├── BinPackingScheduler.java   # Core scheduling logic
│   └── SchedulerService.java      # Runs scheduling loop every 2 seconds
├── controller/
│   ├── JobController.java         # POST/GET/DELETE /api/jobs
│   ├── NodeController.java        # GET /api/nodes
│   └── MetricsController.java     # GET /api/metrics
└── dto/
    ├── JobRequest.java
    └── MetricsResponse.java
```

**Storage:** In-memory H2 database (resets on restart).

**Scheduler loop (every 2s):**
1. Completes any running jobs whose duration has elapsed
2. Picks up queued jobs sorted by priority (CRITICAL > HIGH > MEDIUM > LOW), then by submission time
3. Places each job on the node with the most free GPUs that can fit it (first-fit-decreasing bin packing)
4. If a CRITICAL job can't be placed, it preempts the lowest-priority running job

**Seeded cluster on startup:**
| Node   | GPUs | Memory |
|--------|------|--------|
| Node-A | 8    | 64 GB  |
| Node-B | 8    | 64 GB  |
| Node-C | 4    | 32 GB  |
| Node-D | 4    | 32 GB  |
| **Total** | **24** | **192 GB** |

## Prerequisites

- Java 21+
- Maven (or use the included `./mvnw` wrapper)

## Running

```bash
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**.

## Dashboard

Open **http://localhost:8080** in your browser for the live dashboard:

- Cluster utilization & queue depth charts (auto-refresh every 3s)
- GPU and memory usage per node
- Job submission form
- Job table with cancel buttons

## API

### Jobs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/jobs` | Submit a new job |
| `GET` | `/api/jobs` | List all jobs |
| `GET` | `/api/jobs/{id}` | Get a specific job |
| `DELETE` | `/api/jobs/{id}` | Cancel a job |

**Submit a job:**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"name":"training-run-1","gpuRequest":2,"memoryRequestGb":16,"priority":"HIGH"}'
```

Priority values: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

### Nodes

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/nodes` | List all nodes and their resource usage |

### Metrics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/metrics` | Cluster-wide metrics (utilization %, queue depth, avg wait time) |

## Tests

```bash
./mvnw test
```

## H2 Console

The H2 web console is available at **http://localhost:8080/h2-console** while the app is running.

- JDBC URL: `jdbc:h2:mem:gpuscheduler`
- Username: `sa`
- Password: *(empty)*
