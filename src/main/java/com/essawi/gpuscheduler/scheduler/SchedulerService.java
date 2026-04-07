package com.essawi.gpuscheduler.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final BinPackingScheduler scheduler;

    public SchedulerService(BinPackingScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Scheduled(fixedRate = 2000)
    @Transactional
    public void schedulingLoop() {
        scheduler.completeFinishedJobs();
        scheduler.runAllocationCycle();
    }
}
