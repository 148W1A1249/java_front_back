package com.zestindia.productapi.config;

import com.zestindia.productapi.entity.Product;
import com.zestindia.productapi.entity.Role;
import com.zestindia.productapi.entity.User;
import com.zestindia.productapi.repository.ProductRepository;
import com.zestindia.productapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .email("admin@novamart.shop")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ROLE_ADMIN)
                    .build());
            log.info("Default admin created (admin / admin123)");
        }

        if (productRepository.count() == 0) {
            List<Product> samples = List.of(
                    product("Aurora Desk Lamp", "Warm LED desk lamp with touch dimmer and oak base.", "2499.00", 40,
                            "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&q=80"),
                    product("Nordic Ceramic Mug", "Hand-glazed 350ml mug. Dishwasher safe.", "599.00", 120,
                            "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=600&q=80"),
                    product("Trail Runner Sneakers", "Lightweight everyday runners with breathable mesh.", "4499.00", 55,
                            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&q=80"),
                    product("Canvas Weekender", "Water-resistant canvas bag with leather straps.", "3299.00", 30,
                            "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&q=80"),
                    product("Studio Headphones", "Over-ear wireless headphones with 30h battery.", "7999.00", 25,
                            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&q=80"),
                    product("Minimal Wall Clock", "Silent quartz movement, brushed steel face.", "1899.00", 70,
                            "https://images.unsplash.com/photo-1563861826100-9cb868fdbe1c?w=600&q=80")
            );
            productRepository.saveAll(samples);
            log.info("Seeded {} NovaMart sample products", samples.size());
        }
    }

    private Product product(String name, String description, String price, int stock, String imageUrl) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .stock(stock)
                .imageUrl(imageUrl)
                .active(true)
                .build();
    }
}
