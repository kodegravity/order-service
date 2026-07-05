package com.fooddelivery.orderservice.service;

import com.fooddelivery.orderservice.dto.CreateOrderRequest;
import com.fooddelivery.orderservice.dto.OrderResponse;
import com.fooddelivery.orderservice.dto.UpdateOrderStatusRequest;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getOrdersByUserId(Long userId);

    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request);

    void cancelOrder(Long id);
}
