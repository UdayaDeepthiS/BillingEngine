package com.insurance.billing.data;

import com.insurance.billing.model.domain.DelinquencyStatus;
import com.insurance.billing.model.domain.DelinquentPolicy;
import com.insurance.billing.model.domain.PremiumScheduleItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory canned data store – replaces a real database for simulation purposes.
 * * Three policies are pre-loaded:
 *   POL-1001  Alice Johnson   Quarterly  Life Assurance Gold
 *   POL-1002  Bob Williams    Monthly    Home & Contents Silver
 *   POL-1003  Carol Smith     Biannual   Critical Illness Platinum
  * Simulation rule:  paymentMethodToken ending in "fail"  →  FAILED
 *                   any other token                      →  SUCCESS
 */
@Component
public class CannedDataStore {

    public static final String POL_1001 = "POL-1001";
    public static final String POL_1002 = "POL-1002";
    public static final String POL_1003 = "POL-1003";

    public static final Set<String> KNOWN_POLICIES = Set.of(POL_1001, POL_1002, POL_1003);

    // ── Premium schedules ────────────────────────────────────────────

    private static final Map<String, List<PremiumScheduleItem>> SCHEDULES = Map.of(

        POL_1001, List.of(
            new PremiumScheduleItem(1, LocalDate.of(2025, 1,  1), new BigDecimal("320.00"), "PAID"),
            new PremiumScheduleItem(2, LocalDate.of(2025, 4,  1), new BigDecimal("320.00"), "PAID"),
            new PremiumScheduleItem(3, LocalDate.of(2025, 7,  1), new BigDecimal("320.00"), "OVERDUE"),
            new PremiumScheduleItem(4, LocalDate.of(2025, 10, 1), new BigDecimal("320.00"), "PENDING")
        ),

        POL_1002, List.of(
            new PremiumScheduleItem(1,  LocalDate.of(2025, 1,  1), new BigDecimal("145.50"), "PAID"),
            new PremiumScheduleItem(2,  LocalDate.of(2025, 2,  1), new BigDecimal("145.50"), "PAID"),
            new PremiumScheduleItem(3,  LocalDate.of(2025, 3,  1), new BigDecimal("145.50"), "PAID"),
            new PremiumScheduleItem(4,  LocalDate.of(2025, 4,  1), new BigDecimal("145.50"), "OVERDUE"),
            new PremiumScheduleItem(5,  LocalDate.of(2025, 5,  1), new BigDecimal("145.50"), "PENDING"),
            new PremiumScheduleItem(6,  LocalDate.of(2025, 6,  1), new BigDecimal("145.50"), "PENDING"),
            new PremiumScheduleItem(7,  LocalDate.of(2025, 7,  1), new BigDecimal("145.50"), "PENDING"),
            new PremiumScheduleItem(8,  LocalDate.of(2025, 8,  1), new BigDecimal("145.50"), "PENDING"),
            new PremiumScheduleItem(9,  LocalDate.of(2025, 9,  1), new BigDecimal("145.50"), "PENDING"),
            new PremiumScheduleItem(10, LocalDate.of(2025, 10, 1), new BigDecimal("145.50"), "PENDING"),
            new PremiumScheduleItem(11, LocalDate.of(2025, 11, 1), new BigDecimal("145.50"), "PENDING"),
            new PremiumScheduleItem(12, LocalDate.of(2025, 12, 1), new BigDecimal("145.50"), "PENDING")
        ),

        POL_1003, List.of(
            new PremiumScheduleItem(1, LocalDate.of(2025, 1, 1), new BigDecimal("890.00"), "PAID"),
            new PremiumScheduleItem(2, LocalDate.of(2025, 7, 1), new BigDecimal("890.00"), "OVERDUE")
        )
    );

    // plan type | holder name | billing frequency | annual premium
    private static final Map<String, String[]> POLICY_META = Map.of(
        POL_1001, new String[]{"Life Assurance – Gold",       "Alice Johnson", "QUARTERLY", "1280.00"},
        POL_1002, new String[]{"Home & Contents – Silver",    "Bob Williams",  "MONTHLY",   "1746.00"},
        POL_1003, new String[]{"Critical Illness – Platinum", "Carol Smith",   "BIANNUAL",  "1780.00"}
    );

    public List<PremiumScheduleItem> getSchedule(String policyId) {
        return SCHEDULES.get(policyId);
    }

    public String[] getPolicyMeta(String policyId) {
        return POLICY_META.get(policyId);
    }

    // ── Delinquent policies ──────────────────────────────────────────

    public List<DelinquentPolicy> getDelinquentPolicies() {
        return List.of(
            new DelinquentPolicy(
                POL_1001, "HLD-501", "Alice Johnson",
                DelinquencyStatus.WARNING, 45,
                new BigDecimal("320.00"), 2,
                LocalDate.of(2025, 8, 7),
                LocalDate.of(2025, 8, 14)
            ),
            new DelinquentPolicy(
                POL_1002, "HLD-502", "Bob Williams",
                DelinquencyStatus.GRACE, 12,
                new BigDecimal("145.50"), 1,
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2025, 8, 10)
            ),
            new DelinquentPolicy(
                POL_1003, "HLD-503", "Carol Smith",
                DelinquencyStatus.LAPSED, 90,
                new BigDecimal("890.00"), 3,
                LocalDate.of(2025, 7, 15),
                LocalDate.of(2025, 8, 22)
            )
        );
    }

    // ── Simulation helper ────────────────────────────────────────────

    /** Returns false (simulate failure) when token ends with "fail", true otherwise. */
    public boolean simulateSuccess(String paymentMethodToken) {
        return !paymentMethodToken.toLowerCase().endsWith("fail");
    }
}
