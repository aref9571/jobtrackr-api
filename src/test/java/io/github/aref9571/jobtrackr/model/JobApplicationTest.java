package io.github.aref9571.jobtrackr.model;

import io.github.aref9571.jobtrackr.exception.InvalidApplicationStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobApplicationTest {

    private static final LocalDate APPLIED_DATE = LocalDate.of(2026, 10, 1);

    @Test
    void constructor_validValues_createsAppliedApplication() {
        JobApplication application = new JobApplication(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend");

        assertThat(application.getId()).isNotNull();
        assertThat(application.getCompany()).isEqualTo("ACME Corp");
        assertThat(application.getRole()).isEqualTo("Backend Engineer");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(application.getAppliedDate()).isEqualTo(APPLIED_DATE);
        assertThat(application.getExpectedSalaryCents()).isEqualTo(5_000_000L);
        assertThat(application.getSalaryCurrency()).isEqualTo("EUR");
        assertThat(application.getNotes()).isEqualTo("Referred by a friend");
    }

    @Test
    void constructor_optionalFieldsAbsent_createsApplication() {
        JobApplication application = new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null);

        assertThat(application.getExpectedSalaryCents()).isNull();
        assertThat(application.getSalaryCurrency()).isNull();
        assertThat(application.getNotes()).isNull();
    }

    @Test
    void constructor_twoApplications_getDifferentIds() {
        JobApplication first = applicationWithCompany("ACME Corp");
        JobApplication second = applicationWithCompany("ACME Corp");

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void constructor_companyAndRoleWithSurroundingSpaces_storesTrimmedValues() {
        JobApplication application = new JobApplication("  ACME Corp ", " Backend Engineer  ", APPLIED_DATE, null, null, null);

        assertThat(application.getCompany()).isEqualTo("ACME Corp");
        assertThat(application.getRole()).isEqualTo("Backend Engineer");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void constructor_blankCompany_throwsException(String company) {
        assertThatThrownBy(() -> applicationWithCompany(company))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("company must not be blank");
    }

    @Test
    void constructor_companyOf100Characters_isAccepted() {
        String company = "a".repeat(100);

        JobApplication application = applicationWithCompany(company);

        assertThat(application.getCompany()).isEqualTo(company);
    }

    @Test
    void constructor_companyLongerThan100Characters_throwsException() {
        String company = "a".repeat(101);

        assertThatThrownBy(() -> applicationWithCompany(company))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("company must be at most 100 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void constructor_blankRole_throwsException(String role) {
        assertThatThrownBy(() -> applicationWithRole(role))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role must not be blank");
    }

    @Test
    void constructor_roleOf100Characters_isAccepted() {
        String role = "r".repeat(100);

        JobApplication application = applicationWithRole(role);

        assertThat(application.getRole()).isEqualTo(role);
    }

    @Test
    void constructor_roleLongerThan100Characters_throwsException() {
        String role = "r".repeat(101);

        assertThatThrownBy(() -> applicationWithRole(role))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role must be at most 100 characters");
    }

    @Test
    void constructor_nullAppliedDate_throwsException() {
        assertThatThrownBy(() -> applicationWithAppliedDate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appliedDate must not be null");
    }

    @Test
    void constructor_appliedDateInFuture_throwsException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> applicationWithAppliedDate(tomorrow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appliedDate must not be in the future");
    }

    @Test
    void constructor_appliedDateToday_isAccepted() {
        LocalDate today = LocalDate.now();

        JobApplication application = applicationWithAppliedDate(today);

        assertThat(application.getAppliedDate()).isEqualTo(today);
    }

    @Test
    void constructor_salaryAmountWithoutCurrency_throwsException() {
        assertThatThrownBy(() -> applicationWithSalary(5_000_000L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expectedSalaryCents and salaryCurrency must both be present or both be absent");
    }

    @Test
    void constructor_currencyWithoutSalaryAmount_throwsException() {
        assertThatThrownBy(() -> applicationWithSalary(null, "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expectedSalaryCents and salaryCurrency must both be present or both be absent");
    }

    @Test
    void constructor_negativeSalaryAmount_throwsException() {
        assertThatThrownBy(() -> applicationWithSalary(-1L, "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expectedSalaryCents must not be negative");
    }

    @Test
    void constructor_zeroSalaryAmount_isAccepted() {
        JobApplication application = applicationWithSalary(0L, "EUR");

        assertThat(application.getExpectedSalaryCents()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"eur", "Eur", "EU", "EURO", "E1R", " EUR", "", "ABC"})
    void constructor_invalidCurrency_throwsException(String currency) {
        assertThatThrownBy(() -> applicationWithSalary(5_000_000L, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("salaryCurrency must be a three-letter upper-case ISO 4217 code");
    }

    @ParameterizedTest
    @ValueSource(strings = {"EUR", "USD", "GBP", "SEK"})
    void constructor_validCurrency_isAccepted(String currency) {
        JobApplication application = applicationWithSalary(5_000_000L, currency);

        assertThat(application.getSalaryCurrency()).isEqualTo(currency);
    }

    @Test
    void constructor_notesOf1000Characters_isAccepted() {
        String notes = "n".repeat(1000);

        JobApplication application = applicationWithNotes(notes);

        assertThat(application.getNotes()).isEqualTo(notes);
    }

    @Test
    void constructor_notesLongerThan1000Characters_throwsException() {
        String notes = "n".repeat(1001);

        assertThatThrownBy(() -> applicationWithNotes(notes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("notes must be at most 1000 characters");
    }

    @Test
    void changeStatus_fromAppliedToInterviewing_updatesStatus() {
        JobApplication application = applicationWithCompany("ACME Corp");

        application.changeStatus(ApplicationStatus.INTERVIEWING);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INTERVIEWING);
    }

    @Test
    void changeStatus_fromAppliedToRejected_updatesStatus() {
        JobApplication application = applicationWithCompany("ACME Corp");

        application.changeStatus(ApplicationStatus.REJECTED);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void changeStatus_fromInterviewingToOffer_updatesStatus() {
        JobApplication application = interviewingApplication();

        application.changeStatus(ApplicationStatus.OFFER);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.OFFER);
    }

    @Test
    void changeStatus_fromInterviewingToRejected_updatesStatus() {
        JobApplication application = interviewingApplication();

        application.changeStatus(ApplicationStatus.REJECTED);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"APPLIED", "OFFER"})
    void changeStatus_fromAppliedToNotAllowedStatus_throwsException(ApplicationStatus newStatus) {
        JobApplication application = applicationWithCompany("ACME Corp");

        assertThatThrownBy(() -> application.changeStatus(newStatus))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessage("Cannot move from APPLIED to " + newStatus);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"APPLIED", "INTERVIEWING"})
    void changeStatus_fromInterviewingToNotAllowedStatus_throwsException(ApplicationStatus newStatus) {
        JobApplication application = interviewingApplication();

        assertThatThrownBy(() -> application.changeStatus(newStatus))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessage("Cannot move from INTERVIEWING to " + newStatus);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INTERVIEWING);
    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void changeStatus_fromOffer_throwsException(ApplicationStatus newStatus) {
        JobApplication application = interviewingApplication();
        application.changeStatus(ApplicationStatus.OFFER);

        assertThatThrownBy(() -> application.changeStatus(newStatus))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessage("Cannot change status: application is already in final state OFFER");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.OFFER);
    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void changeStatus_fromRejected_throwsException(ApplicationStatus newStatus) {
        JobApplication application = applicationWithCompany("ACME Corp");
        application.changeStatus(ApplicationStatus.REJECTED);

        assertThatThrownBy(() -> application.changeStatus(newStatus))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessage("Cannot change status: application is already in final state REJECTED");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void changeStatus_null_throwsException() {
        JobApplication application = applicationWithCompany("ACME Corp");

        assertThatThrownBy(() -> application.changeStatus(null))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessage("new status must not be null");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    private static JobApplication applicationWithCompany(String company) {
        return new JobApplication(company, "Backend Engineer", APPLIED_DATE, null, null, null);
    }

    private static JobApplication applicationWithRole(String role) {
        return new JobApplication("ACME Corp", role, APPLIED_DATE, null, null, null);
    }

    private static JobApplication applicationWithAppliedDate(LocalDate appliedDate) {
        return new JobApplication("ACME Corp", "Backend Engineer", appliedDate, null, null, null);
    }

    private static JobApplication applicationWithSalary(Long expectedSalaryCents, String salaryCurrency) {
        return new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, expectedSalaryCents, salaryCurrency, null);
    }

    private static JobApplication applicationWithNotes(String notes) {
        return new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, notes);
    }

    private static JobApplication interviewingApplication() {
        JobApplication application = applicationWithCompany("ACME Corp");
        application.changeStatus(ApplicationStatus.INTERVIEWING);
        return application;
    }
}
