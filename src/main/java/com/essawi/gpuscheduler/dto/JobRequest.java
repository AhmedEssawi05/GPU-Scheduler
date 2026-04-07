package com.essawi.gpuscheduler.dto;

import com.essawi.gpuscheduler.model.Job;

public class JobRequest {
    private String name;
    private int gpuRequest;
    private int memoryRequestGb;
    private Job.Priority priority;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getGpuRequest() { return gpuRequest; }
    public void setGpuRequest(int gpuRequest) { this.gpuRequest = gpuRequest; }
    public int getMemoryRequestGb() { return memoryRequestGb; }
    public void setMemoryRequestGb(int memoryRequestGb) { this.memoryRequestGb = memoryRequestGb; }
    public Job.Priority getPriority() { return priority; }
    public void setPriority(Job.Priority priority) { this.priority = priority; }
}
