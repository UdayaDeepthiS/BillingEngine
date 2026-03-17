package com.insurance.billing.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request body for recording a payment attempt")
public record PaymentAttemptRequest(

        @Schema(description = "Policy to charge", example = "POL-1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "policyId is required")
        String policyId,

        @Schema(description = "Amount to charge in GBP", example = "320.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "Tokenised payment method. End with 'fail' to simulate a declined charge.",
                example = "tok_visa_4242", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "paymentMethodToken is required")
        String paymentMethodToken,

        @Schema(description = "Optional client-supplied idempotency key", example = "idem-abc-001")
        String idempotencyKey
) {}
