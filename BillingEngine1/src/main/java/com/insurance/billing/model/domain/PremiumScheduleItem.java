package com.insurance.billing.model.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A single instalment within a policy's premium schedule")
public record PremiumScheduleItem(

        @Schema(description = "Instalment number", example = "1")
        int installmentNumber,

        @Schema(description = "Payment due date", example = "2025-01-01")
        LocalDate dueDate,

        @Schema(description = "Instalment amount", example = "320.00")
        BigDecimal amount,

        @Schema(description = "Instalment status", example = "PAID",
                allowableValues = {"PENDING", "PAID", "OVERDUE"})
        String status
) {}
