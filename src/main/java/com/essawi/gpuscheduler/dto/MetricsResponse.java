package com.essawi.gpuscheduler.dto;

public class MetricsResponse {
    private double clusterUtilizationPercent;
    private int totalGpus;
    private int allocatedGpus;
    private int queueDepth;
    private int runningJobs;
    private double avgWaitTimeSeconds;

    public MetricsResponse(double clusterUtilizationPercent, int totalGpus, int allocatedGpus,
                           int queueDepth, int runningJobs, double avgWaitTimeSeconds) {
        this.clusterUtilizationPercent = clusterUtilizationPercent;
        this.totalGpus = totalGpus;
        this.allocatedGpus = allocatedGpus;
        this.queueDepth = queueDepth;
        this.runningJobs = runningJobs;
        this.avgWaitTimeSeconds = avgWaitTimeSeconds;
    }

    public double getClusterUtilizationPercent() { return clusterUtilizationPercent; }
    public int getTotalGpus() { return totalGpus; }
    public int getAllocatedGpus() { return allocatedGpus; }
    public int getQueueDepth() { return queueDepth; }
    public int getRunningJobs() { return runningJobs; }
    public double getAvgWaitTimeSeconds() { return avgWaitTimeSeconds; }
}
