package com.essawi.gpuscheduler.repository;

import com.essawi.gpuscheduler.model.GpuNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GpuNodeRepository extends JpaRepository<GpuNode, Long> {
}
