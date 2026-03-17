package com.insurance.billing.model.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** Internal domain record – not exposed in the API. */
public record PaymentAttempt(
        String attemptId,
        String policyId,
        BigDecimal amount,
        String paymentMethodToken,
        PaymentStatus status,
        String gatewayReference,
        String failureReason,
        Instant attemptedAt,
        int attemptNumber
) {}
