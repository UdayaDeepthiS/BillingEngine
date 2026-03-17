package com.insurance.billing.model.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A policy that has missed one or more premium payments")
public record DelinquentPolicy(

        @Schema(description = "Unique policy identifier", example = "POL-1001")
        String policyId,

        @Schema(description = "Policyholder identifier", example = "HLD-501")
        String holderId,

        @Schema(description = "Policyholder full name", example = "Alice Johnson")
        String holderName,

        @Schema(description = "Current delinquency state")
        DelinquencyStatus delinquencyStatus,

        @Schema(description = "Number of calendar days past the due date", example = "45")
        int daysPastDue,

        @Schema(description = "Total outstanding balance in GBP", example = "320.00")
        BigDecimal outstandingBalance,

        @Schema(description = "Number of failed payment attempts", example = "2")
        int failedAttempts,

        @Schema(description = "Date of the most recent payment attempt", example = "2025-08-07")
        LocalDate lastAttemptDate,

        @Schema(description = "Date the next retry is scheduled", example = "2025-08-14")
        LocalDate nextRetryDate
) {}
