package com.insurance.billing.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmation that a retry job has been queued")
public record RetryResponse(

        @Schema(description = "Policy for which the retry was scheduled", example = "POL-1001")
        String policyId,

        @Schema(description = "Unique retry job identifier", example = "JOB-I9J0K1L2")
        String retryJobId,

        @Schema(description = "ISO-8601 timestamp when the retry will be attempted",
                example = "2025-08-11T14:25:00.000Z")
        String scheduledAt,

        @Schema(description = "Attempt number this retry represents", example = "2")
        int attemptNumber,

        @Schema(description = "Human-readable status message",
                example = "Retry job queued – attempt #2 in 1 day(s)")
        String message
) {}
