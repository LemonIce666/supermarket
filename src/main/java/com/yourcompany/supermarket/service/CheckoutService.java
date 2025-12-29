package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.*;
import com.yourcompany.supermarket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    @Autowired
    private CartService cartService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerTransactionMapper customerTransactionMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;

    @Autowired
    private CustomerCouponMapper customerCouponMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Transactional
    public Order checkout(Long customerId, Long customerCouponId, String payMethod) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("会员不存在");
        }
        if (customer.getWalletBalance() == null) {
            customer.setWalletBalance(BigDecimal.ZERO);
        }
        if (customer.getTotalSpent() == null) {
            customer.setTotalSpent(BigDecimal.ZERO);
        }

        List<CartService.CartLine> cartLines = cartService.listCart(customerId);
        if (cartLines.isEmpty()) {
            throw new IllegalStateException("购物车为空，无法结算");
        }

        // Step 1: 校验库存
        cartLines.forEach(line -> {
            Product product = line.getProduct();
            if (product.getStock() == null || product.getStock() < line.getCartItem().getQuantity()) {
                throw new IllegalStateException("库存不足: " + product.getName());
            }
            if (product.getPrice() == null) {
                throw new IllegalStateException("商品价格缺失: " + product.getName());
            }
        });

        BigDecimal total = cartLines.stream()
                .map(CartService.CartLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 2: 计算优惠
        AppliedCoupon appliedCoupon = resolveCoupon(customerId, customerCouponId, total);
        BigDecimal payable = total.subtract(appliedCoupon.discount()).max(BigDecimal.ZERO);

        // Step 3: 扣减钱包或记录现金支付
        String finalStatus = applyPayment(customer, payable, payMethod);

        // Step 4: 写入 Order/OrderItem
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setTotalAmount(payable);
        order.setStatus(finalStatus);
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        for (CartService.CartLine line : cartLines) {
            Product product = line.getProduct();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(line.getCartItem().getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItemMapper.insert(orderItem);
        }

        // Step 5: 库存出库流水
        for (CartService.CartLine line : cartLines) {
            Product product = line.getProduct();
            product.setStock(product.getStock() - line.getCartItem().getQuantity());
            productMapper.updateById(product);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProductId(product.getId());
            transaction.setQuantity(line.getCartItem().getQuantity());
            transaction.setType("OUT");
            transaction.setTransactionTime(LocalDateTime.now());
            inventoryTransactionMapper.insert(transaction);
        }

        // Step 6: 更新顾客累计消费
        customer.setTotalSpent(customer.getTotalSpent().add(payable));
        customerMapper.updateById(customer);

        // 标记优惠券使用
        if (appliedCoupon.customerCoupon() != null) {
            appliedCoupon.customerCoupon().setStatus("USED");
            customerCouponMapper.updateById(appliedCoupon.customerCoupon());
        }

        cartService.clearCart(customerId);
        return order;
    }

    @Transactional
    public Order refund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if ("REFUNDED".equalsIgnoreCase(order.getStatus())) {
            return order;
        }

        List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", orderId));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                continue;
            }
            product.setStock(product.getStock() + item.getQuantity());
            productMapper.updateById(product);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProductId(product.getId());
            transaction.setQuantity(item.getQuantity());
            transaction.setType("IN");
            transaction.setTransactionTime(LocalDateTime.now());
            inventoryTransactionMapper.insert(transaction);
        }

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer != null) {
            if (customer.getWalletBalance() == null) {
                customer.setWalletBalance(BigDecimal.ZERO);
            }
            if (customer.getTotalSpent() == null) {
                customer.setTotalSpent(BigDecimal.ZERO);
            }
            customer.setTotalSpent(customer.getTotalSpent().subtract(order.getTotalAmount()));
            if (customer.getTotalSpent().compareTo(BigDecimal.ZERO) < 0) {
                customer.setTotalSpent(BigDecimal.ZERO);
            }
            if (order.getStatus() != null && order.getStatus().toUpperCase().contains("WALLET")) {
                customer.setWalletBalance(customer.getWalletBalance().add(order.getTotalAmount()));
                CustomerTransaction refundTxn = new CustomerTransaction();
                refundTxn.setCustomerId(customer.getId());
                refundTxn.setAmount(order.getTotalAmount());
                refundTxn.setType("RECHARGE");
                refundTxn.setDescription("订单退款到账户钱包");
                refundTxn.setCreatedAt(LocalDateTime.now());
                customerTransactionMapper.insert(refundTxn);
            } else {
                CustomerTransaction refundTxn = new CustomerTransaction();
                refundTxn.setCustomerId(customer.getId());
                refundTxn.setAmount(order.getTotalAmount());
                refundTxn.setType("SPEND");
                refundTxn.setDescription("订单现金退款记录");
                refundTxn.setCreatedAt(LocalDateTime.now());
                customerTransactionMapper.insert(refundTxn);
            }
            customerMapper.updateById(customer);
        }

        order.setStatus("REFUNDED");
        orderMapper.updateById(order);
        return order;
    }

    private AppliedCoupon resolveCoupon(Long customerId, Long customerCouponId, BigDecimal total) {
        if (customerCouponId != null) {
            CustomerCoupon customerCoupon = customerCouponMapper.selectById(customerCouponId);
            if (customerCoupon == null || !Objects.equals(customerCoupon.getCustomerId(), customerId)) {
                throw new IllegalArgumentException("优惠券不存在或不属于当前顾客");
            }
            Coupon coupon = couponMapper.selectById(customerCoupon.getCouponId());
            validateCoupon(customerCoupon, coupon, total);
            BigDecimal discount = coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
            discount = discount.min(total);
            return new AppliedCoupon(customerCoupon, coupon, discount);
        }

        List<CustomerCoupon> available = customerCouponMapper.selectList(new QueryWrapper<CustomerCoupon>()
                .eq("customer_id", customerId)
                .eq("status", "AVAILABLE"));

        List<AppliedCoupon> candidates = available.stream().map(cc -> {
            Coupon coupon = couponMapper.selectById(cc.getCouponId());
            if (!isCouponUsable(cc, coupon, total)) {
                return null;
            }
            BigDecimal discount = coupon.getFaceValue() == null ? BigDecimal.ZERO : coupon.getFaceValue();
            discount = discount.min(total);
            return new AppliedCoupon(cc, coupon, discount);
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return candidates.stream()
                .max(Comparator.comparing(AppliedCoupon::discount))
                .orElse(new AppliedCoupon(null, null, BigDecimal.ZERO));
    }

    private void validateCoupon(CustomerCoupon customerCoupon, Coupon coupon, BigDecimal total) {
        if (!isCouponUsable(customerCoupon, coupon, total)) {
            throw new IllegalStateException("优惠券不可用");
        }
    }

    private boolean isCouponUsable(CustomerCoupon customerCoupon, Coupon coupon, BigDecimal total) {
        if (customerCoupon == null || coupon == null) {
            return false;
        }
        if (!"AVAILABLE".equalsIgnoreCase(customerCoupon.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            return false;
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(coupon.getStatus())) {
            return false;
        }
        return coupon.getThresholdAmount() == null || total.compareTo(coupon.getThresholdAmount()) >= 0;
    }

    private String applyPayment(Customer customer, BigDecimal payable, String payMethod) {
        String method = payMethod == null ? "WALLET" : payMethod.toUpperCase();
        if ("CASH".equals(method)) {
            recordCashPayment(customer, payable);
            return "PAID_CASH";
        }
        if (customer.getWalletBalance().compareTo(payable) < 0) {
            throw new IllegalStateException("钱包余额不足");
        }
        customer.setWalletBalance(customer.getWalletBalance().subtract(payable));
        customerMapper.updateById(customer);

        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setCustomerId(customer.getId());
        transaction.setAmount(payable.negate());
        transaction.setType("SPEND");
        transaction.setDescription("钱包支付订单");
        transaction.setCreatedAt(LocalDateTime.now());
        customerTransactionMapper.insert(transaction);
        return "PAID_WALLET";
    }

    private void recordCashPayment(Customer customer, BigDecimal payable) {
        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setCustomerId(customer.getId());
        transaction.setAmount(payable);
        transaction.setType("SPEND");
        transaction.setDescription("现金支付记录");
        transaction.setCreatedAt(LocalDateTime.now());
        customerTransactionMapper.insert(transaction);
    }

    private record AppliedCoupon(CustomerCoupon customerCoupon, Coupon coupon, BigDecimal discount) {
    }
}
