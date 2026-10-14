package com.japstransfort.service;

import com.japstransfort.entity.*;
import com.japstransfort.repository.InventoryRepository;
import com.japstransfort.repository.OrderRepository;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.ShipmentRepository;
import com.japstransfort.repository.StockMovementRepository;
import com.japstransfort.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for administrative order management, approval workflow with inventory deduction,
 * and order cancellation with stock reversal.
 */
@Service
public class OrderAdminService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final ResellerRepository resellerRepository;
    private final ShipmentRepository shipmentRepository;
    private final com.japstransfort.repository.PaymentRepository paymentRepository;
    private final CommissionService commissionService;

    public OrderAdminService(OrderRepository orderRepository,
                             InventoryRepository inventoryRepository,
                             StockMovementRepository stockMovementRepository,
                             WarehouseRepository warehouseRepository,
                             ResellerRepository resellerRepository,
                             ShipmentRepository shipmentRepository,
                             com.japstransfort.repository.PaymentRepository paymentRepository,
                             CommissionService commissionService) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.warehouseRepository = warehouseRepository;
        this.resellerRepository = resellerRepository;
        this.shipmentRepository = shipmentRepository;
        this.paymentRepository = paymentRepository;
        this.commissionService = commissionService;
    }

    /**
     * Lists paginated orders filtered by search term, status, and payment status, newest first.
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrders(String search, String status, String paymentStatus, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? size : 10,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "orderId"))
        );

        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        boolean statusNull = true;
        OrderStatus orderStatus = OrderStatus.pending; // fallback dummy for non-null parameter binding

        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status.trim())) {
            try {
                orderStatus = OrderStatus.valueOf(status.trim().toLowerCase());
                statusNull = false;
            } catch (IllegalArgumentException ignored) {
                // Unknown status string, treat as all statuses
                statusNull = true;
            }
        }

        boolean paymentStatusNull = true;
        PaymentStatus payStatus = PaymentStatus.unpaid;

        if (paymentStatus != null && !paymentStatus.trim().isEmpty() && !"all".equalsIgnoreCase(paymentStatus.trim())) {
            try {
                payStatus = PaymentStatus.valueOf(paymentStatus.trim().toLowerCase());
                paymentStatusNull = false;
            } catch (IllegalArgumentException ignored) {
                paymentStatusNull = true;
            }
        }

        return orderRepository.findWithFilters(searchParam, statusNull, orderStatus, paymentStatusNull, payStatus, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrders(String search, String status, int page, int size) {
        return getOrders(search, status, null, page, size);
    }

    /**
     * Retrieves an order with its items, products, reseller, and warehouse eagerly loaded.
     */
    @Transactional(readOnly = true)
    public Order getOrderDetail(Integer orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null.");
        }
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found (ID " + orderId + ")."));
    }

    /**
     * Approves a pending order:
     * - Locks the order and checks status is pending
     * - Locks the reseller record
     * - Checks stock levels at the selected warehouse for all items with PESSIMISTIC_WRITE locks
     * - If any items are short, aborts with an informative error
     * - Deducts warehouse inventory and records 'out' stock movements (negative quantity)
     * - Increases reseller outstanding balance by order total
     * - Updates order status to 'approved' and records warehouse assignment
     */
    @Transactional
    public Order approveOrder(Integer orderId, Integer warehouseId, User admin) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null.");
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse must be selected.");
        }

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found (ID " + orderId + ")."));

        if (order.getStatus() != OrderStatus.pending) {
            throw new IllegalStateException("Order is not in pending status. Current status: " + order.getStatus());
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found (ID " + warehouseId + ")."));

        if (order.getReseller() == null) {
            throw new IllegalStateException("Order #" + order.getOrderNo() + " has no associated reseller.");
        }

        Reseller reseller = resellerRepository.findByIdWithLock(order.getReseller().getResellerId())
                .orElseThrow(() -> new IllegalStateException("Reseller not found (ID " + order.getReseller().getResellerId() + ")."));

        // Collect shortfalls across all items with locked inventory rows
        Map<Integer, Inventory> lockedInventories = new LinkedHashMap<>();
        Map<Integer, Integer> availableStock = new LinkedHashMap<>();

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null) continue;
            Integer productId = product.getProductId();
            if (!lockedInventories.containsKey(productId)) {
                Optional<Inventory> invOpt = inventoryRepository.findByIdWithLock(warehouseId, productId);
                Inventory inv = invOpt.orElse(null);
                lockedInventories.put(productId, inv);
                availableStock.put(productId, (inv != null && inv.getQuantity() != null) ? inv.getQuantity() : 0);
            }
        }

        List<String> shortfalls = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null) continue;
            Integer productId = product.getProductId();
            int orderedQty = item.getQuantity() != null ? item.getQuantity() : 0;
            int currentAvail = availableStock.getOrDefault(productId, 0);

            if (currentAvail < orderedQty) {
                int shortBy = orderedQty - currentAvail;
                shortfalls.add(String.format("%s (Ordered: %d, Available: %d, Short by: %d)",
                        product.getName(), orderedQty, currentAvail, shortBy));
            } else {
                availableStock.put(productId, currentAvail - orderedQty);
            }
        }

        if (!shortfalls.isEmpty()) {
            throw new IllegalStateException("Insufficient stock in warehouse '" + warehouse.getName() + "': "
                    + String.join("; ", shortfalls));
        }

        // Deduct inventory and record stock movements
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null) continue;
            Integer productId = product.getProductId();
            Inventory inventory = lockedInventories.get(productId);
            int currentQty = (inventory != null && inventory.getQuantity() != null) ? inventory.getQuantity() : 0;
            int orderedQty = item.getQuantity() != null ? item.getQuantity() : 0;

            inventory.setQuantity(currentQty - orderedQty);
            inventoryRepository.save(inventory);

            StockMovement movement = new StockMovement(
                    warehouse,
                    product,
                    MoveType.out,
                    -orderedQty,
                    order.getOrderNo(),
                    admin
            );
            stockMovementRepository.save(movement);
        }

        // Update Order and Reseller balance
        order.setWarehouse(warehouse);
        order.setStatus(OrderStatus.approved);

        BigDecimal currentBalance = reseller.getBalance() != null ? reseller.getBalance() : BigDecimal.ZERO;
        BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
        reseller.setBalance(currentBalance.add(orderTotal));

        orderRepository.save(order);
        resellerRepository.save(reseller);
        orderRepository.touchUpdatedAt(orderId);

        return order;
    }

    /**
     * Cancels an order (must be pending or approved):
     * - If pending: marks cancelled directly
     * - If approved: reverses stock deduction (MoveType.return_, positive qty) and subtracts order total from reseller balance
     * Saves cancel_reason, cancelled_by, cancelled_at on both paths.
     */
    @Transactional
    public Order cancelOrder(Integer orderId, String cancelReason, String cancelledBy, User admin) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null.");
        }

        // Server-side reason validation
        String reason = cancelReason != null ? cancelReason.trim() : "";
        if (reason.isEmpty() || reason.length() > 255) {
            throw new IllegalArgumentException("A valid cancellation reason (max 255 characters) is required.");
        }

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found (ID " + orderId + ")."));

        if (paymentRepository.existsByOrder_OrderId(orderId)) {
            throw new IllegalStateException("This order has payments recorded. Cancelling is not available yet.");
        }

        if (order.getStatus() != OrderStatus.pending && order.getStatus() != OrderStatus.approved) {
            String current = order.getStatus().name();
            if ("packed".equals(current) || "shipped".equals(current) || "delivered".equals(current)) {
                throw new IllegalStateException(
                        "Order cannot be cancelled because it is already " + current +
                        ". Only pending or approved orders can be cancelled.");
            }
            throw new IllegalStateException("Only pending or approved orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setCancelReason(reason);
        order.setCancelledBy(cancelledBy != null ? cancelledBy : "ADMIN");
        order.setCancelledAt(LocalDateTime.now());

        if (order.getStatus() == OrderStatus.pending) {
            order.setStatus(OrderStatus.cancelled);
            orderRepository.save(order);
            orderRepository.touchUpdatedAt(orderId);
            return order;
        }

        // Status is approved: reverse inventory and balance
        Warehouse warehouse = order.getWarehouse();
        if (warehouse == null) {
            throw new IllegalStateException("Approved order #" + order.getOrderNo() + " does not have an assigned warehouse.");
        }

        if (order.getReseller() == null) {
            throw new IllegalStateException("Order #" + order.getOrderNo() + " has no associated reseller.");
        }

        Reseller reseller = resellerRepository.findByIdWithLock(order.getReseller().getResellerId())
                .orElseThrow(() -> new IllegalStateException("Reseller not found (ID " + order.getReseller().getResellerId() + ")."));

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null) continue;
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

            Optional<Inventory> invOpt = inventoryRepository.findByIdWithLock(warehouse.getWarehouseId(), product.getProductId());
            Inventory inventory = invOpt.orElseGet(() -> new Inventory(warehouse, product, 0));
            int currentQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            inventory.setQuantity(currentQty + quantity);
            inventoryRepository.save(inventory);

            StockMovement movement = new StockMovement(
                    warehouse,
                    product,
                    MoveType.return_,
                    quantity,
                    order.getOrderNo(),
                    admin
            );
            stockMovementRepository.save(movement);
        }

        BigDecimal currentBalance = reseller.getBalance() != null ? reseller.getBalance() : BigDecimal.ZERO;
        BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
        reseller.setBalance(currentBalance.subtract(orderTotal));
        resellerRepository.save(reseller);

        order.setStatus(OrderStatus.cancelled);
        orderRepository.save(order);
        orderRepository.touchUpdatedAt(orderId);

        return order;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> findAllWarehouses() {
        return warehouseRepository.findAll();
    }

    /**
     * Marks an approved order as packed. Enforces approved → packed transition.
     */
    @Transactional
    public Order markPacked(Integer orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found (ID " + orderId + ")."));

        if (order.getStatus() != OrderStatus.approved) {
            throw new IllegalStateException(
                    "Order cannot be marked as packed. Expected status: approved, current: " + order.getStatus() + ".");
        }

        order.setStatus(OrderStatus.packed);
        orderRepository.save(order);
        orderRepository.touchUpdatedAt(orderId);
        return order;
    }

    /**
     * Ships a packed order: inserts a shipments row, sets order status shipped.
     * Enforces packed → shipped, and that no shipment row exists yet.
     */
    @Transactional
    public Order shipOrder(Integer orderId, String courier, String trackingNo) {
        String courierTrimmed = courier != null ? courier.trim() : "";
        if (courierTrimmed.isEmpty()) {
            throw new IllegalArgumentException("Courier name is required.");
        }

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found (ID " + orderId + ")."));

        if (order.getStatus() != OrderStatus.packed) {
            throw new IllegalStateException(
                    "Order cannot be shipped. Expected status: packed, current: " + order.getStatus() + ".");
        }

        // Guard: one shipment per order
        if (shipmentRepository.findByOrderIdWithLock(orderId).isPresent()) {
            throw new IllegalStateException("A shipment record already exists for this order.");
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setCourier(courierTrimmed);
        String tn = trackingNo != null ? trackingNo.trim() : null;
        shipment.setTrackingNo(tn != null && tn.isEmpty() ? null : tn);
        shipment.setStatus("shipped");
        shipment.setShippedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);

        order.setStatus(OrderStatus.shipped);
        orderRepository.save(order);
        orderRepository.touchUpdatedAt(orderId);
        return order;
    }

    /**
     * Marks a shipped order as delivered: updates shipment status and delivered_at, sets order delivered.
     */
    @Transactional
    public Order markDelivered(Integer orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found (ID " + orderId + ")."));

        if (order.getStatus() != OrderStatus.shipped) {
            throw new IllegalStateException(
                    "Order cannot be marked as delivered. Expected status: shipped, current: " + order.getStatus() + ".");
        }

        Shipment shipment = shipmentRepository.findByOrderIdWithLock(orderId)
                .orElseThrow(() -> new IllegalStateException("Shipment record not found for this order."));

        shipment.setStatus("delivered");
        shipment.setDeliveredAt(LocalDateTime.now());
        shipmentRepository.save(shipment);

        order.setStatus(OrderStatus.delivered);
        orderRepository.save(order);
        orderRepository.touchUpdatedAt(orderId);

        commissionService.createCommissionIfEligible(order);

        return order;
    }

    /**
     * Returns the shipment for the given order, or empty if not yet created.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Shipment> getShipmentForOrder(Integer orderId) {
        return shipmentRepository.findByOrder_OrderId(orderId);
    }
}
