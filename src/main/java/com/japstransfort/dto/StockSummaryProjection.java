package com.japstransfort.dto;

import java.math.BigDecimal;

public interface StockSummaryProjection {
    BigDecimal getTotalUnits();
    BigDecimal getCostValue();
    BigDecimal getSrpValue();
}
