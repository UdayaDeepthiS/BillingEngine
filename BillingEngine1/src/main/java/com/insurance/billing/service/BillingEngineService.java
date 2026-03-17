package com.insurance.billing.service;

import com.insurance.billing.data.CannedDataStore;
import com.insurance.billing.model.domain.DelinquentPolicy;
import com.insurance.billing.model.domain.PaymentAttempt;
import com.insurance.billing.model.domain.PaymentStatus;
import com.insurance.billing.model.dto.request.PaymentAttemptRequest;
import com.insurance.billing.model.dto.response.DelinquentPoliciesResponse;
import com.insurance.billing.model.dto.response.PaymentAttemptResponse;
import com.insurance.billing.model.dto.response.PremiumScheduleResponse;
import com.insurance.billing.model.dto.response.RetryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core billing service.
 * All state is held in-memory (resets on restart) – no database required.
 * Retry back-off ladder
 * ─────────────────────
 *   Attempt 1 → attempt 2 :  +1 day
 *   Attempt 2 → attempt 3 :  +3 days
 *   Attempt 3 → attempt 4 :  +7 days
 *   More than 3 attempts   :  policy escalated – 422 returned
 */
@Service
public class BillingEngineService {

    private static final Logger log = LoggerFactory.getLogger(BillingEngineService.class);

    private static final int[]  RETRY_BACKOFF_DAYS = {1, 3, 7};
    private static final int    MAX_RETRY_ATTEMPTS = 3;

    private final CannedDataStore store;

    // Per-policy running attempt counter
    private final Map<String, AtomicInteger> attemptCounters = new ConcurrentHashMap<>();
    // Most recent attempt per policy (used by retry logic)
    private final Map<String, PaymentAttempt> lastAttempts   = new ConcurrentHashMap<>();

    public BillingEngineService(CannedDataStore store) {
        this.store = store;
    }

    // ── 1. Premium schedule ──────────────────────────────────────────

    public PremiumScheduleResponse getPremiumSchedule(String policyId) {
        log.debug("Fetching schedule for policy={}", policyId);

        if (!CannedDataStore.KNOWN_POLICIES.contains(policyId)) {
            throw new PolicyNotFoundException(policyId);
        }

        var schedule = store.getSchedule(policyId);
        var meta     = store.getPolicyMeta(policyId);

        return new PremiumScheduleResponse(
                policyId,
                meta[0],
                meta[1],
                new BigDecimal(meta[3]),
                meta[2],
                schedule
        );
    }

    // ── 2. Record payment attempt ────────────────────────────────────

    public PaymentAttemptResponse recordPaymentAttempt(PaymentAttemptRequest req) {
        log.debug("Recording payment attempt policy={} amount={}", req.policyId(), req.amount());

        if (!CannedDataStore.KNOWN_POLICIES.contains(req.policyId())) {
            throw new PolicyNotFoundException(req.policyId());
        }

        boolean       succeeded     = store.simulateSuccess(req.paymentMethodToken());
        PaymentStatus status        = succeeded ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        int           attemptNumber = attemptCounters
                .computeIfAbsent(req.policyId(), k -> new AtomicInteger(0))
                .incrementAndGet();

        String  attemptId     = "ATT-" + uuid();
        String  gatewayRef    = succeeded ? "GW-" + uuid() : null;
        String  failureReason = succeeded ? null : "Insufficient funds – simulated failure";
        Instant now           = Instant.now();
        String  nextRetry     = succeeded ? null
                : now.plus(backoffDays(attemptNumber), ChronoUnit.DAYS).toString();

        var attempt = new PaymentAttempt(
                attemptId, req.policyId(), req.amount(), req.paymentMethodToken(),
                status, gatewayRef, failureReason, now, attemptNumber);

        lastAttempts.put(req.policyId(), attempt);

        log.info("Payment attempt recorded id={} policy={} status={} attempt#{}",
                attemptId, req.policyId(), status, attemptNumber);

        return new PaymentAttemptResponse(
                attemptId, req.policyId(), req.amount(), status,
                gatewayRef, failureReason, now, attemptNumber, nextRetry);
    }

    // ── 3. Delinquent policies ───────────────────────────────────────

    public DelinquentPoliciesResponse getDelinquentPolicies() {
        log.debug("Fetching delinquent policy list");
        List<DelinquentPolicy> list = store.getDelinquentPolicies();
        return new DelinquentPoliciesResponse(list.size(), list);
    }

    // ── 4. Trigger retry ─────────────────────────────────────────────

    public RetryResponse triggerRetry(String policyId) {
        log.debug("Retry requested for policy={}", policyId);

        if (!CannedDataStore.KNOWN_POLICIES.contains(policyId)) {
            throw new PolicyNotFoundException(policyId);
        }

        PaymentAttempt last = lastAttempts.get(policyId);

        // No prior attempt in this session – treat as first synthetic failure
        if (last == null) {
            return buildRetryResponse(policyId, 1);
        }

        if (last.status() == PaymentStatus.SUCCESS) {
            throw new RetryNotEligibleException(policyId,
                    "Last payment was successful; no retry required.");
        }

        int nextAttempt = last.attemptNumber() + 1;
        if (nextAttempt > MAX_RETRY_ATTEMPTS) {
            throw new RetryNotEligibleException(policyId,
                    "Maximum retry attempts (" + MAX_RETRY_ATTEMPTS + ") reached. "
                    + "Policy must be escalated to the collections team.");
        }

        return buildRetryResponse(policyId, nextAttempt);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private RetryResponse buildRetryResponse(String policyId, int nextAttempt) {
        int     days        = backoffDays(nextAttempt);
        String  jobId       = "JOB-" + uuid();
        String  scheduledAt = Instant.now().plus(days, ChronoUnit.DAYS).toString();

        log.info("Retry scheduled jobId={} policy={} attempt#{} backoff={}d",
                jobId, policyId, nextAttempt, days);

        return new RetryResponse(policyId, jobId, scheduledAt, nextAttempt,
                "Retry job queued – attempt #" + nextAttempt + " in " + days + " day(s)");
    }

    private int backoffDays(int attemptNumber) {
        int idx = Math.max(0, Math.min(attemptNumber - 1, RETRY_BACKOFF_DAYS.length - 1));
        return RETRY_BACKOFF_DAYS[idx];
    }

    private String uuid() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ── Inner exception types ────────────────────────────────────────

    public static class PolicyNotFoundException extends RuntimeException {
        public final String policyId;
        public PolicyNotFoundException(String policyId) {
            super("Policy not found: " + policyId);
            this.policyId = policyId;
        }
    }

    public static class RetryNotEligibleException extends RuntimeException {
        public final String policyId;
        public RetryNotEligibleException(String policyId, String reason) {
            super(reason);
            this.policyId = policyId;
        }
    }
}
