package io.github.aref9571.jobtrackr.service;

import io.github.aref9571.jobtrackr.dto.AdminStatisticsResponse;
import io.github.aref9571.jobtrackr.repository.JobApplicationRepository;
import io.github.aref9571.jobtrackr.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobApplicationRepository applicationRepository;

    @InjectMocks
    private AdminService service;

    @Test
    void getStatistics_returnsTotalUsersAndTotalApplications() {
        when(userRepository.count()).thenReturn(3L);
        when(applicationRepository.count()).thenReturn(7L);

        AdminStatisticsResponse statistics = service.getStatistics();

        assertThat(statistics).isEqualTo(new AdminStatisticsResponse(3, 7));
    }
}
