package com.japstransfort.service;

import com.japstransfort.dto.CompanyDetailsDto;
import com.japstransfort.dto.StatementDto;
import com.japstransfort.dto.StatementRowDto;
import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import com.japstransfort.entity.Payment;
import com.japstransfort.entity.Reseller;
import com.japstransfort.repository.OrderRepository;
import com.japstransfort.repository.PaymentRepository;
import com.japstransfort.repository.ResellerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Read-only service that builds the Statement of Account DTO.
 * Uses aggregation queries for opening balance and a single query for period rows.
 */
@Service
public class StatementService {

    private static final List<OrderStatus> CHARGE_STATUSES = List.of(
            OrderStatus.approved, OrderStatus.packed, OrderStatus.shipped, OrderStatus.delivered
    );

    @Value("${app.company.name:Japs Transfort}")
    private String companyName;

    @Value("${app.company.address:}")
    private String companyAddress;

    @Value("${app.company.phone:}")
    private String companyPhone;

    @Value("${app.company.email:}")
    private String companyEmail;

    @Value("${app.company.tin:}")
    private String companyTin;

    @Value("${app.invoice.footer:}")
    private String invoiceFooter;

    private final ResellerRepository resellerRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public StatementService(ResellerRepository resellerRepository,
                            OrderRepository orderRepository,
                            PaymentRepository paymentRepository) {
        this.resellerRepository = resellerRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Returns an Optional with the statement, or empty if the reseller is not found.
     */
    @Transactional(readOnly = true)
    public Optional<StatementDto> buildStatement(Integer resellerId, LocalDate from, LocalDate to) {
        Optional<Reseller> opt = resellerRepository.findById(resellerId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Reseller reseller = opt.get();

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();   // exclusive upper bound

        // ── Opening balance (aggregation, no lazy-load loops) ─────────────────
        BigDecimal openingCharges = coalesce(
                orderRepository.sumChargesBeforeDate(resellerId, CHARGE_STATUSES, fromDt));
        BigDecimal openingPayments = coalesce(
                paymentRepository.sumPaymentsForResellerBeforeDate(resellerId, fromDt));
        BigDecimal openingBalance = scale2(openingCharges.subtract(openingPayments));

        // ── Period rows (two queries, no lazy loops) ──────────────────────────
        List<Order> periodOrders = orderRepository.findStatementOrders(resellerId, CHARGE_STATUSES, fromDt, toDt);
        List<Payment> periodPayments = paymentRepository.findStatementPayments(resellerId, fromDt, toDt);

        List<StatementRowDto> rows = new ArrayList<>();

        // Invoice rows
        for (Order o : periodOrders) {
            StatementRowDto row = new StatementRowDto(
                    o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate() : from,
                    "Invoice",
                    o.getOrderNo(),
                    o.getOrderId(),  // orderId for link
                    null,            // no paymentId
                    scale2(o.getTotal()),
                    null,
                    0  // sort order: Invoices first
            );
            rows.add(row);
        }

        // Payment rows
        for (Payment p : periodPayments) {
            String receiptNo = String.format("RCT-%06d", p.getPaymentId());
            StatementRowDto row = new StatementRowDto(
                    p.getPaidAt() != null ? p.getPaidAt().toLocalDate() : from,
                    "Payment",
                    receiptNo,
                    null,            // no orderId
                    p.getPaymentId(), // paymentId for link
                    null,
                    scale2(p.getAmount()),
                    1  // sort order: Payments after Invoices on same date
            );
            rows.add(row);
        }


        // Sort: by date ASC, then sortOrder (Invoice=0 before Payment=1)
        rows.sort(Comparator.comparing(StatementRowDto::getDate)
                            .thenComparingInt(StatementRowDto::getSortOrder));

        // Compute running balance
        BigDecimal running = openingBalance;
        BigDecimal periodCharges = BigDecimal.ZERO;
        BigDecimal periodPaid = BigDecimal.ZERO;
        for (StatementRowDto row : rows) {
            if (row.getDebit() != null) {
                running = running.add(row.getDebit());
                periodCharges = periodCharges.add(row.getDebit());
            } else if (row.getCredit() != null) {
                running = running.subtract(row.getCredit());
                periodPaid = periodPaid.add(row.getCredit());
            }
            row.setBalance(scale2(running));
        }
        BigDecimal closingBalance = scale2(running);

        // ── Reseller credit info ──────────────────────────────────────────────
        BigDecimal accountBalance = scale2(coalesce(reseller.getBalance()));
        BigDecimal creditLimit = scale2(coalesce(reseller.getCreditLimit()));
        BigDecimal availableCredit = scale2(creditLimit.subtract(accountBalance));

        // ── Reconciliation ────────────────────────────────────────────────────
        boolean todayReport = !to.isAfter(LocalDate.now()) && !LocalDate.now().isAfter(to);
        BigDecimal reconciliationDiff = closingBalance.subtract(accountBalance).abs().setScale(2, RoundingMode.HALF_UP);

        // ── Company details (same pattern as InvoiceDto) ──────────────────────
        CompanyDetailsDto company = new CompanyDetailsDto(
                companyName, companyAddress, companyPhone, companyEmail, companyTin, invoiceFooter);

        // ── Assemble DTO ──────────────────────────────────────────────────────
        StatementDto dto = new StatementDto();
        dto.setResellerId(resellerId);
        dto.setBusinessName(reseller.getBusinessName());
        dto.setContactPerson(reseller.getUser() != null ? reseller.getUser().getFullName() : null);
        dto.setAddress(reseller.getAddress());
        dto.setCityLocation(reseller.getTerritory());
        dto.setFromDate(from);
        dto.setToDate(to);
        dto.setOpeningBalance(openingBalance);
        dto.setClosingBalance(closingBalance);
        dto.setAccountBalance(accountBalance);
        dto.setCreditLimit(creditLimit);
        dto.setAvailableCredit(availableCredit);
        dto.setPeriodCharges(scale2(periodCharges));
        dto.setPeriodPayments(scale2(periodPaid));
        dto.setRows(rows);
        dto.setTodayReport(todayReport);
        dto.setReconciliationDiff(reconciliationDiff);
        dto.setReconciliationOk(reconciliationDiff.compareTo(new BigDecimal("0.01")) < 0);
        dto.setCompany(company);

        return Optional.of(dto);
    }

    private BigDecimal coalesce(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal scale2(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
