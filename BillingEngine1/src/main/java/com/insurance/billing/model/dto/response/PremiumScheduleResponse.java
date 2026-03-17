package com.insurance.billing.model.dto.response;

import com.insurance.billing.model.domain.PremiumScheduleItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Full premium schedule for a policy")
public record PremiumScheduleResponse(

        @Schema(description = "Policy identifier", example = "POL-1001")
        String policyId,

        @Schema(description = "Insurance plan name", example = "Life Assurance - Gold")
        String planType,

        @Schema(description = "Policyholder name", example = "Alice Johnson")
        String holderName,

        @Schema(description = "Total annual premium in GBP", example = "1280.00")
        BigDecimal annualPremium,

        @Schema(description = "Billing frequency", example = "QUARTERLY",
                allowableValues = {"MONTHLY", "QUARTERLY", "BIANNUAL", "ANNUAL"})
        String billingFrequency,

        @Schema(description = "Ordered list of instalment items")
        List<PremiumScheduleItem> schedule
) {}
