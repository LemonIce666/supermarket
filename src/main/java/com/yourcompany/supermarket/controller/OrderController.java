package com.yourcompany.supermarket.controller;

import com.yourcompany.supermarket.entity.Order;
import com.yourcompany.supermarket.entity.OrderItem;
import com.yourcompany.supermarket.service.CheckoutService;
import com.yourcompany.supermarket.service.OrderItemService;
import com.yourcompany.supermarket.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @PostMapping("/checkout")
    public OrderDetail createOrder(@RequestBody CheckoutRequest request) {
        if (request == null || request.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId is required");
        }
        Order order = checkoutService.checkout(request.getCustomerId(), request.getCustomerCouponId(), request.getPayMethod());
        List<OrderItem> items = orderItemService.listByOrder(order.getId());
        return new OrderDetail(order, items);
    }

    @GetMapping
    public List<Order> listOrders(@RequestParam(value = "customerId", required = false) Long customerId) {
        if (customerId != null) {
            return orderService.listByCustomer(customerId);
        }
        return orderService.listAll();
    }

    @GetMapping("/{id}")
    public OrderDetail getOrder(@PathVariable("id") Long id) {
        Order order = orderService.get(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        List<OrderItem> items = orderItemService.listByOrder(id);
        return new OrderDetail(order, items);
    }

    @PostMapping("/{id}/refund")
    public Order refund(@PathVariable("id") Long id, @RequestBody(required = false) RefundRequest request) {
        return checkoutService.refund(id);
    }

    public record OrderDetail(Order order, List<OrderItem> items) {
    }
}
