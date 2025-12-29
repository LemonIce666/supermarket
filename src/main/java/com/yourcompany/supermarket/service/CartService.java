package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.CartItem;
import com.yourcompany.supermarket.entity.Product;
import com.yourcompany.supermarket.mapper.CartItemMapper;
import com.yourcompany.supermarket.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Transactional
    public CartItem addItem(Long customerId, Long productId, int quantity) {
        validateQuantity(quantity);
        Product product = findProduct(productId);
        validateStock(product, quantity);

        CartItem existing = cartItemMapper.selectOne(new QueryWrapper<CartItem>()
                .eq("customer_id", customerId)
                .eq("product_id", productId));

        if (existing == null) {
            CartItem cartItem = new CartItem();
            cartItem.setCustomerId(customerId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItemMapper.insert(cartItem);
            return cartItem;
        }

        int newQty = existing.getQuantity() + quantity;
        validateStock(product, newQty);
        existing.setQuantity(newQty);
        cartItemMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public CartItem updateQuantity(Long customerId, Long productId, int quantity) {
        validateQuantity(quantity);
        Product product = findProduct(productId);
        validateStock(product, quantity);

        CartItem existing = cartItemMapper.selectOne(new QueryWrapper<CartItem>()
                .eq("customer_id", customerId)
                .eq("product_id", productId));

        if (existing == null) {
            throw new IllegalArgumentException("购物车中不存在该商品");
        }
        existing.setQuantity(quantity);
        cartItemMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void removeItem(Long customerId, Long productId) {
        cartItemMapper.delete(new QueryWrapper<CartItem>()
                .eq("customer_id", customerId)
                .eq("product_id", productId));
    }

    @Transactional
    public void clearCart(Long customerId) {
        cartItemMapper.delete(new QueryWrapper<CartItem>().eq("customer_id", customerId));
    }

    public List<CartLine> listCart(Long customerId) {
        List<CartItem> items = cartItemMapper.selectList(new QueryWrapper<CartItem>().eq("customer_id", customerId));
        return items.stream().map(this::buildLine).collect(Collectors.toList());
    }

    public BigDecimal calculateSubtotal(CartItem item) {
        Product product = findProduct(item.getProductId());
        if (product.getPrice() == null) {
            throw new IllegalStateException("商品价格未设置");
        }
        return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    public BigDecimal calculateTotal(Long customerId) {
        return listCart(customerId).stream()
                .map(CartLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartLine buildLine(CartItem item) {
        Product product = findProduct(item.getProductId());
        BigDecimal subtotal = Optional.ofNullable(product.getPrice())
                .orElseThrow(() -> new IllegalStateException("商品价格未设置"))
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartLine(item, product, subtotal);
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于 0");
        }
    }

    private void validateStock(Product product, int quantity) {
        if (product.getStock() != null && product.getStock() < quantity) {
            throw new IllegalStateException("库存不足");
        }
    }

    private Product findProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        return product;
    }

    public static class CartLine {
        private final CartItem cartItem;
        private final Product product;
        private final BigDecimal subtotal;

        public CartLine(CartItem cartItem, Product product, BigDecimal subtotal) {
            this.cartItem = cartItem;
            this.product = product;
            this.subtotal = subtotal;
        }

        public CartItem getCartItem() {
            return cartItem;
        }

        public Product getProduct() {
            return product;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }
}
