package com.insurance.billing.model.dto.response;

import com.insurance.billing.model.domain.DelinquentPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated list of delinquent policies")
public record DelinquentPoliciesResponse(

        @Schema(description = "Total number of delinquent policies", example = "3")
        int totalCount,

        @Schema(description = "List of delinquent policy records")
        List<DelinquentPolicy> policies
) {}
