package com.insurance.billing.controller;

import com.insurance.billing.model.dto.request.PaymentAttemptRequest;
import com.insurance.billing.model.dto.response.DelinquentPoliciesResponse;
import com.insurance.billing.model.dto.response.PaymentAttemptResponse;
import com.insurance.billing.model.dto.response.PremiumScheduleResponse;
import com.insurance.billing.model.dto.response.RetryResponse;
import com.insurance.billing.service.BillingEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Billing Engine microservice.
 * * Base path :  /api/v1/billing
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  GET  /policies/{policyId}/premium-schedule  → 200 PremiumScheduleResponse  │
 * │  POST /payments/attempts                     → 201 PaymentAttemptResponse   │
 * │  GET  /policies/delinquent                   → 200 DelinquentPoliciesResponse│
 * │  POST /payments/retry/{policyId}             → 202 RetryResponse            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing Engine", description = "Policy premium schedule, payment processing and delinquency management")
public class BillingEngineController {

    private static final Logger log = LoggerFactory.getLogger(BillingEngineController.class);

    private final BillingEngineService service;

    public BillingEngineController(BillingEngineService service) {
        this.service = service;
    }

    // ── 1. Premium Schedule ──────────────────────────────────────────────────

    @Operation(
        summary     = "Retrieve premium schedule for a policy",
        description = "Returns the full instalment schedule for the given policy, "
                    + "including due dates, amounts and payment status for each instalment."
    )

    @GetMapping("/policies/{policyId}/premium-schedule")
    public ResponseEntity<PremiumScheduleResponse> getPremiumSchedule(
            @Parameter(description = "Policy identifier",required = true)
            @PathVariable String policyId) {

        log.info("GET premium-schedule policyId={}", policyId);
        return ResponseEntity.ok(service.getPremiumSchedule(policyId));
    }

    // ── 2. Record Payment Attempt ────────────────────────────────────────────

    @Operation(
        summary     = "Record a payment attempt",
        description = """
            Submits a charge against a policy and records the outcome.

            **Simulation rule:** set `paymentMethodToken` to any value ending in `fail`
            (e.g. `tok_card_fail`) to force a `FAILED` result. Any other value produces `SUCCESS`.

            A `nextRetryScheduledAt` timestamp is returned for failed attempts, indicating
            when the back-off retry will fire (+1 d / +3 d / +7 d ladder).
            """
    )

    @PostMapping("/payments/attempts")
    public ResponseEntity<PaymentAttemptResponse> recordPaymentAttempt(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Payment attempt details",
                required    = true,
                content     = @Content(
                    schema = @Schema(implementation = PaymentAttemptRequest.class)

                    ))
            @Valid @RequestBody PaymentAttemptRequest request) {

        log.info("POST payment attempt policyId={} amount={}", request.policyId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordPaymentAttempt(request));
    }

    // ── 3. Delinquent Policies ───────────────────────────────────────────────

    @Operation(
        summary     = "List delinquent policies",
        description = "Returns all policies that have missed at least one payment, "
                    + "ordered by severity (WARNING → GRACE → LAPSED)."
    )

    @GetMapping("/policies/delinquent")
    public ResponseEntity<DelinquentPoliciesResponse> getDelinquentPolicies() {

        log.info("GET delinquent policies");
        return ResponseEntity.ok(service.getDelinquentPolicies());
    }

    // ── 4. Trigger Retry ─────────────────────────────────────────────────────

    @Operation(
        summary     = "Trigger a retry for a failed payment",
        description = """
            Schedules a retry job for the most recent failed payment on the given policy.

            Back-off ladder:
            | Prior attempts | Next retry delay |
            |:--------------:|:----------------:|
            | 0 – 1          | +1 day           |
            | 2              | +3 days          |
            | 3+             | 422 – escalate   |
            """
    )

    @PostMapping("/payments/retry/{policyId}")
    public ResponseEntity<RetryResponse> triggerRetry(
            @Parameter(description = "Policy identifier", required = true)
            @PathVariable String policyId) {

        log.info("POST retry policyId={}", policyId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.triggerRetry(policyId));
    }
}
