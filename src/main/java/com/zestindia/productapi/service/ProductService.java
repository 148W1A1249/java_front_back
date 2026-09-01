package com.zestindia.productapi.service;

import com.zestindia.productapi.dto.PageResponse;
import com.zestindia.productapi.dto.ProductRequest;
import com.zestindia.productapi.dto.ProductResponse;
import com.zestindia.productapi.entity.Product;
import com.zestindia.productapi.exception.BadRequestException;
import com.zestindia.productapi.exception.ResourceNotFoundException;
import com.zestindia.productapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getActiveProducts(Pageable pageable) {
        return toPage(productRepository.findByActiveTrue(pageable));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        return toPage(productRepository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {
        return toResponse(findProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() == null || request.getActive())
                .build();
        Product saved = productRepository.save(product);
        auditService.logProductCreated(saved.getId(), "admin");
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = findProduct(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Integer id) {
        Product product = findProduct(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public Product findProduct(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public void decreaseStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    private PageResponse<ProductResponse> toPage(Page<Product> page) {
        return PageResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .createdOn(product.getCreatedOn())
                .modifiedOn(product.getModifiedOn())
                .build();
    }
}
