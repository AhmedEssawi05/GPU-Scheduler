package com.essawi.gpuscheduler.model;

import jakarta.persistence.*;

@Entity
@Table(name = "gpu_nodes")
public class GpuNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int totalGpus;
    private int allocatedGpus;
    private int totalMemoryGb;
    private int allocatedMemoryGb;

    @Enumerated(EnumType.STRING)
    private NodeStatus status;

    public enum NodeStatus {
        AVAILABLE, FULL
    }

    public GpuNode() {}

    public GpuNode(String name, int totalGpus, int totalMemoryGb) {
        this.name = name;
        this.totalGpus = totalGpus;
        this.totalMemoryGb = totalMemoryGb;
        this.allocatedGpus = 0;
        this.allocatedMemoryGb = 0;
        this.status = NodeStatus.AVAILABLE;
    }

    public int freeGpus() {
        return totalGpus - allocatedGpus;
    }

    public int freeMemoryGb() {
        return totalMemoryGb - allocatedMemoryGb;
    }

    public boolean canFit(int gpus, int memoryGb) {
        return freeGpus() >= gpus && freeMemoryGb() >= memoryGb;
    }

    public void allocate(int gpus, int memoryGb) {
        this.allocatedGpus += gpus;
        this.allocatedMemoryGb += memoryGb;
        updateStatus();
    }

    public void free(int gpus, int memoryGb) {
        this.allocatedGpus = Math.max(0, this.allocatedGpus - gpus);
        this.allocatedMemoryGb = Math.max(0, this.allocatedMemoryGb - memoryGb);
        updateStatus();
    }

    private void updateStatus() {
        this.status = (freeGpus() == 0 || freeMemoryGb() == 0) ? NodeStatus.FULL : NodeStatus.AVAILABLE;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTotalGpus() { return totalGpus; }
    public void setTotalGpus(int totalGpus) { this.totalGpus = totalGpus; }
    public int getAllocatedGpus() { return allocatedGpus; }
    public void setAllocatedGpus(int allocatedGpus) { this.allocatedGpus = allocatedGpus; }
    public int getTotalMemoryGb() { return totalMemoryGb; }
    public void setTotalMemoryGb(int totalMemoryGb) { this.totalMemoryGb = totalMemoryGb; }
    public int getAllocatedMemoryGb() { return allocatedMemoryGb; }
    public void setAllocatedMemoryGb(int allocatedMemoryGb) { this.allocatedMemoryGb = allocatedMemoryGb; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
}
