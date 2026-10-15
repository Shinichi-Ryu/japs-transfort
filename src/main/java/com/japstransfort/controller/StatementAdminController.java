package com.japstransfort.controller;

import com.japstransfort.dto.StatementDto;
import com.japstransfort.dto.StatementRowDto;
import com.japstransfort.service.StatementService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Admin endpoint: /admin/resellers/{id}/statement
 */
@Controller
@RequestMapping("/admin/resellers")
@PreAuthorize("hasRole('ADMIN')")
public class StatementAdminController {

    private static final int MAX_RANGE_DAYS = 366;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StatementService statementService;

    public StatementAdminController(StatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping("/{id}/statement")
    public String adminStatement(
            @PathVariable("id") Integer resellerId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model,
            HttpServletResponse response) {

        LocalDate[] dates = resolveDates(from, to);
        from = dates[0];
        to = dates[1];

        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("backUrl", "/admin/resellers");
        model.addAttribute("isAdmin", true);
        model.addAttribute("resellerId", resellerId);

        String errorMsg = validateRange(from, to);
        if (errorMsg != null) {
            model.addAttribute("errorMessage", errorMsg);
            return "statement/view";
        }

        Optional<StatementDto> opt = statementService.buildStatement(resellerId, from, to);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseller not found");
        }

        model.addAttribute("statement", opt.get());
        model.addAttribute("company", opt.get().getCompany());
        return "statement/view";
    }

    @GetMapping("/{id}/statement/export")
    public void adminStatementExport(
            @PathVariable("id") Integer resellerId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {

        LocalDate[] dates = resolveDates(from, to);
        from = dates[0];
        to = dates[1];

        if (validateRange(from, to) != null) {
            response.sendError(HttpStatus.BAD_REQUEST.value());
            return;
        }

        Optional<StatementDto> opt = statementService.buildStatement(resellerId, from, to);
        if (opt.isEmpty()) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }

        writeCsv(response, opt.get(), from, to);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private LocalDate[] resolveDates(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusMonths(3).withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        return new LocalDate[]{from, to};
    }

    private String validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return "The 'From' date cannot be after the 'To' date.";
        }
        if (to.toEpochDay() - from.toEpochDay() > MAX_RANGE_DAYS) {
            return "The date range cannot exceed " + MAX_RANGE_DAYS + " days.";
        }
        return null;
    }

    private void writeCsv(HttpServletResponse response, StatementDto stmt, LocalDate from, LocalDate to) throws IOException {
        String filename = "statement_" + stmt.getResellerId() + "_" + from + "_to_" + to + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF'); // UTF-8 BOM
        writer.println("\"Date\",\"Type\",\"Reference\",\"Debit\",\"Credit\",\"Balance\"");

        // Opening balance row
        writer.println(String.join(",",
                esc(from.format(DATE_FMT)), esc("Opening Balance"), esc(""),
                esc(""), esc(""), esc(scale2(stmt.getOpeningBalance()).toPlainString())));

        for (StatementRowDto row : stmt.getRows()) {
            String date = row.getDate() != null ? row.getDate().format(DATE_FMT) : "";
            String debit = row.getDebit() != null ? scale2(row.getDebit()).toPlainString() : "";
            String credit = row.getCredit() != null ? scale2(row.getCredit()).toPlainString() : "";
            String balance = row.getBalance() != null ? scale2(row.getBalance()).toPlainString() : "";
            writer.println(String.join(",",
                    esc(date), esc(row.getType()), esc(row.getReference()),
                    esc(debit), esc(credit), esc(balance)));
        }

        // Closing balance row
        writer.println(String.join(",",
                esc(to.format(DATE_FMT)), esc("Closing Balance"), esc(""),
                esc(""), esc(""), esc(scale2(stmt.getClosingBalance()).toPlainString())));

        writer.flush();
    }

    private String esc(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private BigDecimal scale2(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
