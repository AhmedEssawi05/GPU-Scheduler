package com.essawi.gpuscheduler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Random;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int gpuRequest;
    private int memoryRequestGb;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private LocalDateTime submittedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long assignedNodeId;
    private int durationSeconds;

    public enum Priority {
        LOW(0), MEDIUM(1), HIGH(2), CRITICAL(3);

        private final int tier;
        Priority(int tier) { this.tier = tier; }
        public int getTier() { return tier; }
    }

    public enum JobStatus {
        QUEUED, RUNNING, PREEMPTED, COMPLETED, CANCELLED
    }

    public Job() {}

    public Job(String name, int gpuRequest, int memoryRequestGb, Priority priority) {
        this.name = name;
        this.gpuRequest = gpuRequest;
        this.memoryRequestGb = memoryRequestGb;
        this.priority = priority;
        this.status = JobStatus.QUEUED;
        this.submittedAt = LocalDateTime.now();
        this.durationSeconds = 30 + new Random().nextInt(91); // 30-120
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getGpuRequest() { return gpuRequest; }
    public void setGpuRequest(int gpuRequest) { this.gpuRequest = gpuRequest; }
    public int getMemoryRequestGb() { return memoryRequestGb; }
    public void setMemoryRequestGb(int memoryRequestGb) { this.memoryRequestGb = memoryRequestGb; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getAssignedNodeId() { return assignedNodeId; }
    public void setAssignedNodeId(Long assignedNodeId) { this.assignedNodeId = assignedNodeId; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
}
