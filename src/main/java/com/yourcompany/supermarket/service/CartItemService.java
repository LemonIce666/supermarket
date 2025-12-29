package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.CartItem;
import com.yourcompany.supermarket.entity.Product;
import com.yourcompany.supermarket.mapper.CartItemMapper;
import com.yourcompany.supermarket.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartItemService {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private ProductMapper productMapper;

    public CartItem addItem(CartItem cartItem) {
        validateCartItem(cartItem);
        cartItemMapper.insert(cartItem);
        return cartItem;
    }

    public List<CartItem> listByCustomer(Long customerId) {
        return cartItemMapper.selectList(new QueryWrapper<CartItem>().eq("customer_id", customerId));
    }

    public void removeItem(Long id) {
        cartItemMapper.deleteById(id);
    }

    public void clearCart(Long customerId) {
        cartItemMapper.delete(new QueryWrapper<CartItem>().eq("customer_id", customerId));
    }

    private void validateCartItem(CartItem cartItem) {
        if (cartItem.getCustomerId() == null || cartItem.getProductId() == null) {
            throw new IllegalArgumentException("缺少顾客或商品信息");
        }
        if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("数量必须大于 0");
        }
        Product product = productMapper.selectById(cartItem.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (product.getStock() != null && product.getStock() < cartItem.getQuantity()) {
            throw new IllegalStateException("库存不足，无法加入购物车");
        }
    }
}
