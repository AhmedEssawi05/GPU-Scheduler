package com.essawi.gpuscheduler;

import com.essawi.gpuscheduler.model.GpuNode;
import com.essawi.gpuscheduler.repository.GpuNodeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GpuSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GpuSchedulerApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedData(GpuNodeRepository nodeRepository) {
        return args -> {
            nodeRepository.save(new GpuNode("Node-A", 8, 64));
            nodeRepository.save(new GpuNode("Node-B", 8, 64));
            nodeRepository.save(new GpuNode("Node-C", 4, 32));
            nodeRepository.save(new GpuNode("Node-D", 4, 32));
            System.out.println("Seeded 4 GPU nodes. Total: 24 GPUs, 192GB memory.");
        };
    }
}
