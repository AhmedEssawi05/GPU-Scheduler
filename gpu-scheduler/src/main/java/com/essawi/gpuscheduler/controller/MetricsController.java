package com.essawi.gpuscheduler.controller;

import com.essawi.gpuscheduler.dto.MetricsResponse;
import com.essawi.gpuscheduler.model.GpuNode;
import com.essawi.gpuscheduler.model.Job;
import com.essawi.gpuscheduler.repository.GpuNodeRepository;
import com.essawi.gpuscheduler.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final GpuNodeRepository nodeRepository;
    private final JobRepository jobRepository;

    public MetricsController(GpuNodeRepository nodeRepository, JobRepository jobRepository) {
        this.nodeRepository = nodeRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public MetricsResponse getMetrics() {
        List<GpuNode> nodes = nodeRepository.findAll();
        int totalGpus = nodes.stream().mapToInt(GpuNode::getTotalGpus).sum();
        int allocatedGpus = nodes.stream().mapToInt(GpuNode::getAllocatedGpus).sum();
        double utilizationPercent = totalGpus == 0 ? 0 : (allocatedGpus * 100.0 / totalGpus);

        List<Job> allJobs = jobRepository.findAll();
        long queueDepth = allJobs.stream()
                .filter(j -> j.getStatus() == Job.JobStatus.QUEUED || j.getStatus() == Job.JobStatus.PREEMPTED)
                .count();
        long runningJobs = allJobs.stream()
                .filter(j -> j.getStatus() == Job.JobStatus.RUNNING)
                .count();

        // Average wait time: time between submittedAt and startedAt for running/completed jobs
        double avgWaitSeconds = allJobs.stream()
                .filter(j -> j.getStartedAt() != null && j.getSubmittedAt() != null)
                .mapToLong(j -> Duration.between(j.getSubmittedAt(), j.getStartedAt()).getSeconds())
                .average()
                .orElse(0.0);

        return new MetricsResponse(
                Math.round(utilizationPercent * 10.0) / 10.0,
                totalGpus,
                allocatedGpus,
                (int) queueDepth,
                (int) runningJobs,
                Math.round(avgWaitSeconds * 10.0) / 10.0
        );
    }
}
