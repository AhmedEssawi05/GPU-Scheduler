package com.essawi.gpuscheduler.scheduler;

import com.essawi.gpuscheduler.model.GpuNode;
import com.essawi.gpuscheduler.model.Job;
import com.essawi.gpuscheduler.repository.GpuNodeRepository;
import com.essawi.gpuscheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class BinPackingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BinPackingScheduler.class);

    private final JobRepository jobRepository;
    private final GpuNodeRepository nodeRepository;

    public BinPackingScheduler(JobRepository jobRepository, GpuNodeRepository nodeRepository) {
        this.jobRepository = jobRepository;
        this.nodeRepository = nodeRepository;
    }

    public void completeFinishedJobs() {
        List<Job> running = jobRepository.findByStatus(Job.JobStatus.RUNNING);
        LocalDateTime now = LocalDateTime.now();
        for (Job job : running) {
            if (job.getStartedAt() != null &&
                    now.isAfter(job.getStartedAt().plusSeconds(job.getDurationSeconds()))) {
                freeNodeResources(job);
                job.setStatus(Job.JobStatus.COMPLETED);
                job.setCompletedAt(now);
                jobRepository.save(job);
                log.info("Job {} completed", job.getName());
            }
        }
    }

    public void runAllocationCycle() {
        List<Job> queued = jobRepository.findByStatus(Job.JobStatus.QUEUED);
        if (queued.isEmpty()) return;

        // Sort by priority tier DESC, then submittedAt ASC (first-fit-decreasing within priority)
        queued.sort(Comparator
                .comparingInt((Job j) -> j.getPriority().getTier()).reversed()
                .thenComparing(Job::getSubmittedAt));

        List<GpuNode> nodes = nodeRepository.findAll();

        for (Job job : queued) {
            boolean placed = tryPlace(job, nodes);
            if (!placed && job.getPriority() == Job.Priority.CRITICAL) {
                placed = tryPreemptAndPlace(job, nodes);
            }
            if (!placed) {
                log.debug("Job {} could not be placed (gpus={}, mem={})",
                        job.getName(), job.getGpuRequest(), job.getMemoryRequestGb());
            }
        }
    }

    private boolean tryPlace(Job job, List<GpuNode> nodes) {
        // First-fit-decreasing: nodes sorted by available GPU desc
        List<GpuNode> sorted = nodes.stream()
                .filter(n -> n.getStatus() == GpuNode.NodeStatus.AVAILABLE)
                .sorted(Comparator.comparingInt(GpuNode::freeGpus).reversed())
                .toList();

        for (GpuNode node : sorted) {
            if (node.canFit(job.getGpuRequest(), job.getMemoryRequestGb())) {
                assign(job, node);
                return true;
            }
        }
        return false;
    }

    private boolean tryPreemptAndPlace(Job job, List<GpuNode> nodes) {
        // Find lowest-priority RUNNING job to preempt
        List<Job> running = jobRepository.findByStatus(Job.JobStatus.RUNNING);
        Optional<Job> victim = running.stream()
                .filter(j -> j.getPriority().getTier() < job.getPriority().getTier())
                .min(Comparator.comparingInt(j -> j.getPriority().getTier()));

        if (victim.isEmpty()) return false;

        Job preempted = victim.get();
        log.info("Preempting job {} (priority {}) for CRITICAL job {}",
                preempted.getName(), preempted.getPriority(), job.getName());

        freeNodeResources(preempted);
        preempted.setStatus(Job.JobStatus.PREEMPTED);
        preempted.setAssignedNodeId(null);
        preempted.setStartedAt(null);
        jobRepository.save(preempted);

        // Refresh node list after freeing resources
        List<GpuNode> refreshed = nodeRepository.findAll();
        return tryPlace(job, refreshed);
    }

    private void assign(Job job, GpuNode node) {
        node.allocate(job.getGpuRequest(), job.getMemoryRequestGb());
        nodeRepository.save(node);

        job.setStatus(Job.JobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        job.setAssignedNodeId(node.getId());
        jobRepository.save(job);

        log.info("Job {} assigned to node {} (gpus={}, mem={})",
                job.getName(), node.getName(), job.getGpuRequest(), job.getMemoryRequestGb());
    }

    private void freeNodeResources(Job job) {
        if (job.getAssignedNodeId() == null) return;
        nodeRepository.findById(job.getAssignedNodeId()).ifPresent(node -> {
            node.free(job.getGpuRequest(), job.getMemoryRequestGb());
            nodeRepository.save(node);
        });
    }
}
