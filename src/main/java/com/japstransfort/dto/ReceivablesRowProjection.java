package com.japstransfort.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ReceivablesRowProjection {
    Integer getResellerId();
    String getBusinessName();
    BigDecimal getBalance();
    BigDecimal getCreditLimit();
    Long getOpenOrderCount();
    LocalDate getOldestOrderDate();
    BigDecimal getBucket030();
    BigDecimal getBucket3160();
    BigDecimal getBucket6190();
    BigDecimal getBucketOver90();
    BigDecimal getTotalRemaining();
}
