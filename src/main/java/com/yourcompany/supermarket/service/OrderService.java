package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.CartItem;
import com.yourcompany.supermarket.entity.Order;
import com.yourcompany.supermarket.entity.OrderItem;
import com.yourcompany.supermarket.entity.Product;
import com.yourcompany.supermarket.mapper.CartItemMapper;
import com.yourcompany.supermarket.mapper.OrderItemMapper;
import com.yourcompany.supermarket.mapper.OrderMapper;
import com.yourcompany.supermarket.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private ProductMapper productMapper;

    public Order get(Long id) {
        return orderMapper.selectById(id);
    }

    public List<Order> listAll() {
        return orderMapper.selectList(new QueryWrapper<>());
    }

    public void delete(Long id) {
        orderMapper.deleteById(id);
    }

    @Transactional
    public Order createOrderFromCart(Long customerId) {
        List<CartItem> cartItems = cartItemMapper.selectList(new QueryWrapper<CartItem>().eq("customer_id", customerId));
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("购物车为空，无法创建订单");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("商品不存在: " + cartItem.getProductId());
            }
            if (product.getStock() == null || product.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException("库存不足: " + product.getName());
            }
            if (product.getPrice() == null) {
                throw new IllegalStateException("商品价格缺失: " + product.getName());
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setTotalAmount(total);
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            product.setStock(product.getStock() - cartItem.getQuantity());
            productMapper.updateById(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItemMapper.insert(orderItem);
        }

        cartItemMapper.delete(new QueryWrapper<CartItem>().eq("customer_id", customerId));
        return order;
    }
}
