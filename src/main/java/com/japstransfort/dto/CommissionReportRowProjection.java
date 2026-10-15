package com.japstransfort.dto;

import java.math.BigDecimal;

public interface CommissionReportRowProjection {
    Integer getResellerId();
    String getBusinessName();
    Long getCommissionCount();
    BigDecimal getPendingAmount();
    BigDecimal getPaidAmount();
    BigDecimal getTotalAmount();
}
