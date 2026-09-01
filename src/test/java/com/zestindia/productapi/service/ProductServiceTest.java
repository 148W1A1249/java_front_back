package com.zestindia.productapi.service;

import com.zestindia.productapi.dto.ProductRequest;
import com.zestindia.productapi.dto.ProductResponse;
import com.zestindia.productapi.entity.Product;
import com.zestindia.productapi.exception.ResourceNotFoundException;
import com.zestindia.productapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductService productService;

    @Test
    void create_shouldSaveAndReturnProduct() {
        ProductRequest request = new ProductRequest();
        request.setName("Lamp");
        request.setDescription("Desk lamp");
        request.setPrice(new BigDecimal("2499.00"));
        request.setStock(10);

        Product saved = Product.builder()
                .id(1)
                .name("Lamp")
                .description("Desk lamp")
                .price(new BigDecimal("2499.00"))
                .stock(10)
                .active(true)
                .createdOn(Instant.now())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse response = productService.create(request);

        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo("Lamp");
        assertThat(response.getPrice()).isEqualByComparingTo("2499.00");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getById_whenMissing_shouldThrow() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getActiveProducts_shouldReturnPagedResponse() {
        Product product = Product.builder()
                .id(1)
                .name("Mug")
                .price(new BigDecimal("599"))
                .stock(5)
                .active(true)
                .createdOn(Instant.now())
                .build();
        Page<Product> page = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findByActiveTrue(any(PageRequest.class))).thenReturn(page);

        var result = productService.getActiveProducts(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void delete_shouldSoftDeactivate() {
        Product product = Product.builder()
                .id(1)
                .name("Bag")
                .price(BigDecimal.TEN)
                .stock(1)
                .active(true)
                .build();
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.delete(1);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }
}
