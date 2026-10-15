package com.japstransfort.dto;

import java.math.BigDecimal;

public record ProductSalesDto(
        String sku,
        String name,
        Long quantitySold,
        BigDecimal revenue
) {}
