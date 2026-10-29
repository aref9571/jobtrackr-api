package io.github.aref9571.jobtrackr.service;

import io.github.aref9571.jobtrackr.dto.AdminStatisticsResponse;
import io.github.aref9571.jobtrackr.repository.JobApplicationRepository;
import io.github.aref9571.jobtrackr.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;

    public AdminService(UserRepository userRepository, JobApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatisticsResponse getStatistics() {
        return new AdminStatisticsResponse(userRepository.count(), applicationRepository.count());
    }
}
