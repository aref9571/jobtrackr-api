package io.github.aref9571.jobtrackr.controller;

import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.exception.ApplicationNotFoundException;
import io.github.aref9571.jobtrackr.exception.DuplicateApplicationException;
import io.github.aref9571.jobtrackr.exception.InvalidApplicationStateException;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    private static final UUID ID = UUID.fromString("3f2b8c1e-6d4a-4f7b-9a1e-2c5d8e7f6a01");
    private static final LocalDate APPLIED_DATE = LocalDate.of(2026, 10, 1);
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T09:30:00Z");

    private static final String APPLICATION_JSON = """
            {
              "company": "ACME Corp",
              "role": "Backend Engineer",
              "appliedDate": "2026-10-01",
              "expectedSalaryCents": 5000000,
              "salaryCurrency": "EUR",
              "notes": "Referred by a friend"
            }
            """;

    private final MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    ApplicationControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createApplication_validRequest_returns201WithLocationAndBody() throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend");
        when(applicationService.createApplication(request)).thenReturn(response(ApplicationStatus.APPLIED));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/applications/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.company").value("ACME Corp"))
                .andExpect(jsonPath("$.role").value("Backend Engineer"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedDate").value("2026-10-01"))
                .andExpect(jsonPath("$.expectedSalaryCents").value(5_000_000))
                .andExpect(jsonPath("$.salaryCurrency").value("EUR"))
                .andExpect(jsonPath("$.notes").value("Referred by a friend"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-01T09:30:00Z"));

        verify(applicationService).createApplication(request);
    }

    @Test
    void createApplication_duplicate_returns409ProblemDetail() throws Exception {
        String message = "Application for Backend Engineer at ACME Corp already exists";
        when(applicationService.createApplication(new CreateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend")))
                .thenThrow(new DuplicateApplicationException(message));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value(message))
                .andExpect(jsonPath("$.instance").value("/api/applications"));
    }

    @Test
    void getApplication_existingId_returns200WithApplication() throws Exception {
        when(applicationService.getApplication(ID)).thenReturn(response(ApplicationStatus.INTERVIEWING));

        mockMvc.perform(get("/api/applications/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.status").value("INTERVIEWING"));
    }

    @Test
    void getApplication_unknownId_returns404ProblemDetail() throws Exception {
        String message = "No application found with id: " + ID;
        when(applicationService.getApplication(ID)).thenThrow(new ApplicationNotFoundException(message));

        mockMvc.perform(get("/api/applications/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value(message))
                .andExpect(jsonPath("$.instance").value("/api/applications/" + ID));
    }

    @Test
    void getAllApplications_applicationsExist_returns200WithList() throws Exception {
        when(applicationService.getAllApplications()).thenReturn(List.of(response(ApplicationStatus.APPLIED)));

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ID.toString()))
                .andExpect(jsonPath("$[0].company").value("ACME Corp"));
    }

    @Test
    void getAllApplications_noApplications_returns200WithEmptyList() throws Exception {
        when(applicationService.getAllApplications()).thenReturn(List.of());

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void updateApplication_existingId_returns200WithUpdatedApplication() throws Exception {
        UpdateApplicationRequest request = new UpdateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend");
        when(applicationService.updateApplication(ID, request)).thenReturn(response(ApplicationStatus.APPLIED));

        mockMvc.perform(put("/api/applications/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.notes").value("Referred by a friend"));

        verify(applicationService).updateApplication(ID, request);
    }

    @Test
    void updateApplication_unknownId_returns404ProblemDetail() throws Exception {
        String message = "No application found with id: " + ID;
        when(applicationService.updateApplication(ID, new UpdateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend")))
                .thenThrow(new ApplicationNotFoundException(message));

        mockMvc.perform(put("/api/applications/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Test
    void updateApplication_duplicate_returns409ProblemDetail() throws Exception {
        String message = "Application for Backend Engineer at ACME Corp already exists";
        when(applicationService.updateApplication(ID, new UpdateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend")))
                .thenThrow(new DuplicateApplicationException(message));

        mockMvc.perform(put("/api/applications/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Test
    void changeStatus_allowedChange_returns200WithNewStatus() throws Exception {
        ChangeStatusRequest request = new ChangeStatusRequest(ApplicationStatus.INTERVIEWING);
        when(applicationService.changeStatus(ID, request)).thenReturn(response(ApplicationStatus.INTERVIEWING));

        mockMvc.perform(patch("/api/applications/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "INTERVIEWING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEWING"));

        verify(applicationService).changeStatus(ID, request);
    }

    @Test
    void changeStatus_forbiddenChange_returns409ProblemDetail() throws Exception {
        String message = "Cannot move from APPLIED to OFFER";
        when(applicationService.changeStatus(ID, new ChangeStatusRequest(ApplicationStatus.OFFER)))
                .thenThrow(new InvalidApplicationStateException(message));

        mockMvc.perform(patch("/api/applications/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "OFFER"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value(message))
                .andExpect(jsonPath("$.instance").value("/api/applications/" + ID + "/status"));
    }

    @Test
    void changeStatus_unknownId_returns404ProblemDetail() throws Exception {
        String message = "No application found with id: " + ID;
        when(applicationService.changeStatus(ID, new ChangeStatusRequest(ApplicationStatus.INTERVIEWING)))
                .thenThrow(new ApplicationNotFoundException(message));

        mockMvc.perform(patch("/api/applications/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "INTERVIEWING"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Test
    void deleteApplication_existingId_returns204WithoutBody() throws Exception {
        mockMvc.perform(delete("/api/applications/{id}", ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(applicationService).deleteApplication(ID);
    }

    @Test
    void deleteApplication_unknownId_returns404ProblemDetail() throws Exception {
        String message = "No application found with id: " + ID;
        doThrow(new ApplicationNotFoundException(message)).when(applicationService).deleteApplication(ID);

        mockMvc.perform(delete("/api/applications/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(message));
    }

    private static ApplicationResponse response(ApplicationStatus status) {
        return new ApplicationResponse(
                ID,
                "ACME Corp",
                "Backend Engineer",
                status,
                APPLIED_DATE,
                5_000_000L,
                "EUR",
                "Referred by a friend",
                CREATED_AT,
                CREATED_AT);
    }
}
