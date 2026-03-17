package com.insurance.billing.exception;

import com.insurance.billing.service.BillingEngineService.PolicyNotFoundException;
import com.insurance.billing.service.BillingEngineService.RetryNotEligibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Translates service exceptions into RFC 7807 ProblemDetail responses.
 *
 * HTTP status mapping
 * ───────────────────
 *   400  MethodArgumentNotValidException  →  validation failure
 *   404  PolicyNotFoundException          →  unknown policy
 *   422  RetryNotEligibleException        →  retry guard
 *   500  Exception                        →  unexpected error
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_BASE_URI = "https://billing-engine.insurance.com/errors";

    // ── 400 Validation ───────────────────────────────────────────────

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setType(URI.create(ERROR_BASE_URI + "/validation-error"));

        log.warn("Validation error: {}", detail);
        return ResponseEntity.badRequest().body(problem);
    }

    // ── 404 Policy not found ─────────────────────────────────────────

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePolicyNotFound(PolicyNotFoundException ex) {
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Policy not found");
        problem.setType(URI.create(ERROR_BASE_URI + "/policy-not-found"));
        problem.setProperty("policyId", ex.policyId);

        log.warn("Policy not found: {}", ex.policyId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // ── 422 Retry not eligible ───────────────────────────────────────

    @ExceptionHandler(RetryNotEligibleException.class)
    public ResponseEntity<ProblemDetail> handleRetryNotEligible(RetryNotEligibleException ex) {
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Retry not eligible");
        problem.setType(URI.create(ERROR_BASE_URI + "/retry-not-eligible"));
        problem.setProperty("policyId", ex.policyId);

        log.warn("Retry not eligible policy={}: {}", ex.policyId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    // ── 500 Fallback ─────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact the platform team.");
        problem.setTitle("Internal server error");
        problem.setType(URI.create(ERROR_BASE_URI + "/internal-error"));

        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
