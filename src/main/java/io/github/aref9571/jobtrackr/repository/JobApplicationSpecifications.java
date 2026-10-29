package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public final class JobApplicationSpecifications {

    private static final char ESCAPE_CHARACTER = '\\';

    private JobApplicationSpecifications() {
    }

    public static Specification<JobApplication> hasOwner(UUID ownerId) {
        return (root, query, builder) -> builder.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<JobApplication> hasStatus(ApplicationStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<JobApplication> companyContains(String company) {
        if (company == null || company.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escapeLikeWildcards(company.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("company")), pattern, ESCAPE_CHARACTER);
    }

    public static Specification<JobApplication> appliedOnOrAfter(LocalDate date) {
        if (date == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("appliedDate"), date);
    }

    public static Specification<JobApplication> appliedOnOrBefore(LocalDate date) {
        if (date == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("appliedDate"), date);
    }

    private static String escapeLikeWildcards(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
