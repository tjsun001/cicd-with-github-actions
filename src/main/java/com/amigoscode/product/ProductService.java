package com.amigoscode.product;

import com.amigoscode.exception.ResourceNotFound;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(mapToResponse())
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(UUID id) {
        return productRepository.findById(id)
                .map(mapToResponse())
                .orElseThrow(() -> new ResourceNotFound(
                        "product with id [" + id + "] not found"
                ));
    }

    public void deleteProductById(UUID id) {
        boolean exists = productRepository.existsById(id);
        if (!exists) {
            throw new ResourceNotFound(
                    "product with id [" + id + "] not found"
            );
        }
        productRepository.deleteById(id);
    }

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
        return id;
    }

    Function<Product, ProductResponse> mapToResponse() {
        return p -> new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getImageUrl(),
                p.getStockLevel(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getDeletedAt()
        );
    }


    public void updateProduct(UUID id,
                              UpdateProductRequest updateRequest) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound(
                        "product with id [" + id + "] not found"
                ));

        if (updateRequest.name() != null && !updateRequest.name().equals(product.getName())) {
            product.setName(updateRequest.name());
        }
        if (updateRequest.description() != null && !updateRequest.description().equals(product.getDescription())) {
            product.setDescription(updateRequest.description());
        }
        if (updateRequest.price() != null && !updateRequest.price().equals(product.getPrice())) {
            product.setPrice(updateRequest.price());
        }
        if (updateRequest.imageUrl() != null && !updateRequest.imageUrl().equals(product.getImageUrl())) {
            product.setImageUrl(updateRequest.imageUrl());
        }
        if (updateRequest.stockLevel() != null && !updateRequest.stockLevel().equals(product.getStockLevel())) {
            product.setStockLevel(updateRequest.stockLevel());
        }

        if (updateRequest.isPublished() != null && !updateRequest.isPublished().equals(product.getPublished())) {
            product.setPublished(updateRequest.isPublished());
        }

        productRepository.save(product);
    }
}
