package com.japstransfort.controller;

import com.japstransfort.dto.DailySalesDto;
import com.japstransfort.dto.PaymentMethodSalesDto;
import com.japstransfort.dto.ProductSalesDto;
import com.japstransfort.dto.ResellerSalesDto;
import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import com.japstransfort.entity.Payment;
import com.japstransfort.entity.PaymentMethod;
import com.japstransfort.repository.OrderRepository;
import com.japstransfort.repository.PaymentRepository;
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
 * Controller for admin sales reporting, analytics breakdowns, and CSV export.
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

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public SalesReportController(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/sales")
    public String salesReport(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "showAllResellers", defaultValue = "false") boolean showAllResellers,
            @RequestParam(value = "showAllProducts", defaultValue = "false") boolean showAllProducts,
            Model model) {

        if (from == null) {
            from = LocalDate.now().withDayOfMonth(1);
        }
        if (to == null) {
            to = LocalDate.now();
        }

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
        BigDecimal grossSales = orderRepository.sumGrossSales(SALES_STATUSES, startDateTime, endDateTime);
        if (grossSales == null) grossSales = BigDecimal.ZERO;

        BigDecimal totalCollected = paymentRepository.sumAmountByPaidAtBetween(startDateTime, endDateTime);
        if (totalCollected == null) totalCollected = BigDecimal.ZERO;

        BigDecimal averageOrderValue = (totalOrders > 0)
                ? grossSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("grossSales", grossSales);
        model.addAttribute("totalCollected", totalCollected);
        model.addAttribute("averageOrderValue", averageOrderValue);

        // Sales by Reseller
        List<ResellerSalesDto> allResellers = orderRepository.findSalesByReseller(SALES_STATUSES, startDateTime, endDateTime);
        boolean hasMoreResellers = allResellers.size() > 20;
        List<ResellerSalesDto> displayedResellers = (hasMoreResellers && !showAllResellers)
                ? allResellers.subList(0, 20)
                : allResellers;
        model.addAttribute("resellerSales", displayedResellers);
        model.addAttribute("hasMoreResellers", hasMoreResellers);
        model.addAttribute("totalResellersCount", allResellers.size());

        // Sales by Product
        List<ProductSalesDto> allProducts = orderRepository.findSalesByProduct(SALES_STATUSES, startDateTime, endDateTime);
        boolean hasMoreProducts = allProducts.size() > 20;
        List<ProductSalesDto> displayedProducts = (hasMoreProducts && !showAllProducts)
                ? allProducts.subList(0, 20)
                : allProducts;
        model.addAttribute("productSales", displayedProducts);
        model.addAttribute("hasMoreProducts", hasMoreProducts);
        model.addAttribute("totalProductsCount", allProducts.size());

        // Daily Sales
        List<Object[]> rawDaily = orderRepository.findDailySales(startDateTime, endDateTime);
        List<DailySalesDto> dailySales = rawDaily.stream().map(row -> {
            String date = String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            BigDecimal amount = row[2] instanceof BigDecimal bd ? bd : new BigDecimal(row[2].toString());
            return new DailySalesDto(date, count, amount);
        }).toList();
        model.addAttribute("dailySales", dailySales);

        // Sales by Payment Method (COD, Online, On account)
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

        if (from == null) {
            from = LocalDate.now().withDayOfMonth(1);
        }
        if (to == null) {
            to = LocalDate.now();
        }

        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.plusDays(1).atStartOfDay();

        List<Order> orders = (from.isAfter(to))
                ? Collections.emptyList()
                : orderRepository.findSalesOrdersForExport(SALES_STATUSES, startDateTime, endDateTime);

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"sales_report_" + from + "_to_" + to + ".csv\"");

        PrintWriter writer = response.getWriter();
        // UTF-8 BOM so Excel opens it with proper encoding
        writer.write('\uFEFF');

        writer.println("\"Order No\",\"Date\",\"Reseller\",\"Payment Method\",\"Status\",\"Payment Status\",\"Total\",\"Amount Paid\"");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Order ord : orders) {
            String orderNo = escapeCsv(ord.getOrderNo());
            String date = escapeCsv(ord.getCreatedAt() != null ? ord.getCreatedAt().format(dtf) : "");
            String resellerName = escapeCsv(ord.getReseller() != null ? ord.getReseller().getBusinessName() : "");

            String paymentMethodName = "On account";
            if (ord.getPaymentMethod() == PaymentMethod.COD) {
                paymentMethodName = "Cash on delivery";
            } else if (ord.getPaymentMethod() == PaymentMethod.ONLINE) {
                paymentMethodName = "Online";
            }
            String paymentMethod = escapeCsv(paymentMethodName);

            String status = escapeCsv(ord.getStatus() != null ? ord.getStatus().name() : "");
            String paymentStatus = escapeCsv(ord.getPaymentStatus() != null ? ord.getPaymentStatus().name() : "");

            BigDecimal total = ord.getTotal() != null ? ord.getTotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
            BigDecimal paid = BigDecimal.ZERO;
            if (ord.getPayments() != null) {
                paid = ord.getPayments().stream()
                        .map(Payment::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            writer.println(String.join(",",
                    orderNo,
                    date,
                    resellerName,
                    paymentMethod,
                    status,
                    paymentStatus,
                    total.toPlainString(),
                    paid.toPlainString()
            ));
        }

        writer.flush();
    }

    private List<PaymentMethodSalesDto> defaultPaymentMethodSales() {
        return List.of(
                new PaymentMethodSalesDto(PaymentMethod.COD, 0L, BigDecimal.ZERO),
                new PaymentMethodSalesDto(PaymentMethod.ONLINE, 0L, BigDecimal.ZERO),
                new PaymentMethodSalesDto(PaymentMethod.CREDIT, 0L, BigDecimal.ZERO)
        );
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
