package com.japstransfort.dto;

import java.math.BigDecimal;

public record DailySalesDto(
        String date,
        long orderCount,
        BigDecimal total
) {}
