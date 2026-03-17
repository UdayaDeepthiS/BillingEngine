package com.insurance.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.billing.model.dto.request.PaymentAttemptRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Billing Engine Controller – Integration Tests")
class BillingEngineControllerTest {

    private static final String BASE = "/api/v1/billing";

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    // ═══════════════════════════════════════════════════════════════
    // 1. PREMIUM SCHEDULE
    // ═══════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("GET premium-schedule – 200 OK for known quarterly policy (POL-1001)")
    void premiumSchedule_knownQuarterlyPolicy_returns200() throws Exception {
        mockMvc.perform(get(BASE + "/policies/POL-1001/premium-schedule")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value("POL-1001"))
                .andExpect(jsonPath("$.planType").value("Life Assurance – Gold"))
                .andExpect(jsonPath("$.holderName").value("Alice Johnson"))
                .andExpect(jsonPath("$.billingFrequency").value("QUARTERLY"))
                .andExpect(jsonPath("$.annualPremium").value(1280.00))
                .andExpect(jsonPath("$.schedule").isArray())
                .andExpect(jsonPath("$.schedule.length()").value(4))
                .andExpect(jsonPath("$.schedule[0].status").value("PAID"))
                .andExpect(jsonPath("$.schedule[2].status").value("OVERDUE"));
    }

