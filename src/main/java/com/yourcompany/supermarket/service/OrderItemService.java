package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.OrderItem;
import com.yourcompany.supermarket.mapper.OrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemMapper orderItemMapper;

    public OrderItem create(OrderItem orderItem) {
        orderItemMapper.insert(orderItem);
        return orderItem;
    }

    public List<OrderItem> listByOrder(Long orderId) {
        return orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", orderId));
    }

    public void delete(Long id) {
        orderItemMapper.deleteById(id);
    }
}
