package com.japstransfort.service;

import com.japstransfort.dto.InventoryRowDto;
import com.japstransfort.entity.*;
import com.japstransfort.repository.InventoryRepository;
import com.japstransfort.repository.ProductRepository;
import com.japstransfort.repository.StockMovementRepository;
import com.japstransfort.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling inventory management, receiving stock, adjustments, and movements.
 */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            StockMovementRepository stockMovementRepository,
                            ProductRepository productRepository,
                            WarehouseRepository warehouseRepository) {
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    /**
     * Lists one row per product and warehouse with current quantity.
     * Includes products with no inventory row (quantity 0).
     */
    @Transactional(readOnly = true)
    public Page<InventoryRowDto> getInventoryRows(String search, Integer warehouseId,
                                                 int page, int size,
                                                 String sortField, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String prop = switch (sortField != null ? sortField.toLowerCase() : "") {
            case "sku" -> "p.sku";
            case "warehouse" -> "w.name";
            case "quantity" -> "i.quantity";
            default -> "p.name";
        };

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? size : 10,
                Sort.by(direction, prop).and(Sort.by(Sort.Direction.ASC, "w.name"))
        );

        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return inventoryRepository.findInventoryRows(trimmedSearch, warehouseId, pageable);
    }

    /**
     * Receives stock for a product at a warehouse.
     * Updates inventory quantity and records a stock_movements row of type 'in'.
     */
    @Transactional
    public StockMovement receiveStock(Integer productId, Integer warehouseId,
                                      int quantity, String reference, User admin) {
        if (productId == null) {
            throw new IllegalArgumentException("Product must be selected.");
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse must be selected.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Received quantity must be at least 1.");
        }
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference is required.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found (ID " + productId + ")."));
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new IllegalArgumentException("Product '" + product.getName() + "' is inactive and cannot receive stock.");
        }
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found (ID " + warehouseId + ")."));

        Inventory inventory = inventoryRepository.findById_WarehouseIdAndId_ProductId(warehouseId, productId)
                .orElseGet(() -> new Inventory(warehouse, product, 0));

        int currentQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        inventory.setQuantity(currentQty + quantity);
        inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement();
        movement.setWarehouse(warehouse);
        movement.setProduct(product);
        movement.setType(MoveType.in);
        movement.setQuantity(quantity);
        movement.setReference(reference.trim());
        movement.setCreatedBy(admin);

        return stockMovementRepository.save(movement);
    }

    /**
     * Adjusts stock for a product at a warehouse with either a new count or a +/- change.
     * Never allows quantity to go below 0.
     * Records a stock_movements row of type 'adjust' with the signed change.
     */
    @Transactional
    public StockMovement adjustStock(Integer productId, Integer warehouseId,
                                     String mode, int quantity, String reference, User admin) {
        if (productId == null) {
            throw new IllegalArgumentException("Product must be selected.");
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse must be selected.");
        }
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference is required.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found (ID " + productId + ")."));
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new IllegalArgumentException("Product '" + product.getName() + "' is inactive and cannot adjust stock.");
        }
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found (ID " + warehouseId + ")."));

        Inventory inventory = inventoryRepository.findById_WarehouseIdAndId_ProductId(warehouseId, productId)
                .orElseGet(() -> new Inventory(warehouse, product, 0));

        int currentQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int targetQty;
        int signedChange;

        if ("set".equalsIgnoreCase(mode) || "new_count".equalsIgnoreCase(mode)) {
            targetQty = quantity;
            signedChange = targetQty - currentQty;
        } else {
            // Relative change (+/-)
            signedChange = quantity;
            targetQty = currentQty + signedChange;
        }

        if (targetQty < 0) {
            throw new IllegalArgumentException(String.format(
                "Stock quantity cannot be less than 0. Current stock is %d, and requested adjustment (%s%d) results in %d.",
                currentQty, signedChange > 0 ? "+" : "", signedChange, targetQty));
        }

        inventory.setQuantity(targetQty);
        inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement();
        movement.setWarehouse(warehouse);
        movement.setProduct(product);
        movement.setType(MoveType.adjust);
        movement.setQuantity(signedChange);
        movement.setReference(reference.trim());
        movement.setCreatedBy(admin);

        return stockMovementRepository.save(movement);
    }

    /**
     * Searches active products by name or SKU (case-insensitive, limited results).
     */
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> searchActiveProducts(String query, int maxResults) {
        String trimmed = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        Pageable pageable = PageRequest.of(0, maxResults > 0 ? maxResults : 20, Sort.by(Sort.Direction.ASC, "name"));
        Page<Product> page = productRepository.findWithFilters(trimmed, null, true, pageable);

        return page.getContent().stream()
                .map(p -> {
                    java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", p.getProductId());
                    map.put("name", p.getName());
                    map.put("sku", p.getSku());
                    map.put("text", p.getName() + " (" + p.getSku() + ")");
                    return map;
                })
                .toList();
    }

    /**
     * Lists stock movements paginated, latest movements first.
     */
    @Transactional(readOnly = true)
    public Page<StockMovement> getMovements(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 10);
        return stockMovementRepository.findAllWithDetailsOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> findAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }
}
