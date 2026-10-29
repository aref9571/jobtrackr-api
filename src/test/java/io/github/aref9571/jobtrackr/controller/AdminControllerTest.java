package io.github.aref9571.jobtrackr.controller;

import io.github.aref9571.jobtrackr.config.SecurityConfig;
import io.github.aref9571.jobtrackr.dto.AdminStatisticsResponse;
import io.github.aref9571.jobtrackr.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = UserDetailsService.class)
class AdminControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    AdminControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void getStatistics_adminRole_returns200WithTotals() throws Exception {
        when(adminService.getStatistics()).thenReturn(new AdminStatisticsResponse(3, 7));

        mockMvc.perform(get("/api/admin/stats")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.totalApplications").value(7));
    }

    @Test
    void getStatistics_userRole_returns403WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }

    @Test
    void getStatistics_withoutToken_returns401WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminService);
    }
}
