package com.essawi.gpuscheduler.repository;

import com.essawi.gpuscheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(Job.JobStatus status);
    List<Job> findByAssignedNodeId(Long nodeId);
}
