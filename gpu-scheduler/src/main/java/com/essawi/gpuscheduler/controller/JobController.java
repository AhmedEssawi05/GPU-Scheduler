package com.essawi.gpuscheduler.controller;

import com.essawi.gpuscheduler.dto.JobRequest;
import com.essawi.gpuscheduler.model.Job;
import com.essawi.gpuscheduler.repository.GpuNodeRepository;
import com.essawi.gpuscheduler.repository.JobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final GpuNodeRepository nodeRepository;

    public JobController(JobRepository jobRepository, GpuNodeRepository nodeRepository) {
        this.jobRepository = jobRepository;
        this.nodeRepository = nodeRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Job> createJob(@RequestBody JobRequest request) {
        Job job = new Job(
                request.getName(),
                request.getGpuRequest(),
                request.getMemoryRequestGb(),
                request.getPriority()
        );
        return ResponseEntity.ok(jobRepository.save(job));
    }

    @GetMapping
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> cancelJob(@PathVariable Long id) {
        if (!jobRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Job job = jobRepository.findById(id).get();
        if (job.getStatus() == Job.JobStatus.RUNNING && job.getAssignedNodeId() != null) {
            nodeRepository.findById(job.getAssignedNodeId()).ifPresent(node -> {
                node.free(job.getGpuRequest(), job.getMemoryRequestGb());
                nodeRepository.save(node);
            });
        }
        job.setStatus(Job.JobStatus.CANCELLED);
        jobRepository.save(job);
        return ResponseEntity.<Void>ok().build();
    }
}
