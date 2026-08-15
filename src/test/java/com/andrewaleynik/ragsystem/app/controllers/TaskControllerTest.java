package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.services.core.TaskService;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskId;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void getTasks_shouldReturn200() throws Exception {
        Task task = Task.builder()
                .id(new TaskId(Project.class, 1L))
                .type(TaskType.SYNCING)
                .status(TaskStatus.QUEUED)
                .updatedAt(LocalDateTime.parse("2025-01-01T00:00:00"))
                .build();
        when(taskService.getTasks()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.tasks[0].id").value("Project_1"))
                .andExpect(jsonPath("$.tasks[0].type").value("SYNCING"))
                .andExpect(jsonPath("$.tasks[0].status").value("QUEUED"));
    }
}
