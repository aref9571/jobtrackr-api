package io.github.aref9571.jobtrackr.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateApplicationRequest(
        @NotBlank @Size(max = 100) String company,
        @NotBlank @Size(max = 100) String role,
        @NotNull @PastOrPresent LocalDate appliedDate,
        @PositiveOrZero Long expectedSalaryCents,
        @Pattern(regexp = "[A-Z]{3}", message = "must be a three-letter upper-case currency code")
        String salaryCurrency,
        @Size(max = 1000) String notes) {

    @AssertTrue(message = "expectedSalaryCents and salaryCurrency must both be present or both be absent")
    public boolean isSalaryConsistent() {
        return (expectedSalaryCents == null) == (salaryCurrency == null);
    }
}
