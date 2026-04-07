package com.essawi.gpuscheduler;

import com.essawi.gpuscheduler.model.GpuNode;
import com.essawi.gpuscheduler.model.Job;
import com.essawi.gpuscheduler.repository.GpuNodeRepository;
import com.essawi.gpuscheduler.repository.JobRepository;
import com.essawi.gpuscheduler.scheduler.BinPackingScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BinPackingSchedulerTest {

    @Autowired
    private BinPackingScheduler scheduler;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private GpuNodeRepository nodeRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        nodeRepository.deleteAll();
    }

    @Test
    void testBinPackingAssignsJobToNodeWithEnoughResources() {
        GpuNode node = nodeRepository.save(new GpuNode("Test-Node", 8, 64));

        Job job = jobRepository.save(new Job("ml-train", 2, 16, Job.Priority.MEDIUM));

        scheduler.runAllocationCycle();

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Job.JobStatus.RUNNING);
        assertThat(updated.getAssignedNodeId()).isEqualTo(node.getId());

        GpuNode updatedNode = nodeRepository.findById(node.getId()).orElseThrow();
        assertThat(updatedNode.getAllocatedGpus()).isEqualTo(2);
        assertThat(updatedNode.getAllocatedMemoryGb()).isEqualTo(16);
    }

    @Test
    void testJobCannotBePlacedWhenNodeLacksResources() {
        nodeRepository.save(new GpuNode("Small-Node", 2, 16));

        Job job = jobRepository.save(new Job("large-job", 4, 32, Job.Priority.MEDIUM));

        scheduler.runAllocationCycle();

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Job.JobStatus.QUEUED);
    }

    @Test
    void testHighPriorityJobScheduledBeforeLowPriority() {
        // Single node with only 2 GPUs — only one job can fit
        GpuNode node = nodeRepository.save(new GpuNode("Single-Node", 2, 16));

        Job lowJob = new Job("low-job", 2, 8, Job.Priority.LOW);
        lowJob.setSubmittedAt(LocalDateTime.now().minusSeconds(10));
        lowJob = jobRepository.save(lowJob);

        Job highJob = new Job("high-job", 2, 8, Job.Priority.HIGH);
        highJob.setSubmittedAt(LocalDateTime.now());
        highJob = jobRepository.save(highJob);

        scheduler.runAllocationCycle();

        Job updatedHigh = jobRepository.findById(highJob.getId()).orElseThrow();
        Job updatedLow = jobRepository.findById(lowJob.getId()).orElseThrow();

        assertThat(updatedHigh.getStatus()).isEqualTo(Job.JobStatus.RUNNING);
        assertThat(updatedLow.getStatus()).isEqualTo(Job.JobStatus.QUEUED);
    }

    @Test
    void testCriticalJobPreemptsLowPriorityJob() {
        GpuNode node = nodeRepository.save(new GpuNode("Node", 2, 16));

        // Fill the node with a LOW job
        Job lowJob = new Job("low-job", 2, 8, Job.Priority.LOW);
        lowJob = jobRepository.save(lowJob);
        scheduler.runAllocationCycle();

        Job updatedLow = jobRepository.findById(lowJob.getId()).orElseThrow();
        assertThat(updatedLow.getStatus()).isEqualTo(Job.JobStatus.RUNNING);

        // Submit a CRITICAL job that needs the same resources
        Job criticalJob = new Job("critical-job", 2, 8, Job.Priority.CRITICAL);
        criticalJob = jobRepository.save(criticalJob);

        scheduler.runAllocationCycle();

        Job updatedCritical = jobRepository.findById(criticalJob.getId()).orElseThrow();
        Job preempted = jobRepository.findById(lowJob.getId()).orElseThrow();

        assertThat(updatedCritical.getStatus()).isEqualTo(Job.JobStatus.RUNNING);
        assertThat(preempted.getStatus()).isEqualTo(Job.JobStatus.PREEMPTED);
    }

    @Test
    void testJobCompletionFreesResources() {
        GpuNode node = nodeRepository.save(new GpuNode("Node", 4, 32));

        Job job = new Job("short-job", 2, 8, Job.Priority.MEDIUM);
        job = jobRepository.save(job);

        // Manually set it as running with an expired start time
        job.setStatus(Job.JobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now().minusSeconds(200)); // already expired
        job.setDurationSeconds(30);
        job.setAssignedNodeId(node.getId());
        node.allocate(2, 8);
        nodeRepository.save(node);
        jobRepository.save(job);

        scheduler.completeFinishedJobs();

        Job completed = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(Job.JobStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();

        GpuNode updatedNode = nodeRepository.findById(node.getId()).orElseThrow();
        assertThat(updatedNode.getAllocatedGpus()).isEqualTo(0);
        assertThat(updatedNode.getAllocatedMemoryGb()).isEqualTo(0);
    }

    @Test
    void testMultipleJobsBinPackedAcrossNodes() {
        GpuNode nodeA = nodeRepository.save(new GpuNode("Node-A", 4, 32));
        GpuNode nodeB = nodeRepository.save(new GpuNode("Node-B", 4, 32));

        // Submit 2 jobs that together require all GPUs across 2 nodes
        Job job1 = jobRepository.save(new Job("job-1", 4, 16, Job.Priority.MEDIUM));
        Job job2 = jobRepository.save(new Job("job-2", 4, 16, Job.Priority.MEDIUM));

        scheduler.runAllocationCycle();

        Job updated1 = jobRepository.findById(job1.getId()).orElseThrow();
        Job updated2 = jobRepository.findById(job2.getId()).orElseThrow();

        assertThat(updated1.getStatus()).isEqualTo(Job.JobStatus.RUNNING);
        assertThat(updated2.getStatus()).isEqualTo(Job.JobStatus.RUNNING);
        // Both jobs placed on different nodes
        assertThat(updated1.getAssignedNodeId()).isNotEqualTo(updated2.getAssignedNodeId());
    }
}
