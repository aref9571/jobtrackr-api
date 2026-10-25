package io.github.aref9571.jobtrackr.controller;

import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ApplicationSearchCriteria;
import io.github.aref9571.jobtrackr.dto.ApplicationStatisticsResponse;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.PageResponse;
import io.github.aref9571.jobtrackr.dto.StatusChangeResponse;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.exception.InvalidSearchParameterException;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.service.ApplicationService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final List<String> SORTABLE_FIELDS = List.of("appliedDate", "company", "status", "createdAt");

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/stats")
    public ApplicationStatisticsResponse getStatistics() {
        return applicationService.getStatistics();
    }

    @GetMapping("/{id}")
    public ApplicationResponse getApplication(@PathVariable UUID id) {
        return applicationService.getApplication(id);
    }

    @GetMapping
    public PageResponse<ApplicationResponse> searchApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedTo,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        requireValidDateRange(appliedFrom, appliedTo);
        requireSortableFields(pageable.getSort());
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria(status, company, appliedFrom, appliedTo);
        return applicationService.searchApplications(criteria, pageable);
    }

    @PutMapping("/{id}")
    public ApplicationResponse updateApplication(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateApplicationRequest request) {
        return applicationService.updateApplication(id, request);
    }

    @PatchMapping("/{id}/status")
    public ApplicationResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return applicationService.changeStatus(id, request);
    }

    @GetMapping("/{id}/history")
    public List<StatusChangeResponse> getStatusHistory(@PathVariable UUID id) {
        return applicationService.getStatusHistory(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApplication(@PathVariable UUID id) {
        applicationService.deleteApplication(id);
    }

    private static void requireValidDateRange(LocalDate appliedFrom, LocalDate appliedTo) {
        if (appliedFrom != null && appliedTo != null && appliedFrom.isAfter(appliedTo)) {
            throw new InvalidSearchParameterException("appliedFrom must not be after appliedTo");
        }
    }

    private static void requireSortableFields(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidSearchParameterException("Cannot sort by '" + order.getProperty()
                        + "'. Allowed fields: " + String.join(", ", SORTABLE_FIELDS));
            }
        }
    }
}
