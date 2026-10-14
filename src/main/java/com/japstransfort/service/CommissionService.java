package com.japstransfort.service;

import com.japstransfort.entity.Commission;
import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import com.japstransfort.entity.PaymentStatus;
import com.japstransfort.entity.Reseller;
import com.japstransfort.entity.ResellerTier;
import com.japstransfort.repository.CommissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service for reseller commission calculations, admin approvals, and reporting.
 */
@Service
public class CommissionService {

    private final CommissionRepository commissionRepository;

    public CommissionService(CommissionRepository commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    /**
     * Shared method called inside existing transactions of recordPayment and markDelivered.
     * Creates a pending commission if the order is delivered, fully paid, reseller tier has commission % > 0,
     * and no commission exists yet for this order.
     * Duplicate rejection by the unique index is treated as already created without failing the main action.
     */
    @Transactional
    public void createCommissionIfEligible(Order order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }

        if (order.getStatus() != OrderStatus.delivered) {
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.paid) {
            return;
        }

        Reseller reseller = order.getReseller();
        if (reseller == null) {
            return;
        }

        ResellerTier tier = reseller.getTier();
        if (tier == null || tier.getCommissionPct() == null || tier.getCommissionPct().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (commissionRepository.existsByOrder_OrderId(order.getOrderId())) {
            return;
        }

        BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
        BigDecimal amount = orderTotal.multiply(tier.getCommissionPct())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        try {
            commissionRepository.insertIfNotExists(reseller.getResellerId(), order.getOrderId(), amount);
        } catch (Exception ex) {
            // If the unique index rejects a duplicate, treat it as already created and do not fail the main action.
        }
    }

    /**
     * Marks a pending commission as paid.
     * Re-checks inside the transaction that status is still pending.
     * Double clicks or stale pages do not perform duplicate actions.
     * A paid commission cannot be reverted.
     */
    @Transactional
    public Commission markAsPaid(Integer commissionId) {
        if (commissionId == null) {
            throw new IllegalArgumentException("Commission ID must not be null.");
        }

        Commission commission = commissionRepository.findByIdWithLock(commissionId)
                .orElseThrow(() -> new IllegalArgumentException("Commission not found (ID " + commissionId + ")."));

        if (!"pending".equalsIgnoreCase(commission.getStatus())) {
            return commission;
        }

        commission.setStatus("paid");
        return commissionRepository.save(commission);
    }

    /**
     * Retrieves paginated commissions for admin view filtered by search and status, newest first.
     */
    @Transactional(readOnly = true)
    public Page<Commission> getCommissions(String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? size : 10,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "commissionId"))
        );

        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        boolean statusNull = true;
        String statusParam = null;
        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status.trim())) {
            statusNull = false;
            statusParam = status.trim().toLowerCase();
        }

        return commissionRepository.findWithFilters(searchParam, statusNull, statusParam, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPending() {
        BigDecimal sum = commissionRepository.sumAmountByStatus("pending");
        return sum != null ? sum.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaid() {
        BigDecimal sum = commissionRepository.sumAmountByStatus("paid");
        return sum != null ? sum.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Optional<Commission> getCommissionForOrder(Integer orderId) {
        if (orderId == null) return Optional.empty();
        return commissionRepository.findByOrder_OrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<Commission> getCommissionsForReseller(Integer resellerId) {
        if (resellerId == null) return List.of();
        return commissionRepository.findByReseller_ResellerIdOrderByCreatedAtDesc(resellerId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPendingTotalForReseller(Integer resellerId) {
        if (resellerId == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sum = commissionRepository.sumAmountByResellerIdAndStatus(resellerId, "pending");
        return sum != null ? sum.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPaidTotalForReseller(Integer resellerId) {
        if (resellerId == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sum = commissionRepository.sumAmountByResellerIdAndStatus(resellerId, "paid");
        return sum != null ? sum.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
