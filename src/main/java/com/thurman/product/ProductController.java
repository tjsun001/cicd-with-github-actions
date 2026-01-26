package com.thurman.product;

import com.thurman.storage.S3StorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    // CloudFront-friendly caching:
    // - cache at shared/CDN layer for 60s
    // - don't encourage browser caching
    private static final String CACHE_JSON = "public, s-maxage=60, max-age=0";

    // Images can be cached longer at CDN and browser
    private static final CacheControl CACHE_IMAGE =
            CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic();

    // Writes should never be cached
    private static final CacheControl NO_STORE =
            CacheControl.noStore();

    private final ProductService productService;
    private final ProductImageService productImageService;

    public ProductController(ProductService productService, ProductImageService productImageService) {
        this.productService = productService;
        this.productImageService = productImageService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_JSON)
                .body(productService.getAllProducts());
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_JSON)
                .body(productService.getProductById(id));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteProductById(@PathVariable("id") UUID id) {
        productService.deleteProductById(id);
        return ResponseEntity.noContent()
                .cacheControl(NO_STORE)
                .build();
    }

    @PostMapping
    public ResponseEntity<UUID> saveProduct(@RequestBody @Valid NewProductRequest product) {
        UUID id = productService.saveNewProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(NO_STORE)
                .body(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UUID> saveProductWithImage(@RequestParam("name") @NotBlank String name,
                                                     @RequestParam("description") @NotBlank String description,
                                                     @RequestParam("price") @NotBlank String price,
                                                     @RequestParam("stockLevel") @NotBlank String stockLevel,
                                                     @RequestParam(value = "image", required = false) MultipartFile image) {
        UUID id = productService.saveNewProductWithImage(name, description, price, stockLevel, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(NO_STORE)
                .body(id);
    }

    @PutMapping("{id}")
    public ResponseEntity<Void> updateProduct(@PathVariable UUID id,
                                              @RequestBody @Valid UpdateProductRequest request) {
        productService.updateProduct(id, request);
        return ResponseEntity.noContent()
                .cacheControl(NO_STORE)
                .build();
    }

    @PostMapping("{id}/image")
    public ResponseEntity<Void> uploadProductImage(@PathVariable UUID id,
                                                   @RequestParam("file") MultipartFile file) {
        productImageService.uploadProductImage(id, file);
        return ResponseEntity.ok()
                .cacheControl(NO_STORE)
                .build();
    }

    @GetMapping("{id}/image")
    public ResponseEntity<byte[]> downloadProductImage(@PathVariable UUID id,
                                                       @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        S3StorageService.StoredObject storedObject = productImageService.downloadProductImage(id);

        // Simple, deterministic ETag based on bytes (fine for demo; for huge files, you'd use S3 ETag/metadata)
        String etag = "\"" + sha256Base64(storedObject.bytes()) + "\"";

        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CACHE_IMAGE)
                    .build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(storedObject.contentType()));
        headers.setContentLength(storedObject.bytes().length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"product-image\"");
        headers.setCacheControl(CACHE_IMAGE.getHeaderValue());
        headers.setETag(etag);

        return ResponseEntity.ok()
                .headers(headers)
                .body(storedObject.bytes());
    }

    private static String sha256Base64(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            // Worst case: fall back to weak but stable-ish value for demo
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(("len:" + bytes.length).getBytes(StandardCharsets.UTF_8));
        }
    }
}
