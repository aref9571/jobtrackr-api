package io.github.aref9571.jobtrackr.controller;

import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.service.ApplicationService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

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

    @GetMapping("/{id}")
    public ApplicationResponse getApplication(@PathVariable UUID id) {
        return applicationService.getApplication(id);
    }

    @GetMapping
    public List<ApplicationResponse> getAllApplications() {
        return applicationService.getAllApplications();
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApplication(@PathVariable UUID id) {
        applicationService.deleteApplication(id);
    }
}
