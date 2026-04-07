package com.essawi.gpuscheduler;

import com.essawi.gpuscheduler.model.Job;
import com.essawi.gpuscheduler.repository.GpuNodeRepository;
import com.essawi.gpuscheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private GpuNodeRepository nodeRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void testCreateJobReturnsQueuedJob() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"test-job","gpuRequest":2,"memoryRequestGb":16,"priority":"HIGH"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-job"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.gpuRequest").value(2))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void testGetAllJobsReturnsList() throws Exception {
        jobRepository.save(new Job("job-a", 1, 8, Job.Priority.LOW));
        jobRepository.save(new Job("job-b", 2, 16, Job.Priority.MEDIUM));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("job-a", "job-b")));
    }

    @Test
    void testGetJobByIdReturnsJob() throws Exception {
        Job saved = jobRepository.save(new Job("specific-job", 4, 32, Job.Priority.CRITICAL));

        mockMvc.perform(get("/api/jobs/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("specific-job"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }

    @Test
    void testGetJobByIdNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/jobs/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCancelQueuedJobSetsStatusCancelled() throws Exception {
        Job saved = jobRepository.save(new Job("cancel-me", 2, 8, Job.Priority.LOW));

        mockMvc.perform(delete("/api/jobs/" + saved.getId()))
                .andExpect(status().isOk());

        Job cancelled = jobRepository.findById(saved.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(cancelled.getStatus())
                .isEqualTo(Job.JobStatus.CANCELLED);
    }

    @Test
    void testCancelNonExistentJobReturns404() throws Exception {
        mockMvc.perform(delete("/api/jobs/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetNodesReturnsSeededNodes() throws Exception {
        mockMvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testMetricsEndpointReturnsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGpus").isNumber())
                .andExpect(jsonPath("$.allocatedGpus").isNumber())
                .andExpect(jsonPath("$.clusterUtilizationPercent").isNumber())
                .andExpect(jsonPath("$.queueDepth").isNumber())
                .andExpect(jsonPath("$.runningJobs").isNumber())
                .andExpect(jsonPath("$.avgWaitTimeSeconds").isNumber());
    }

    @Test
    void testJobDurationIsBetween30And120() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"duration-test","gpuRequest":1,"memoryRequestGb":4,"priority":"LOW"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationSeconds").value(
                        allOf(greaterThanOrEqualTo(30), lessThanOrEqualTo(120))));
    }
}
