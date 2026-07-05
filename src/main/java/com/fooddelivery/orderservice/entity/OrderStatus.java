package com.fooddelivery.orderservice.entity;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    CONFIRMED,
    CANCELLED,
    DELIVERED
}
