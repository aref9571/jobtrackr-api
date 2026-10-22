package io.github.aref9571.jobtrackr.controller;

import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ApplicationSearchCriteria;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.PageResponse;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.exception.ApplicationNotFoundException;
import io.github.aref9571.jobtrackr.exception.DuplicateApplicationException;
import io.github.aref9571.jobtrackr.exception.InvalidApplicationStateException;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void createApplication_blankCompany_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON.replace("\"ACME Corp\"", "\"  \"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/applications"))
                .andExpect(jsonPath("$.errors.company").value("must not be blank"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_emptyBody_returns400WithAllRequiredFields() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.company").value("must not be blank"))
                .andExpect(jsonPath("$.errors.role").value("must not be blank"))
                .andExpect(jsonPath("$.errors.appliedDate").value("must not be null"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_tooLongValues_returns400WithFieldErrors() throws Exception {
        String json = """
                {
                  "company": "%s",
                  "role": "%s",
                  "appliedDate": "2026-10-01",
                  "notes": "%s"
                }
                """.formatted("c".repeat(101), "r".repeat(101), "n".repeat(1001));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.company").value("size must be between 0 and 100"))
                .andExpect(jsonPath("$.errors.role").value("size must be between 0 and 100"))
                .andExpect(jsonPath("$.errors.notes").value("size must be between 0 and 1000"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_futureAppliedDate_returns400WithFieldError() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON.replace("2026-10-01", tomorrow)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.appliedDate").value("must be a date in the past or in the present"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_negativeSalary_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON.replace("5000000", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.expectedSalaryCents").value("must be greater than or equal to 0"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_salaryWithoutCurrency_returns400WithFieldError() throws Exception {
        String json = """
                {"company": "ACME Corp", "role": "Backend Engineer", "appliedDate": "2026-10-01",
                 "expectedSalaryCents": 5000000}
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salaryConsistent")
                        .value("expectedSalaryCents and salaryCurrency must both be present or both be absent"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_currencyWithoutSalary_returns400WithFieldError() throws Exception {
        String json = """
                {"company": "ACME Corp", "role": "Backend Engineer", "appliedDate": "2026-10-01",
                 "salaryCurrency": "EUR"}
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salaryConsistent")
                        .value("expectedSalaryCents and salaryCurrency must both be present or both be absent"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_lowerCaseCurrency_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON.replace("\"EUR\"", "\"eur\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salaryCurrency")
                        .value("must be a three-letter upper-case currency code"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_unknownCurrencyCode_returns400ProblemDetail() throws Exception {
        String message = "salaryCurrency must be a three-letter upper-case ISO 4217 code";
        when(applicationService.createApplication(new CreateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "XYZ", "Referred by a friend")))
                .thenThrow(new IllegalArgumentException(message));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON.replace("\"EUR\"", "\"XYZ\"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Test
    void createApplication_malformedJson_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\": \"ACME Corp\","))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Failed to read request"))
                .andExpect(content().string(not(containsString("Exception"))));

        verifyNoInteractions(applicationService);
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
    void getApplication_invalidUuid_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/applications/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to convert 'id' with value: 'not-a-uuid'"))
                .andExpect(jsonPath("$.instance").value("/api/applications/not-a-uuid"))
                .andExpect(content().string(not(containsString("Exception"))));

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_uniqueIndexViolation_returns409ProblemDetail() throws Exception {
        when(applicationService.createApplication(new CreateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend")))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Request conflicts with existing data"))
                .andExpect(content().string(not(containsString("unique constraint"))));
    }

    @Test
    void getApplication_unexpectedError_returns500WithoutInternalDetails() throws Exception {
        when(applicationService.getApplication(ID))
                .thenThrow(new IllegalStateException("connection to jdbc:postgresql://db failed"));

        mockMvc.perform(get("/api/applications/{id}", ID))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("postgresql"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void searchApplications_applicationsExist_returns200WithPage() throws Exception {
        when(applicationService.searchApplications(any(ApplicationSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(response(ApplicationStatus.APPLIED)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.content[0].company").value("ACME Corp"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void searchApplications_noApplications_returns200WithEmptyPage() throws Exception {
        when(applicationService.searchApplications(any(ApplicationSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0}
                        """));
    }

    @Test
    void searchApplications_filterParameters_passesCriteriaToService() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .param("status", "INTERVIEWING")
                        .param("company", "acme")
                        .param("appliedFrom", "2026-09-01")
                        .param("appliedTo", "2026-09-30"))
                .andExpect(status().isOk());

        verify(applicationService).searchApplications(
                eq(new ApplicationSearchCriteria(ApplicationStatus.INTERVIEWING, "acme",
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))),
                any(Pageable.class));
    }

    @Test
    void searchApplications_pagingParameters_passesPageableToService() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "appliedDate,desc"))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("appliedDate"));
    }

    @Test
    void searchApplications_noParameters_usesDefaultPagingAndNewestFirst() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("createdAt"));
    }

    @Test
    void searchApplications_sizeAboveMaximum_isCappedAt100() throws Exception {
        mockMvc.perform(get("/api/applications").param("size", "500"))
                .andExpect(status().isOk());

        assertThat(capturePageable().getPageSize()).isEqualTo(100);
    }

    @Test
    void searchApplications_unknownSortField_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/applications").param("sort", "salary,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("Cannot sort by 'salary'. Allowed fields: appliedDate, company, status, createdAt"))
                .andExpect(jsonPath("$.instance").value("/api/applications"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void searchApplications_existingButNotSortableField_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/applications").param("sort", "notes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Cannot sort by 'notes'. Allowed fields: appliedDate, company, status, createdAt"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void searchApplications_oneOfTwoSortFieldsNotAllowed_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .param("sort", "company,asc")
                        .param("sort", "role,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Cannot sort by 'role'. Allowed fields: appliedDate, company, status, createdAt"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void searchApplications_appliedFromAfterAppliedTo_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .param("appliedFrom", "2026-10-01")
                        .param("appliedTo", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("appliedFrom must not be after appliedTo"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void searchApplications_unknownStatus_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/applications").param("status", "HIRED"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Failed to convert 'status' with value: 'HIRED'"))
                .andExpect(content().string(not(containsString("ApplicationStatus"))));

        verifyNoInteractions(applicationService);
    }

    @Test
    void searchApplications_invalidDate_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/applications").param("appliedFrom", "01.09.2026"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Failed to convert 'appliedFrom' with value: '01.09.2026'"))
                .andExpect(content().string(not(containsString("Exception"))));

        verifyNoInteractions(applicationService);
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
    void updateApplication_invalidRequest_returns400WithFieldErrors() throws Exception {
        String json = """
                {"company": "", "role": "Backend Engineer", "appliedDate": "2026-10-01",
                 "expectedSalaryCents": 5000000, "salaryCurrency": "euro"}
                """;

        mockMvc.perform(put("/api/applications/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.company").value("must not be blank"))
                .andExpect(jsonPath("$.errors.salaryCurrency")
                        .value("must be a three-letter upper-case currency code"));

        verifyNoInteractions(applicationService);
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
    void changeStatus_unknownStatus_returns400ProblemDetail() throws Exception {
        mockMvc.perform(patch("/api/applications/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "HIRED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to read request"))
                .andExpect(content().string(not(containsString("ApplicationStatus"))));

        verifyNoInteractions(applicationService);
    }

    @Test
    void changeStatus_missingStatus_returns400WithFieldError() throws Exception {
        mockMvc.perform(patch("/api/applications/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").value("must not be null"));

        verifyNoInteractions(applicationService);
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

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(applicationService).searchApplications(any(ApplicationSearchCriteria.class), pageableCaptor.capture());
        return pageableCaptor.getValue();
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
