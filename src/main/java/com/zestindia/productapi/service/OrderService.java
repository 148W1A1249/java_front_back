package com.zestindia.productapi.service;

import com.zestindia.productapi.dto.CheckoutRequest;
import com.zestindia.productapi.dto.OrderItemResponse;
import com.zestindia.productapi.dto.OrderResponse;
import com.zestindia.productapi.entity.*;
import com.zestindia.productapi.exception.BadRequestException;
import com.zestindia.productapi.exception.ResourceNotFoundException;
import com.zestindia.productapi.repository.OrderRepository;
import com.zestindia.productapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductService productService;

    @Transactional
    public OrderResponse checkout(String username, CheckoutRequest request) {
        Cart cart = cartService.getCartEntity(username);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.CONFIRMED)
                .shippingName(request.getShippingName())
                .shippingAddress(request.getShippingAddress())
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : List.copyOf(cart.getItems())) {
            Product product = cartItem.getProduct();
            productService.decreaseStock(product, cartItem.getQuantity());

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .lineTotal(lineTotal)
                    .build();
            order.getItems().add(orderItem);
            total = total.add(lineTotal);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        cartService.clearCart(cart);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUserOrderByCreatedOnDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedOnDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id, String username, boolean admin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (!admin && !order.getUser().getUsername().equals(username)) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingName(order.getShippingName())
                .shippingAddress(order.getShippingAddress())
                .createdOn(order.getCreatedOn())
                .username(order.getUser().getUsername())
                .items(order.getItems().stream().map(i -> OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build()).toList())
                .build();
    }
}
