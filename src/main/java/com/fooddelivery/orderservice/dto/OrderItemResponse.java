package com.fooddelivery.orderservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {

    private Long id;
    private Long menuItemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
}
