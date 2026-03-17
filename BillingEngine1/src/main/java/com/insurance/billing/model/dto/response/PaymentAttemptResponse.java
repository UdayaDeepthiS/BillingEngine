package com.insurance.billing.model.dto.response;

import com.insurance.billing.model.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Result of a recorded payment attempt")
public record PaymentAttemptResponse(

        @Schema(description = "Unique attempt identifier", example = "ATT-A1B2C3D4")
        String attemptId,

        @Schema(description = "Policy charged", example = "POL-1001")
        String policyId,

        @Schema(description = "Amount charged in GBP", example = "320.00")
        BigDecimal amount,

        @Schema(description = "Outcome of the charge")
        PaymentStatus status,

        @Schema(description = "Gateway transaction reference (null when FAILED)", example = "GW-9F8E7D6C")
        String gatewayReference,

        @Schema(description = "Human-readable failure reason (null when SUCCESS)", example = "Insufficient funds")
        String failureReason,

        @Schema(description = "UTC timestamp of the attempt", example = "2025-08-10T14:22:01.123Z")
        Instant attemptedAt,

        @Schema(description = "Running attempt count for this policy", example = "1")
        int attemptNumber,

        @Schema(description = "ISO-8601 timestamp of the next scheduled retry (null when SUCCESS)",
                example = "2025-08-11T14:22:01.123Z")
        String nextRetryScheduledAt
) {}
