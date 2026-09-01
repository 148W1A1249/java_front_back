package com.zestindia.productapi.service;

import com.zestindia.productapi.dto.*;
import com.zestindia.productapi.entity.Cart;
import com.zestindia.productapi.entity.CartItem;
import com.zestindia.productapi.entity.Product;
import com.zestindia.productapi.entity.User;
import com.zestindia.productapi.exception.BadRequestException;
import com.zestindia.productapi.exception.ResourceNotFoundException;
import com.zestindia.productapi.repository.CartItemRepository;
import com.zestindia.productapi.repository.CartRepository;
import com.zestindia.productapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public CartResponse getCart(String username) {
        return toResponse(getOrCreateCart(username));
    }

    @Transactional
    public CartResponse addItem(String username, CartItemRequest request) {
        Cart cart = getOrCreateCart(username);
        Product product = productService.findProduct(request.getProductId());

        if (!product.isActive()) {
            throw new BadRequestException("Product is not available");
        }
        if (product.getStock() < request.getQuantity()) {
            throw new BadRequestException("Not enough stock available");
        }

        CartItem existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + request.getQuantity();
            if (product.getStock() < newQty) {
                throw new BadRequestException("Not enough stock available");
            }
            existing.setQuantity(newQty);
            cartItemRepository.save(existing);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public CartResponse updateItem(String username, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(username);
        CartItem item = findCartItem(cart, itemId);

        if (item.getProduct().getStock() < request.getQuantity()) {
            throw new BadRequestException("Not enough stock available");
        }
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String username, Long itemId) {
        Cart cart = getOrCreateCart(username);
        CartItem item = findCartItem(cart, itemId);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    @Transactional
    public Cart getCartEntity(String username) {
        return getOrCreateCart(username);
    }

    @Transactional
    public void clearCart(Cart cart) {
        cart.clearItems();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return cartRepository.findByUser(user).orElseGet(() ->
                cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build()));
    }

    private CartItem findCartItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(this::toItemResponse).toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = items.stream().mapToInt(CartItemResponse::getQuantity).sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .totalAmount(total)
                .totalItems(count)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .unitPrice(product.getPrice())
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .stockAvailable(product.getStock())
                .build();
    }
}
