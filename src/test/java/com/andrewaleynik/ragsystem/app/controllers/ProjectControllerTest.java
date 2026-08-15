package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.request.project.ProjectCreateRequest;
import com.andrewaleynik.ragsystem.app.dto.request.project.ProjectDeleteRequest;
import com.andrewaleynik.ragsystem.app.dto.request.project.ProjectRetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.request.project.ProjectUpdateRequest;
import com.andrewaleynik.ragsystem.app.dto.response.DocumentListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.ProjectListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.ProjectResponse;
import com.andrewaleynik.ragsystem.app.dto.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.app.services.ProjectCrudService;
import com.andrewaleynik.ragsystem.app.services.core.ProjectIndexService;
import com.andrewaleynik.ragsystem.app.services.core.ProjectSyncService;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectCrudService projectCrudService;

    @MockitoBean
    private ProjectSyncService projectSyncService;

    @MockitoBean
    private ProjectIndexService projectIndexService;

    @Test
    void createProject_shouldReturn201() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "https://github.com/test/repo.git", "main", "test-project", ProjectType.GIT
        );
        ProjectResponse response = ProjectResponse.builder().id(1L).name("test-project").build();
        when(projectCrudService.createProject(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("test-project"));
    }

    @Test
    void getAllProjects_shouldReturn200() throws Exception {
        ProjectListResponse response = ProjectListResponse.builder()
                .count(1)
                .projects(List.of(ProjectResponse.builder().id(1L).name("p1").build()))
                .build();
        when(projectCrudService.retrieveProjects()).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.projects[0].name").value("p1"));
    }

    @Test
    void getProjectById_shouldReturn200() throws Exception {
        ProjectResponse response = ProjectResponse.builder().id(1L).name("test-project").build();
        when(projectCrudService.retrieveProject(any(ProjectRetrieveRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("test-project"));
    }

    @Test
    void updateProject_shouldReturn200() throws Exception {
        ProjectResponse response = ProjectResponse.builder().id(1L).name("updated").build();
        when(projectCrudService.updateProject(any(ProjectUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"updated","defaultBranch":"main"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated"));

        verify(projectCrudService).updateProject(new ProjectUpdateRequest(1L, "updated", "main"));
    }

    @Test
    void deleteProject_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/1"))
                .andExpect(status().isNoContent());

        verify(projectCrudService).deleteProject(new ProjectDeleteRequest(1L));
    }

    @Test
    void activateProject_shouldReturn204() throws Exception {
        mockMvc.perform(get("/api/v1/projects/1/activate"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivateProject_shouldReturn204() throws Exception {
        mockMvc.perform(get("/api/v1/projects/1/deactivate"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getProjectDocuments_shouldReturn200() throws Exception {
        DocumentListResponse response = new DocumentListResponse(0, 0, List.of());
        when(projectCrudService.getProjectDocuments(1L, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void syncProject_shouldReturn202WhenQueued() throws Exception {
        when(projectSyncService.tryStartSyncProject(any()))
                .thenReturn(new TaskStatusResponse(TaskStatus.QUEUED));

        mockMvc.perform(post("/api/v1/projects/1/sync"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void syncProject_shouldReturn409WhenRejected() throws Exception {
        when(projectSyncService.tryStartSyncProject(any()))
                .thenReturn(new TaskStatusResponse(TaskStatus.REJECTED));

        mockMvc.perform(post("/api/v1/projects/1/sync"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void indexProject_shouldReturn202WhenQueued() throws Exception {
        when(projectIndexService.tryStartIndexProject(any()))
                .thenReturn(new TaskStatusResponse(TaskStatus.QUEUED));

        mockMvc.perform(post("/api/v1/projects/1/index"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void indexProject_shouldReturn409WhenRejected() throws Exception {
        when(projectIndexService.tryStartIndexProject(any()))
                .thenReturn(new TaskStatusResponse(TaskStatus.REJECTED));

        mockMvc.perform(post("/api/v1/projects/1/index"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
