package io.github.aref9571.jobtrackr.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @CsvSource({
            "APPLIED, APPLIED, false",
            "APPLIED, INTERVIEWING, true",
            "APPLIED, OFFER, false",
            "APPLIED, REJECTED, true",
            "INTERVIEWING, APPLIED, false",
            "INTERVIEWING, INTERVIEWING, false",
            "INTERVIEWING, OFFER, true",
            "INTERVIEWING, REJECTED, true",
            "OFFER, APPLIED, false",
            "OFFER, INTERVIEWING, false",
            "OFFER, OFFER, false",
            "OFFER, REJECTED, false",
            "REJECTED, APPLIED, false",
            "REJECTED, INTERVIEWING, false",
            "REJECTED, OFFER, false",
            "REJECTED, REJECTED, false"
    })
    void canMoveTo_followsTransitionTable(ApplicationStatus from, ApplicationStatus to, boolean expected) {
        boolean allowed = from.canMoveTo(to);

        assertThat(allowed).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void canMoveTo_null_returnsFalse(ApplicationStatus from) {
        boolean allowed = from.canMoveTo(null);

        assertThat(allowed).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"APPLIED", "INTERVIEWING"})
    void activeStatuses_areActiveAndNotFinal(ApplicationStatus status) {
        assertThat(status.isActive()).isTrue();
        assertThat(status.isFinal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"OFFER", "REJECTED"})
    void finalStatuses_areFinalAndNotActive(ApplicationStatus status) {
        assertThat(status.isFinal()).isTrue();
        assertThat(status.isActive()).isFalse();
    }
}
