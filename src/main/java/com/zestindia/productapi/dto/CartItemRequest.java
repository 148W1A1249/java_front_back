package com.zestindia.productapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {

    @NotNull
    private Integer productId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
