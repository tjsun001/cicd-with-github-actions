package com.thurman.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thurman.exception.ResourceNotFound;
import com.thurman.outbox.OutboxEvent;
import com.thurman.outbox.OutboxEventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageService productImageService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository,
                          ProductImageService productImageService,
                          OutboxEventRepository outboxEventRepository,
                          ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.productImageService = productImageService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    // READ: cache the full list (short TTL configured in application.yml)
    @Cacheable(cacheNames = "products_all")
    public List<ProductResponse> getAllProducts() {
        if (log.isInfoEnabled()) {
            log.info("DB HIT: getAllProducts()");
        }
        System.out.println(">>> DB HIT: getAllProducts()");
        return productRepository.findAll().stream()
                .map(mapToResponse())
                .collect(Collectors.toList());
    }

    // READ: cache by ID
    @Cacheable(cacheNames = "products_by_id", key = "#id")
    public ProductResponse getProductById(UUID id) {
        if (log.isInfoEnabled()) {
            log.info("DB HIT: getProductById)");
        }
        return productRepository.findById(id)
                .map(mapToResponse())
                .orElseThrow(() -> new ResourceNotFound(
                        "product with id [" + id + "] not found"
                ));
    }

    // WRITE: evict caches
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "products_by_id", key = "#id"),
            @CacheEvict(cacheNames = "products_all", allEntries = true)
    })
    public void deleteProductById(UUID id) {
        boolean exists = productRepository.existsById(id);
        if (!exists) {
            throw new ResourceNotFound(
                    "product with id [" + id + "] not found"
            );
        }

        productRepository.deleteById(id);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", id.toString());

        publishOutboxEvent("PRODUCT_DELETED", id.toString(), payload);
    }

    // WRITE: evict list cache (and optionally by-id if you later return the created ProductResponse)
    @Transactional
    @CacheEvict(cacheNames = "products_all", allEntries = true)
    public UUID saveNewProduct(NewProductRequest product) {
        UUID id = UUID.randomUUID();

        Product newProduct = new Product(
                id,
                product.name(),
                product.description(),
                product.price(),
                product.imageUrl(),
                product.stockLevel()
        );

        productRepository.save(newProduct);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", id.toString());
        payload.put("name", product.name());
        payload.put("description", product.description());
        payload.put("price", product.price());
        payload.put("imageUrl", product.imageUrl()); // may be null -> OK
        payload.put("stockLevel", product.stockLevel());

        publishOutboxEvent("PRODUCT_CREATED", id.toString(), payload);

        return id;
    }

    // WRITE: evict list cache
    @Transactional
    @CacheEvict(cacheNames = "products_all", allEntries = true)
    public UUID saveNewProductWithImage(String name,
                                        String description,
                                        String price,
                                        String stockLevel,
                                        MultipartFile image) {
        UUID id = UUID.randomUUID();

        // Validate input
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Product description cannot be empty");
        }

        BigDecimal priceValue;
        try {
            priceValue = new BigDecimal(price);
            if (priceValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price format: " + price);
        }

        Integer stockLevelValue;
        try {
            stockLevelValue = Integer.parseInt(stockLevel);
            if (stockLevelValue < 0) {
                throw new IllegalArgumentException("Stock level cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stock level format: " + stockLevel);
        }

        // Create product first
        Product newProduct = new Product(
                id,
                name.trim(),
                description.trim(),
                priceValue,
                null, // imageUrl set after upload (optional)
                stockLevelValue
        );

        productRepository.save(newProduct);

        // Upload image (best effort; do NOT fail product creation)
        boolean imageUploadAttempted = false;
        boolean imageUploadSucceeded = false;

        if (image != null && !image.isEmpty()) {
            imageUploadAttempted = true;
            try {
                productImageService.uploadProductImage(id, image);
                imageUploadSucceeded = true;
            } catch (Exception e) {
                System.err.println("Failed to upload image for product " + id + ": " + e.getMessage());
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", id.toString());
        payload.put("name", name.trim());
        payload.put("description", description.trim());
        payload.put("price", priceValue);
        payload.put("stockLevel", stockLevelValue);
        payload.put("imageUploadAttempted", imageUploadAttempted);
        payload.put("imageUploadSucceeded", imageUploadSucceeded);

        publishOutboxEvent("PRODUCT_CREATED_WITH_IMAGE", id.toString(), payload);

        return id;
    }

    // WRITE: evict by-id and list (if the product changed)
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "products_by_id", key = "#id"),
            @CacheEvict(cacheNames = "products_all", allEntries = true)
    })
    public void updateProduct(UUID id, UpdateProductRequest updateRequest) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound(
                        "product with id [" + id + "] not found"
                ));

        boolean changed = false;

        if (updateRequest.name() != null && !updateRequest.name().equals(product.getName())) {
            product.setName(updateRequest.name());
            changed = true;
        }
        if (updateRequest.description() != null && !updateRequest.description().equals(product.getDescription())) {
            product.setDescription(updateRequest.description());
            changed = true;
        }
        if (updateRequest.price() != null && !updateRequest.price().equals(product.getPrice())) {
            product.setPrice(updateRequest.price());
            changed = true;
        }
        if (updateRequest.imageUrl() != null && !updateRequest.imageUrl().equals(product.getImageUrl())) {
            product.setImageUrl(updateRequest.imageUrl());
            changed = true;
        }
        if (updateRequest.stockLevel() != null && !updateRequest.stockLevel().equals(product.getStockLevel())) {
            product.setStockLevel(updateRequest.stockLevel());
            changed = true;
        }
        if (updateRequest.isPublished() != null && !updateRequest.isPublished().equals(product.getPublished())) {
            product.setPublished(updateRequest.isPublished());
            changed = true;
        }

        productRepository.save(product);

        if (changed) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("productId", id.toString());
            payload.put("name", product.getName());
            payload.put("description", product.getDescription());
            payload.put("price", product.getPrice());
            payload.put("imageUrl", product.getImageUrl()); // may be null -> OK
            payload.put("stockLevel", product.getStockLevel());
            payload.put("published", product.getPublished());

            publishOutboxEvent("PRODUCT_UPDATED", id.toString(), payload);
        }
    }

    Function<Product, ProductResponse> mapToResponse() {
        return p -> new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getImageUrl(),
                p.getStockLevel(),
                p.getPublished(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getDeletedAt()
        );
    }

    private void publishOutboxEvent(String eventType, String aggregateId, Map<String, Object> payloadObj) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadObj);
        } catch (JsonProcessingException e) {
            // Fail fast: don't commit product changes without outbox
            throw new RuntimeException("Failed to serialize outbox payload for eventType=" + eventType, e);
        }

        OutboxEvent evt = new OutboxEvent(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                payloadJson
        );

        outboxEventRepository.save(evt);
    }
}
