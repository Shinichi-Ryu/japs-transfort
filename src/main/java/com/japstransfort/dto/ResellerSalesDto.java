package com.japstransfort.dto;

import java.math.BigDecimal;

public record ResellerSalesDto(
        String businessName,
        long orderCount,
        BigDecimal total
) {}