    @Test @Order(2)
    @DisplayName("GET premium-schedule – 200 OK for monthly policy (POL-1002) with 12 instalments")
    void premiumSchedule_knownMonthlyPolicy_returns12Instalments() throws Exception {
        mockMvc.perform(get(BASE + "/policies/POL-1002/premium-schedule")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingFrequency").value("MONTHLY"))
                .andExpect(jsonPath("$.schedule.length()").value(12));
    }

    @Test @Order(3)
    @DisplayName("GET premium-schedule – 200 OK for biannual policy (POL-1003) with 2 instalments")
    void premiumSchedule_knownBiannualPolicy_returns2Instalments() throws Exception {
        mockMvc.perform(get(BASE + "/policies/POL-1003/premium-schedule")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingFrequency").value("BIANNUAL"))
                .andExpect(jsonPath("$.schedule.length()").value(2));
    }

    @Test @Order(4)
    @DisplayName("GET premium-schedule – 404 for unknown policy")
    void premiumSchedule_unknownPolicy_returns404() throws Exception {
        mockMvc.perform(get(BASE + "/policies/POL-UNKNOWN/premium-schedule")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Policy not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.policyId").value("POL-UNKNOWN"));
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. RECORD PAYMENT ATTEMPT
    // ═══════════════════════════════════════════════════════════════

    @Test @Order(5)
    @DisplayName("POST payment attempt – 201 CREATED with status SUCCESS (token does not end in 'fail')")
    void recordPayment_successToken_returns201Success() throws Exception {
        var req = new PaymentAttemptRequest(
                "POL-1001", new BigDecimal("320.00"), "tok_visa_4242", "idem-test-001");

        mockMvc.perform(post(BASE + "/payments/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyId").value("POL-1001"))
                .andExpect(jsonPath("$.amount").value(320.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.attemptId", startsWith("ATT-")))
                .andExpect(jsonPath("$.gatewayReference", startsWith("GW-")))
                .andExpect(jsonPath("$.failureReason").doesNotExist())
                .andExpect(jsonPath("$.nextRetryScheduledAt").doesNotExist())
                .andExpect(jsonPath("$.attemptNumber").value(greaterThanOrEqualTo(1)));
    }

    @Test @Order(6)
    @DisplayName("POST payment attempt – 201 CREATED with status FAILED (token ends in 'fail')")
    void recordPayment_failToken_returns201Failed() throws Exception {
        var req = new PaymentAttemptRequest(
                "POL-1002", new BigDecimal("145.50"), "tok_card_fail", "idem-test-002");

        mockMvc.perform(post(BASE + "/payments/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").isString())
                .andExpect(jsonPath("$.nextRetryScheduledAt").isString())
                .andExpect(jsonPath("$.gatewayReference").doesNotExist());
    }

    @Test @Order(7)
    @DisplayName("POST payment attempt – 400 Bad Request when policyId is blank")
    void recordPayment_blankPolicyId_returns400() throws Exception {
        String body = """
                {
                  "policyId":           "",
                  "amount":             320.00,
                  "paymentMethodToken": "tok_test"
                }
                """;

        mockMvc.perform(post(BASE + "/payments/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail", containsString("policyId")));
    }

    @Test @Order(8)
    @DisplayName("POST payment attempt – 400 Bad Request when amount is zero")
    void recordPayment_zeroAmount_returns400() throws Exception {
        String body = """
                {
                  "policyId":           "POL-1001",
                  "amount":             0,
                  "paymentMethodToken": "tok_test"
                }
                """;

        mockMvc.perform(post(BASE + "/payments/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("amount")));
    }

    @Test @Order(9)
    @DisplayName("POST payment attempt – 400 Bad Request when paymentMethodToken is missing")
    void recordPayment_missingToken_returns400() throws Exception {
        String body = """
                {
                  "policyId": "POL-1001",
                  "amount":   320.00
                }
                """;

        mockMvc.perform(post(BASE + "/payments/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("paymentMethodToken")));
    }

    @Test @Order(10)
    @DisplayName("POST payment attempt – 404 for unknown policy")
    void recordPayment_unknownPolicy_returns404() throws Exception {
        var req = new PaymentAttemptRequest(
                "POL-9999", new BigDecimal("100.00"), "tok_visa", null);

        mockMvc.perform(post(BASE + "/payments/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.policyId").value("POL-9999"));
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. DELINQUENT POLICIES
    // ═══════════════════════════════════════════════════════════════

    @Test @Order(11)
    @DisplayName("GET delinquent – 200 OK returns 3 policies with correct statuses")
    void delinquent_returns200WithThreePolicies() throws Exception {
        mockMvc.perform(get(BASE + "/policies/delinquent")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.policies").isArray())
                .andExpect(jsonPath("$.policies.length()").value(3))
                .andExpect(jsonPath("$.policies[0].delinquencyStatus").value("WARNING"))
                .andExpect(jsonPath("$.policies[1].delinquencyStatus").value("GRACE"))
                .andExpect(jsonPath("$.policies[2].delinquencyStatus").value("LAPSED"));
    }

    @Test @Order(12)
    @DisplayName("GET delinquent – response contains all three expected policy IDs")
    void delinquent_containsAllExpectedPolicyIds() throws Exception {
        mockMvc.perform(get(BASE + "/policies/delinquent")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policies[*].policyId",
                        hasItems("POL-1001", "POL-1002", "POL-1003")));
    }

    @Test @Order(13)
    @DisplayName("GET delinquent – LAPSED policy has 3 failed attempts and 90 days past due")
    void delinquent_lapsedPolicyHasCorrectDetails() throws Exception {
        mockMvc.perform(get(BASE + "/policies/delinquent")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policies[2].policyId").value("POL-1003"))
                .andExpect(jsonPath("$.policies[2].daysPastDue").value(90))
                .andExpect(jsonPath("$.policies[2].failedAttempts").value(3))
                .andExpect(jsonPath("$.policies[2].outstandingBalance").value(890.00));
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. TRIGGER RETRY
    // ═══════════════════════════════════════════════════════════════

    @Test @Order(14)
    @DisplayName("POST retry – 202 Accepted for known policy with no prior session attempt")
    void retry_knownPolicyNoHistory_returns202() throws Exception {
        mockMvc.perform(post(BASE + "/payments/retry/POL-1003"))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.policyId").value("POL-1003"))
                .andExpect(jsonPath("$.retryJobId", startsWith("JOB-")))
                .andExpect(jsonPath("$.scheduledAt").isString())
                .andExpect(jsonPath("$.attemptNumber").isNumber())
                .andExpect(jsonPath("$.message", containsString("Retry job queued")));
    }

    @Test @Order(15)
    @DisplayName("POST retry – 202 Accepted with correct back-off after a recorded FAILED attempt")
    void retry_afterFailedAttempt_returns202WithBackoff() throws Exception {
        // Step 1: record a failed attempt for POL-1001
        var failReq = new PaymentAttemptRequest(
                "POL-1001", new BigDecimal("320.00"), "tok_decline_fail", null);

        mockMvc.perform(post(BASE + "/payments/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));

        // Step 2: trigger the retry
        mockMvc.perform(post(BASE + "/payments/retry/POL-1001"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.policyId").value("POL-1001"))
                .andExpect(jsonPath("$.retryJobId", startsWith("JOB-")))
                .andExpect(jsonPath("$.scheduledAt").isString())
                .andExpect(jsonPath("$.message", containsString("day")));
    }

    @Test @Order(16)
    @DisplayName("POST retry – 404 for unknown policy")
    void retry_unknownPolicy_returns404() throws Exception {
        mockMvc.perform(post(BASE + "/payments/retry/POL-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Policy not found"))
                .andExpect(jsonPath("$.policyId").value("POL-9999"));
    }

    @Test @Order(17)
    @DisplayName("POST retry – 422 Unprocessable when last payment was SUCCESS")
    void retry_afterSuccessfulPayment_returns422() throws Exception {
        // Step 1: record a successful payment for POL-1002
        var successReq = new PaymentAttemptRequest(
                "POL-1002", new BigDecimal("145.50"), "tok_visa_ok", null);

        mockMvc.perform(post(BASE + "/payments/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(successReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Step 2: retry should be rejected
        mockMvc.perform(post(BASE + "/payments/retry/POL-1002"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Retry not eligible"))
                .andExpect(jsonPath("$.policyId").value("POL-1002"));
    }
}
