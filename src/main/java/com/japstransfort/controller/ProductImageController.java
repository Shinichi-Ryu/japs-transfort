package com.japstransfort.controller;

import com.japstransfort.entity.ProductImage;
import com.japstransfort.service.ProductService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Serves product photos at GET /products/{id}/image for any authenticated user.
 */
@RestController
public class ProductImageController {

    private final ProductService productService;

    public ProductImageController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping({"/products/{id}/image", "/admin/products/{id}/image"})
    public ResponseEntity<byte[]> serveImage(@PathVariable("id") Integer id) {
        Optional<ProductImage> opt = productService.getImage(id);
        if (opt.isEmpty() || opt.get().getData() == null || opt.get().getData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        ProductImage img = opt.get();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .contentLength(img.getData().length)
                .cacheControl(CacheControl.maxAge(86400, TimeUnit.SECONDS).cachePrivate())
                .body(img.getData());
    }
}
