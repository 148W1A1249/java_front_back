package com.zestindia.productapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestindia.productapi.dto.CartItemRequest;
import com.zestindia.productapi.dto.CheckoutRequest;
import com.zestindia.productapi.dto.ProductRequest;
import com.zestindia.productapi.entity.Role;
import com.zestindia.productapi.entity.User;
import com.zestindia.productapi.repository.ProductRepository;
import com.zestindia.productapi.repository.UserRepository;
import com.zestindia.productapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        User user = userRepository.findByUsername("testuser").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("testuser")
                        .email("testuser@example.com")
                        .password(passwordEncoder.encode("password"))
                        .role(Role.ROLE_USER)
                        .build()));

        User admin = userRepository.findByUsername("testadmin").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("testadmin")
                        .email("testadmin@example.com")
                        .password(passwordEncoder.encode("password"))
                        .role(Role.ROLE_ADMIN)
                        .build()));

        userToken = jwtService.generateAccessToken(user);
        adminToken = jwtService.generateAccessToken(admin);
    }

    @Test
    void listProducts_isPublic() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    void createProduct_requiresAdmin() throws Exception {
        ProductRequest request = sampleProduct("Sneakers");

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Sneakers")));
    }

    @Test
    void checkoutFlow_asUser() throws Exception {
        ProductRequest request = sampleProduct("Mug");
        String created = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer productId = objectMapper.readTree(created).get("id").asInt();

        CartItemRequest cartItem = new CartItemRequest();
        cartItem.setProductId(productId);
        cartItem.setQuantity(2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItems", is(2)));

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setShippingName("Test User");
        checkout.setShippingAddress("12 Sample Street");

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkout)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    private ProductRequest sampleProduct(String name) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription("Test product");
        request.setPrice(new BigDecimal("999.00"));
        request.setStock(20);
        request.setImageUrl("https://example.com/p.jpg");
        request.setActive(true);
        return request;
    }
}
