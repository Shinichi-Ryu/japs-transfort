package com.japstransfort.service;

import com.japstransfort.dto.ProductFormDto;
import com.japstransfort.dto.ProductImageMeta;
import com.japstransfort.entity.Category;
import com.japstransfort.entity.Product;
import com.japstransfort.entity.ProductImage;
import com.japstransfort.repository.CategoryRepository;
import com.japstransfort.repository.ProductImageRepository;
import com.japstransfort.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Service encapsulating business logic for Product management.
 */
@Service
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "productId", "name", "sku", "costPrice", "srp", "reorderLevel", "isActive"
    );

    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final int  MAX_SIDE_PX    = 800;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional(readOnly = true)
    public Page<Product> getProducts(String search, Integer categoryId, String status,
                                     int page, int size, String sortField, String sortDir) {
        // Map status filter
        Boolean isActive = null;
        if ("active".equalsIgnoreCase(status)) {
            isActive = true;
        } else if ("inactive".equalsIgnoreCase(status)) {
            isActive = false;
        }

        if ("price".equalsIgnoreCase(sortField)) {
            sortField = "srp";
        }

        // Validate sort field to prevent invalid property injection
        String validSortField = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 10, Sort.by(direction, validSortField));

        return productRepository.findWithFilters(
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                categoryId,
                isActive,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Product findById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
    }

    @Transactional
    public Product create(ProductFormDto dto) {
        String trimmedSku = dto.getSku() != null ? dto.getSku().trim() : "";
        if (trimmedSku.isEmpty()) {
            do {
                Long nextVal = productRepository.getNextSkuSequenceValue();
                trimmedSku = String.format("PRD-%06d", nextVal);
            } while (productRepository.existsBySkuIgnoreCase(trimmedSku));
        } else {
            if (productRepository.existsBySkuIgnoreCase(trimmedSku)) {
                throw new IllegalArgumentException("A product with SKU '" + trimmedSku + "' already exists.");
            }
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Selected category does not exist."));

        Product product = new Product();
        product.setSku(trimmedSku);
        product.setName(dto.getName().trim());
        product.setCategory(category);
        product.setUnit(dto.getUnit() != null ? dto.getUnit().trim() : "pc");
        product.setCostPrice(dto.getCostPrice());
        product.setSrp(dto.getSrp());
        product.setReorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : 10);
        product.setIsActive(true);

        return productRepository.save(product);
    }

    @Transactional
    public Product create(ProductFormDto dto, MultipartFile photoFile) {
        Product product = create(dto);
        if (photoFile != null && !photoFile.isEmpty()) {
            saveImage(product.getProductId(), photoFile);
        }
        return product;
    }

    @Transactional
    public Product update(Integer id, ProductFormDto dto) {
        Product product = findById(id);

        String trimmedSku = dto.getSku() != null ? dto.getSku().trim() : "";
        if (trimmedSku.isEmpty()) {
            throw new IllegalArgumentException("SKU is required.");
        }

        productRepository.findBySkuIgnoreCase(trimmedSku).ifPresent(existing -> {
            if (!existing.getProductId().equals(id)) {
                throw new IllegalArgumentException("Another product with SKU '" + trimmedSku + "' already exists.");
            }
        });

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Selected category does not exist."));

        product.setSku(trimmedSku);
        product.setName(dto.getName().trim());
        product.setCategory(category);
        product.setUnit(dto.getUnit() != null ? dto.getUnit().trim() : "pc");
        product.setCostPrice(dto.getCostPrice());
        product.setSrp(dto.getSrp());
        product.setReorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : 10);

        return productRepository.save(product);
    }

    @Transactional
    public Product update(Integer id, ProductFormDto dto, MultipartFile photoFile, Boolean removePhoto) {
        Product product = update(id, dto);
        if (Boolean.TRUE.equals(removePhoto)) {
            removeImage(id);
        } else if (photoFile != null && !photoFile.isEmpty()) {
            saveImage(id, photoFile);
        }
        return product;
    }

    /**
     * Soft delete: preserves audit history and stock movement records while deactivating the product.
     */
    @Transactional
    public void softDelete(Integer id) {
        Product product = findById(id);
        product.setIsActive(false);
        productRepository.save(product);
    }

    /**
     * Reactivates a previously soft-deleted product.
     */
    @Transactional
    public void restore(Integer id) {
        Product product = findById(id);
        product.setIsActive(true);
        productRepository.save(product);
    }

    // ── Image helpers ─────────────────────────────────────────────────────────

    /** Lightweight metadata for list pages; never loads byte[] data. */
    @Transactional(readOnly = true)
    public ProductImageMeta getImageMeta(Integer productId) {
        Optional<java.time.LocalDateTime> ts = productImageRepository.findUpdatedAtByProductId(productId);
        return new ProductImageMeta(productId, ts.isPresent(), ts.orElse(null));
    }

    /** Full image for the serving endpoint. */
    @Transactional(readOnly = true)
    public Optional<ProductImage> getImage(Integer productId) {
        return productImageRepository.findById(productId);
    }

    /**
     * Validates, processes, and saves (or replaces) a product image.
     * Runs in the same transaction as the product save when called from create/update.
     */
    @Transactional
    public void saveImage(Integer productId, MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        // Magic-byte detection (JPEG: FF D8 FF; PNG: 89 50 4E 47)
        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file.");
        }
        if (rawBytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image exceeds 2 MB limit.");
        }
        if (!isJpegOrPng(rawBytes)) {
            throw new IllegalArgumentException("Only JPEG and PNG images are accepted (detected from file content).");
        }

        // Decode with ImageIO
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(rawBytes));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not decode image file.");
        }
        if (original == null) {
            throw new IllegalArgumentException("File could not be decoded as a valid image.");
        }

        // Scale down if largest side > MAX_SIDE_PX; never enlarge
        BufferedImage scaled = scaleDown(original, MAX_SIDE_PX);

        // Flatten transparency onto white background and re-encode as JPEG
        byte[] jpegBytes = encodeJpeg(scaled, 0.85f);

        Optional<ProductImage> opt = productImageRepository.findById(productId);
        if (opt.isPresent()) {
            ProductImage img = opt.get();
            img.setContentType("image/jpeg");
            img.setData(jpegBytes);
            img.setUpdatedAt(LocalDateTime.now());
            productImageRepository.save(img);
        } else {
            ProductImage img = new ProductImage(productId, "image/jpeg", jpegBytes, LocalDateTime.now());
            img.setNew(true);
            productImageRepository.save(img);
        }
    }

    @Transactional
    public void removeImage(Integer productId) {
        if (productImageRepository.existsById(productId)) {
            productImageRepository.deleteById(productId);
        }
    }

    // ── Private image utilities ───────────────────────────────────────────────

    private boolean isJpegOrPng(byte[] bytes) {
        if (bytes.length < 4) return false;
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        return (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47;
    }

    private BufferedImage scaleDown(BufferedImage src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxSide && h <= maxSide) return src;
        double ratio = (double) maxSide / Math.max(w, h);
        int nw = (int) Math.max(1, w * ratio);
        int nh = (int) Math.max(1, h * ratio);
        BufferedImage result = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return result;
    }

    private byte[] encodeJpeg(BufferedImage img, float quality) {
        // Flatten onto white background (handles PNG with alpha channel)
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(quality);
                    }
                    writer.write(null, new IIOImage(rgb, null, null), param);
                    ios.flush();
                } finally {
                    writer.dispose();
                }
            }
            if (baos.size() == 0) {
                boolean success = ImageIO.write(rgb, "jpg", baos);
                if (!success) {
                    throw new IllegalArgumentException("Failed to encode image as JPEG.");
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to encode image as JPEG.", e);
        }
    }
}

