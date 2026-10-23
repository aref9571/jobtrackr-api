package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.dto.CompanyCount;
import io.github.aref9571.jobtrackr.dto.StatusCount;
import io.github.aref9571.jobtrackr.model.JobApplication;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID>,
        JpaSpecificationExecutor<JobApplication> {

    boolean existsByCompanyIgnoreCaseAndRoleIgnoreCase(String company, String role);

    @Query("""
            select new io.github.aref9571.jobtrackr.dto.StatusCount(a.status, count(a))
            from JobApplication a
            group by a.status""")
    List<StatusCount> countPerStatus();

    @Query("""
            select new io.github.aref9571.jobtrackr.dto.CompanyCount(lower(a.company), count(a))
            from JobApplication a
            group by lower(a.company)
            order by count(a) desc, lower(a.company) asc""")
    List<CompanyCount> findTopCompanies(Limit limit);
}
