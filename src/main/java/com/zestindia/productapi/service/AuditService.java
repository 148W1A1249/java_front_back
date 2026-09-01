package com.zestindia.productapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditService {

    @Async
    public void logProductCreated(Integer productId, String username) {
        log.info("Async audit: product {} created by {}", productId, username);
    }
}
