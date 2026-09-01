package com.zestindia.productapi.repository;

import com.zestindia.productapi.entity.Order;
import com.zestindia.productapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedOnDesc(User user);
    List<Order> findAllByOrderByCreatedOnDesc();
}
