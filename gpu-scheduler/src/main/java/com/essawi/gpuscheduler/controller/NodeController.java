package com.essawi.gpuscheduler.controller;

import com.essawi.gpuscheduler.model.GpuNode;
import com.essawi.gpuscheduler.repository.GpuNodeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final GpuNodeRepository nodeRepository;

    public NodeController(GpuNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @GetMapping
    public List<GpuNode> getAllNodes() {
        return nodeRepository.findAll();
    }
}
