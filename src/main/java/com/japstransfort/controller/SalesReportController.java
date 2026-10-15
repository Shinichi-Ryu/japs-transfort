package com.japstransfort.controller;

import com.japstransfort.dto.*;
import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import com.japstransfort.entity.Payment;
import com.japstransfort.entity.PaymentMethod;
import com.japstransfort.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for admin reports: index, sales, receivables, stock, commissions.
 */
@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class SalesReportController {

    private static final List<OrderStatus> SALES_STATUSES = List.of(
            OrderStatus.approved,
            OrderStatus.packed,
            OrderStatus.shipped,
            OrderStatus.delivered
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ResellerRepository resellerRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CommissionRepository commissionRepository;
    private final WarehouseRepository warehouseRepository;

    public SalesReportController(OrderRepository orderRepository,
                                 PaymentRepository paymentRepository,
                                 ResellerRepository resellerRepository,
                                 StockMovementRepository stockMovementRepository,
                                 CommissionRepository commissionRepository,
                                 WarehouseRepository warehouseRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.resellerRepository = resellerRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.commissionRepository = commissionRepository;
        this.warehouseRepository = warehouseRepository;
    }

    // ── Reports Index ──────────────────────────────────────────────────────────

    @GetMapping({"", "/"})
    public String reportsIndex(Model model) {
        return "admin/reports/index";
    }

    // ── Sales Report ───────────────────────────────────────────────────────────

    @GetMapping("/sales")
    public String salesReport(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "showAllResellers", defaultValue = "false") boolean showAllResellers,
            @RequestParam(value = "showAllProducts", defaultValue = "false") boolean showAllProducts,
            Model model) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("showAllResellers", showAllResellers);
        model.addAttribute("showAllProducts", showAllProducts);

        if (from.isAfter(to)) {
            model.addAttribute("errorMessage", "The 'From' date cannot be after the 'To' date.");
            model.addAttribute("totalOrders", 0L);
            model.addAttribute("grossSales", BigDecimal.ZERO);
            model.addAttribute("totalCollected", BigDecimal.ZERO);
            model.addAttribute("averageOrderValue", BigDecimal.ZERO);
            model.addAttribute("resellerSales", Collections.emptyList());
            model.addAttribute("hasMoreResellers", false);
            model.addAttribute("totalResellersCount", 0);
            model.addAttribute("productSales", Collections.emptyList());
            model.addAttribute("hasMoreProducts", false);
            model.addAttribute("totalProductsCount", 0);
            model.addAttribute("dailySales", Collections.emptyList());
            model.addAttribute("paymentMethodSales", defaultPaymentMethodSales());
            return "admin/reports/sales";
        }

        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.plusDays(1).atStartOfDay();

        long totalOrders = orderRepository.countSalesOrders(SALES_STATUSES, startDateTime, endDateTime);
        BigDecimal grossSales = coalesce(orderRepository.sumGrossSales(SALES_STATUSES, startDateTime, endDateTime));
        BigDecimal totalCollected = coalesce(paymentRepository.sumAmountByPaidAtBetween(startDateTime, endDateTime));
        BigDecimal averageOrderValue = totalOrders > 0
                ? grossSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("grossSales", grossSales);
        model.addAttribute("totalCollected", totalCollected);
        model.addAttribute("averageOrderValue", averageOrderValue);

        List<ResellerSalesDto> allResellers = orderRepository.findSalesByReseller(SALES_STATUSES, startDateTime, endDateTime);
        boolean hasMoreResellers = allResellers.size() > 20;
        model.addAttribute("resellerSales", hasMoreResellers && !showAllResellers ? allResellers.subList(0, 20) : allResellers);
        model.addAttribute("hasMoreResellers", hasMoreResellers);
        model.addAttribute("totalResellersCount", allResellers.size());

        List<ProductSalesDto> allProducts = orderRepository.findSalesByProduct(SALES_STATUSES, startDateTime, endDateTime);
        boolean hasMoreProducts = allProducts.size() > 20;
        model.addAttribute("productSales", hasMoreProducts && !showAllProducts ? allProducts.subList(0, 20) : allProducts);
        model.addAttribute("hasMoreProducts", hasMoreProducts);
        model.addAttribute("totalProductsCount", allProducts.size());

        List<Object[]> rawDaily = orderRepository.findDailySales(startDateTime, endDateTime);
        model.addAttribute("dailySales", rawDaily.stream().map(row -> new DailySalesDto(
                String.valueOf(row[0]),
                ((Number) row[1]).longValue(),
                row[2] instanceof BigDecimal bd ? bd : new BigDecimal(row[2].toString())
        )).toList());

        List<PaymentMethodSalesDto> queryPmSales = orderRepository.findSalesByPaymentMethod(SALES_STATUSES, startDateTime, endDateTime);
        Map<PaymentMethod, PaymentMethodSalesDto> pmMap = queryPmSales.stream()
                .collect(Collectors.toMap(PaymentMethodSalesDto::paymentMethod, d -> d, (a, b) -> a));
        List<PaymentMethodSalesDto> paymentMethodSales = new ArrayList<>();
        for (PaymentMethod pm : List.of(PaymentMethod.COD, PaymentMethod.ONLINE, PaymentMethod.CREDIT)) {
            paymentMethodSales.add(pmMap.getOrDefault(pm, new PaymentMethodSalesDto(pm, 0L, BigDecimal.ZERO)));
        }
        model.addAttribute("paymentMethodSales", paymentMethodSales);

        return "admin/reports/sales";
    }

    @GetMapping("/sales/export")
    public void exportSalesCsv(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.plusDays(1).atStartOfDay();

        List<Order> orders = from.isAfter(to)
                ? Collections.emptyList()
                : orderRepository.findSalesOrdersForExport(SALES_STATUSES, startDateTime, endDateTime);

        setupCsvResponse(response, "sales_report_" + from + "_to_" + to + ".csv");
        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("\"Order No\",\"Date\",\"Time\",\"Reseller\",\"Payment Method\",\"Status\",\"Payment Status\",\"Total\",\"Amount Paid\"");

        for (Order ord : orders) {
            String orderNo = esc(ord.getOrderNo());
            String date = esc(ord.getCreatedAt() != null ? ord.getCreatedAt().format(DATE_FMT) : "");
            String time = esc(ord.getCreatedAt() != null ? ord.getCreatedAt().format(TIME_FMT) : "");
            String resellerName = esc(ord.getReseller() != null ? ord.getReseller().getBusinessName() : "");
            String paymentMethod = esc(pmDisplayName(ord.getPaymentMethod()));
            String status = esc(ord.getStatus() != null ? ord.getStatus().name() : "");
            String paymentStatus = esc(ord.getPaymentStatus() != null ? ord.getPaymentStatus().name() : "");
            BigDecimal total = scale2(ord.getTotal());
            BigDecimal paid = ord.getPayments() == null ? BigDecimal.ZERO.setScale(2) :
                    ord.getPayments().stream().map(Payment::getAmount).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

            writer.println(String.join(",", orderNo, date, time, resellerName, paymentMethod,
                    status, paymentStatus, total.toPlainString(), paid.toPlainString()));
        }
        writer.flush();
    }

    // ── Receivables Report ─────────────────────────────────────────────────────

    @GetMapping("/receivables")
    public String receivablesReport(
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "balanceOnly", defaultValue = "false") boolean balanceOnly,
            Model model) {

        model.addAttribute("search", search);
        model.addAttribute("balanceOnly", balanceOnly);

        boolean searchNull = search == null || search.trim().isEmpty();
        List<ReceivablesRowProjection> rows = orderRepository.findReceivablesRows(searchNull, search == null ? "" : search.trim(), balanceOnly);

        List<ReceivablesRowDto> receivables = rows.stream().map(r -> new ReceivablesRowDto(
                r.getResellerId(),
                r.getBusinessName(),
                r.getBalance(),
                r.getCreditLimit(),
                r.getOpenOrderCount() != null ? r.getOpenOrderCount() : 0L,
                r.getOldestOrderDate(),
                r.getBucket030(),
                r.getBucket3160(),
                r.getBucket6190(),
                r.getBucketOver90(),
                r.getTotalRemaining()
        )).toList();

        BigDecimal totalOutstanding = receivables.stream()
                .map(ReceivablesRowDto::getTotalRemaining).reduce(BigDecimal.ZERO, BigDecimal::add);
        long resellersWithBalance = resellerRepository.countWithBalance();
        long resellersOverLimit = resellerRepository.countOverCreditLimit();

        // Reconciliation: sum of reseller balance columns vs sum of open order remainders
        BigDecimal sumBalances = resellerRepository.sumTotalBalance();
        BigDecimal sumOpenOrders = coalesce(orderRepository.sumTotalRemainingOpenOrders());
        BigDecimal diff = sumBalances.subtract(sumOpenOrders).abs();
        boolean balanced = diff.compareTo(new BigDecimal("0.01")) < 0;
        String reconciliationMsg = balanced
                ? "Balances match open orders"
                : "Difference of ₱" + diff.setScale(2, RoundingMode.HALF_UP).toPlainString() + " between balances and open orders";

        model.addAttribute("receivables", receivables);
        model.addAttribute("totalOutstanding", totalOutstanding);
        model.addAttribute("resellersWithBalance", resellersWithBalance);
        model.addAttribute("resellersOverLimit", resellersOverLimit);
        model.addAttribute("reconciliationMsg", reconciliationMsg);
        model.addAttribute("reconciliationOk", balanced);

        return "admin/reports/receivables";
    }

    @GetMapping("/receivables/export")
    public void exportReceivablesCsv(
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "balanceOnly", defaultValue = "false") boolean balanceOnly,
            HttpServletResponse response) throws IOException {

        boolean searchNull = search == null || search.trim().isEmpty();
        List<ReceivablesRowProjection> rows = orderRepository.findReceivablesRows(searchNull, search == null ? "" : search.trim(), balanceOnly);

        setupCsvResponse(response, "receivables_report.csv");
        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("\"Business Name\",\"Balance\",\"Credit Limit\",\"Available Credit\",\"Open Orders\",\"Oldest Order Date\",\"0-30 Days\",\"31-60 Days\",\"61-90 Days\",\"Over 90 Days\",\"Total Remaining\"");

        for (ReceivablesRowProjection r : rows) {
            String businessName = esc(r.getBusinessName());
            BigDecimal balance = coalesce(r.getBalance());
            BigDecimal creditLimit = coalesce(r.getCreditLimit());
            BigDecimal available = creditLimit.subtract(balance);
            long openCount = r.getOpenOrderCount() != null ? r.getOpenOrderCount() : 0L;
            String oldestDate = r.getOldestOrderDate() != null ? r.getOldestOrderDate().format(DATE_FMT) : "";
            BigDecimal b030 = coalesce(r.getBucket030());
            BigDecimal b3160 = coalesce(r.getBucket3160());
            BigDecimal b6190 = coalesce(r.getBucket6190());
            BigDecimal bOver = coalesce(r.getBucketOver90());
            BigDecimal total = coalesce(r.getTotalRemaining());

            writer.println(String.join(",",
                    businessName, esc(scale2(balance).toPlainString()), esc(scale2(creditLimit).toPlainString()),
                    esc(scale2(available).toPlainString()), String.valueOf(openCount), esc(oldestDate),
                    esc(scale2(b030).toPlainString()), esc(scale2(b3160).toPlainString()),
                    esc(scale2(b6190).toPlainString()), esc(scale2(bOver).toPlainString()),
                    esc(scale2(total).toPlainString())));
        }
        writer.flush();
    }

    // ── Stock Report ───────────────────────────────────────────────────────────

    @GetMapping("/stock")
    public String stockReport(
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "warehouseId", required = false) Integer warehouseId,
            @RequestParam(value = "statusFilter", required = false, defaultValue = "all") String statusFilter,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        model.addAttribute("search", search);
        model.addAttribute("warehouseId", warehouseId);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("warehouses", warehouseRepository.findAll());

        boolean searchNull = search == null || search.trim().isEmpty();
        String sf = (statusFilter == null || statusFilter.isBlank()) ? "all" : statusFilter;
        List<StockRowProjection> stockRows = stockMovementRepository.findStockRows(
                searchNull, searchNull ? "" : search.trim(),
                warehouseId == null, warehouseId,
                sf);

        // Summary from full (unfiltered) stock
        StockSummaryProjection summary = stockMovementRepository.findStockSummary();
        BigDecimal totalUnits = summary != null && summary.getTotalUnits() != null ? summary.getTotalUnits() : BigDecimal.ZERO;
        BigDecimal costValue = summary != null && summary.getCostValue() != null ? summary.getCostValue() : BigDecimal.ZERO;
        long outOfStockCount = stockMovementRepository.countOutOfStockItems();
        long lowStockCount = stockMovementRepository.countLowStockItemsCross();

        model.addAttribute("stockRows", stockRows);
        model.addAttribute("totalUnits", totalUnits.longValue());
        model.addAttribute("costValue", costValue);
        model.addAttribute("outOfStockCount", outOfStockCount);
        model.addAttribute("lowStockCount", lowStockCount);

        // Movement summary table
        if (from.isAfter(to)) {
            model.addAttribute("errorMessage", "The 'From' date cannot be after the 'To' date.");
            model.addAttribute("movementSummary", Collections.emptyList());
        } else {
            LocalDateTime startDateTime = from.atStartOfDay();
            LocalDateTime endDateTime = to.plusDays(1).atStartOfDay();
            List<StockMovementSummaryProjection> rawMov = stockMovementRepository.findMovementSummaryByProduct(
                    startDateTime, endDateTime, searchNull, searchNull ? "" : search.trim());
            model.addAttribute("movementSummary", rawMov);
        }

        return "admin/reports/stock";
    }

    @GetMapping("/stock/export")
    public void exportStockCsv(
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "warehouseId", required = false) Integer warehouseId,
            @RequestParam(value = "statusFilter", required = false, defaultValue = "all") String statusFilter,
            HttpServletResponse response) throws IOException {

        boolean searchNull = search == null || search.trim().isEmpty();
        String sf = (statusFilter == null || statusFilter.isBlank()) ? "all" : statusFilter;
        List<StockRowProjection> rawStock = stockMovementRepository.findStockRows(
                searchNull, searchNull ? "" : search.trim(),
                warehouseId == null, warehouseId, sf);

        setupCsvResponse(response, "stock_report.csv");
        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("\"SKU\",\"Product Name\",\"Warehouse\",\"Quantity\",\"Reorder Level\",\"Status\",\"Stock Value (Cost)\",\"Stock Value (SRP)\"");

        for (StockRowProjection row : rawStock) {
            writer.println(String.join(",",
                    esc(row.getSku()), esc(row.getName()), esc(row.getWarehouseName()),
                    String.valueOf(row.getQuantity() != null ? row.getQuantity() : 0),
                    String.valueOf(row.getReorderLevel() != null ? row.getReorderLevel() : 0),
                    esc(row.getStockStatus()),
                    scale2(row.getStockValueAtCost()).toPlainString(),
                    scale2(row.getStockValueAtSrp()).toPlainString()));
        }
        writer.flush();
    }

    // ── Commission Report ──────────────────────────────────────────────────────

    @GetMapping("/commissions")
    public String commissionsReport(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "statusFilter", required = false, defaultValue = "all") String statusFilter,
            Model model) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("statusFilter", statusFilter);

        if (from.isAfter(to)) {
            model.addAttribute("errorMessage", "The 'From' date cannot be after the 'To' date.");
            model.addAttribute("commissionRows", Collections.emptyList());
            model.addAttribute("totalAmount", BigDecimal.ZERO);
            model.addAttribute("pendingAmount", BigDecimal.ZERO);
            model.addAttribute("paidAmount", BigDecimal.ZERO);
            return "admin/reports/commissions";
        }

        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.plusDays(1).atStartOfDay();
        String sf = (statusFilter == null || statusFilter.isBlank()) ? "all" : statusFilter.trim().toLowerCase();

        List<CommissionReportRowProjection> rawRows = commissionRepository.findCommissionReportByReseller(startDateTime, endDateTime, sf);
        List<CommissionReportRowDto> commissionRows = rawRows.stream().map(r -> new CommissionReportRowDto(
                r.getResellerId(), r.getBusinessName(),
                r.getCommissionCount() != null ? r.getCommissionCount() : 0L,
                r.getPendingAmount(), r.getPaidAmount(), r.getTotalAmount()
        )).toList();

        BigDecimal totalAmount = coalesce(commissionRepository.sumTotalInRange(startDateTime, endDateTime));
        BigDecimal pendingAmount = coalesce(commissionRepository.sumPendingInRange(startDateTime, endDateTime));
        BigDecimal paidAmount = coalesce(commissionRepository.sumPaidInRange(startDateTime, endDateTime));

        model.addAttribute("commissionRows", commissionRows);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("pendingAmount", pendingAmount);
        model.addAttribute("paidAmount", paidAmount);

        return "admin/reports/commissions";
    }

    @GetMapping("/commissions/export")
    public void exportCommissionsCsv(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "statusFilter", required = false, defaultValue = "all") String statusFilter,
            HttpServletResponse response) throws IOException {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        List<CommissionReportRowProjection> rawRows = from.isAfter(to) ? Collections.emptyList() :
                commissionRepository.findCommissionReportByReseller(
                        from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                        (statusFilter == null || statusFilter.isBlank()) ? "all" : statusFilter.trim().toLowerCase());

        setupCsvResponse(response, "commissions_report_" + from + "_to_" + to + ".csv");
        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("\"Business Name\",\"Commissions\",\"Pending Amount\",\"Paid Amount\",\"Total Amount\"");

        for (CommissionReportRowProjection r : rawRows) {
            writer.println(String.join(",",
                    esc(r.getBusinessName()),
                    String.valueOf(r.getCommissionCount() != null ? r.getCommissionCount() : 0L),
                    scale2(coalesce(r.getPendingAmount())).toPlainString(),
                    scale2(coalesce(r.getPaidAmount())).toPlainString(),
                    scale2(coalesce(r.getTotalAmount())).toPlainString()));
        }
        writer.flush();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<PaymentMethodSalesDto> defaultPaymentMethodSales() {
        return List.of(
                new PaymentMethodSalesDto(PaymentMethod.COD, 0L, BigDecimal.ZERO),
                new PaymentMethodSalesDto(PaymentMethod.ONLINE, 0L, BigDecimal.ZERO),
                new PaymentMethodSalesDto(PaymentMethod.CREDIT, 0L, BigDecimal.ZERO)
        );
    }

    private String pmDisplayName(PaymentMethod pm) {
        if (pm == null) return "On account";
        return switch (pm) {
            case COD -> "Cash on delivery";
            case ONLINE -> "Online";
            case CREDIT -> "On account";
        };
    }

    private void setupCsvResponse(HttpServletResponse response, String filename) {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }

    private String esc(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private BigDecimal coalesce(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal toBD(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        String s = o.toString().trim();
        if (s.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot convert non-numeric value '" + o + "' to BigDecimal");
        }
    }

    private BigDecimal scale2(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
