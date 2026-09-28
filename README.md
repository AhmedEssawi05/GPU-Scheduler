# GPU Scheduler

A distributed GPU job scheduler built with Java and Spring Boot: bin-packing placement, priority queuing, and preemption across a simulated multi-node GPU cluster, with a real-time dashboard for cluster utilization, queue depth, and scheduling efficiency.

The project lives in [`gpu-scheduler/`](./gpu-scheduler) — see [`gpu-scheduler/README.md`](./gpu-scheduler/README.md) for the full architecture, API reference, and H2 console details. This file is just the fastest path to a running local demo.

## Quick start

**Prerequisites:** Java 21+ (the project uses the Maven wrapper, so no separate Maven install is required).

```bash
cd gpu-scheduler
./mvnw spring-boot:run
```

Wait for `Started GpuSchedulerApplication` in the logs, then open **http://localhost:8080** for the live dashboard. A 4-node, 24-GPU cluster is seeded automatically on startup — no setup or seed script needed.

## Try it

With the app running, submit jobs from another terminal and watch them get bin-packed live on the dashboard. This sequence fills the entire 24-GPU cluster with low-priority jobs, then submits a CRITICAL job that can only fit by preempting one of them:

```bash
# Fill the whole cluster (24 GPUs total across Node-A/B/C/D) with LOW-priority jobs
curl -X POST http://localhost:8080/api/jobs -H "Content-Type: application/json" \
  -d '{"name":"filler-1","gpuRequest":8,"memoryRequestGb":64,"priority":"LOW"}'
curl -X POST http://localhost:8080/api/jobs -H "Content-Type: application/json" \
  -d '{"name":"filler-2","gpuRequest":8,"memoryRequestGb":64,"priority":"LOW"}'
curl -X POST http://localhost:8080/api/jobs -H "Content-Type: application/json" \
  -d '{"name":"filler-3","gpuRequest":4,"memoryRequestGb":32,"priority":"LOW"}'
curl -X POST http://localhost:8080/api/jobs -H "Content-Type: application/json" \
  -d '{"name":"filler-4","gpuRequest":4,"memoryRequestGb":32,"priority":"LOW"}'

# Wait ~2s for the scheduler loop to place all four, then submit a CRITICAL job
# that has nowhere to go except by preempting one of the running LOW jobs
curl -X POST http://localhost:8080/api/jobs -H "Content-Type: application/json" \
  -d '{"name":"urgent-inference","gpuRequest":4,"memoryRequestGb":32,"priority":"CRITICAL"}'
```

Within a couple seconds, `GET http://localhost:8080/api/jobs` shows one of the filler jobs flip to `PREEMPTED` and `urgent-inference` move to `RUNNING` in its place — the same thing you'll see happen live on the dashboard and in `/api/nodes` and `/api/metrics`. (Verified locally: this exact sequence preempted `filler-3` to place the CRITICAL job on Node-C.)
