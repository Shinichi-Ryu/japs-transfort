package com.japstransfort.service;

import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderMessage;
import com.japstransfort.entity.User;
import com.japstransfort.repository.OrderMessageRepository;
import com.japstransfort.repository.OrderRepository;
import com.japstransfort.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing per-order messages between admin and resellers.
 */
@Service
public class OrderMessageService {

    private final OrderMessageRepository orderMessageRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderMessageService(OrderMessageRepository orderMessageRepository,
                               OrderRepository orderRepository,
                               UserRepository userRepository) {
        this.orderMessageRepository = orderMessageRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderMessage> listForOrder(Integer orderId) {
        if (orderId == null) {
            return List.of();
        }
        return orderMessageRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    @Transactional
    public OrderMessage sendAsAdmin(Integer orderId, Integer adminUserId, String body) {
        validateBody(body);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        User user = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + adminUserId));

        OrderMessage message = new OrderMessage(
                order,
                user,
                "ADMIN",
                body.trim(),
                LocalDateTime.now(),
                true,   // read by admin immediately
                false   // unread by reseller
        );

        return orderMessageRepository.save(message);
    }

    @Transactional
    public OrderMessage sendAsReseller(Integer orderId, Integer resellerId, Integer resellerUserId, String body) {
        validateBody(body);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getReseller() == null || !order.getReseller().getResellerId().equals(resellerId)) {
            throw new IllegalArgumentException("Order does not belong to this reseller.");
        }

        User user = userRepository.findById(resellerUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + resellerUserId));

        OrderMessage message = new OrderMessage(
                order,
                user,
                "RESELLER",
                body.trim(),
                LocalDateTime.now(),
                false,  // unread by admin
                true    // read by reseller immediately
        );

        return orderMessageRepository.save(message);
    }

    @Transactional
    public void markReadFor(Integer orderId, String role) {
        if (orderId == null || role == null) {
            return;
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            orderMessageRepository.markAllAsReadForAdmin(orderId);
        } else if ("RESELLER".equalsIgnoreCase(role)) {
            orderMessageRepository.markAllAsReadForReseller(orderId);
        }
    }

    @Transactional(readOnly = true)
    public long countUnreadForAdmin(Integer orderId) {
        if (orderId == null) return 0L;
        return orderMessageRepository.countUnreadForAdminByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public long countUnreadTotalForAdmin() {
        return orderMessageRepository.countUnreadForAdminTotal();
    }

    @Transactional(readOnly = true)
    public long countUnreadTotalForReseller(Integer resellerId) {
        if (resellerId == null) return 0L;
        return orderMessageRepository.countUnreadForResellerTotal(resellerId);
    }

    @Transactional(readOnly = true)
    public long countUnreadForAdmin() {
        return orderMessageRepository.countUnreadForAdmin();
    }

    @Transactional(readOnly = true)
    public long countUnreadForReseller(Integer resellerId) {
        if (resellerId == null) return 0L;
        return orderMessageRepository.countUnreadForReseller(resellerId);
    }

    @Transactional(readOnly = true)
    public long countUnreadMessages(String role, Integer resellerId) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return countUnreadForAdmin();
        } else if ("RESELLER".equalsIgnoreCase(role)) {
            return countUnreadForReseller(resellerId);
        }
        return 0L;
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> getUnreadCountsMapForAdmin() {
        Map<Integer, Long> map = new HashMap<>();
        List<Object[]> rows = orderMessageRepository.countUnreadGroupedByOrderForAdmin();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] instanceof Integer oid && row[1] instanceof Long cnt) {
                map.put(oid, cnt);
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> getUnreadCountsMapForReseller(Integer resellerId) {
        Map<Integer, Long> map = new HashMap<>();
        if (resellerId == null) return map;
        List<Object[]> rows = orderMessageRepository.countUnreadGroupedByOrderForReseller(resellerId);
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] instanceof Integer oid && row[1] instanceof Long cnt) {
                map.put(oid, cnt);
            }
        }
        return map;
    }

    private void validateBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }
        if (body.trim().length() > 3000) {
            throw new IllegalArgumentException("Message cannot exceed 3000 characters.");
        }
    }
}
