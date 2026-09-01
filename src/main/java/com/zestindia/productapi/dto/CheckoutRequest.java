package com.zestindia.productapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank
    @Size(max = 150)
    private String shippingName;

    @NotBlank
    @Size(max = 500)
    private String shippingAddress;
}
