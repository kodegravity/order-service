package com.fooddelivery.orderservice.service.impl;

import com.fooddelivery.orderservice.dto.*;
import com.fooddelivery.orderservice.entity.Order;
import com.fooddelivery.orderservice.entity.OrderItem;
import com.fooddelivery.orderservice.entity.OrderStatus;
import com.fooddelivery.orderservice.exception.InvalidOrderException;
import com.fooddelivery.orderservice.exception.ResourceNotFoundException;
import com.fooddelivery.orderservice.repository.OrderRepository;
import com.fooddelivery.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .userId(request.getUserId())
                .restaurantId(request.getRestaurantId())
                .status(OrderStatus.CREATED)
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(itemRequest -> mapToOrderItem(itemRequest, order))
                .collect(Collectors.toList());

        order.setItems(items);

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findOrderById(id);
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = findOrderById(id);
        order.setStatus(request.getStatus());
        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = findOrderById(id);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderException("Cannot cancel an order that has already been delivered");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private OrderItem mapToOrderItem(OrderItemRequest itemRequest, Order order) {
        BigDecimal subtotal = itemRequest.getPrice()
                .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

        return OrderItem.builder()
                .order(order)
                .menuItemId(itemRequest.getMenuItemId())
                .itemName(itemRequest.getItemName())
                .quantity(itemRequest.getQuantity())
                .price(itemRequest.getPrice())
                .subtotal(subtotal)
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .restaurantId(order.getRestaurantId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItemId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}
