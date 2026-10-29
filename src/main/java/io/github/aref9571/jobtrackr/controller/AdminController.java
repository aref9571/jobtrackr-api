package io.github.aref9571.jobtrackr.controller;

import io.github.aref9571.jobtrackr.config.SecurityConfig;
import io.github.aref9571.jobtrackr.dto.AdminStatisticsResponse;
import io.github.aref9571.jobtrackr.service.AdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = SecurityConfig.BEARER_AUTH)
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public AdminStatisticsResponse getStatistics() {
        return adminService.getStatistics();
    }
}
