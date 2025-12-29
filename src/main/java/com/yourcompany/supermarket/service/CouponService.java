package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.Coupon;
import com.yourcompany.supermarket.entity.Customer;
import com.yourcompany.supermarket.entity.CustomerCoupon;
import com.yourcompany.supermarket.mapper.CouponMapper;
import com.yourcompany.supermarket.mapper.CustomerCouponMapper;
import com.yourcompany.supermarket.mapper.CustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CouponService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerCouponMapper customerCouponMapper;

    public Coupon createTemplate(Coupon coupon) {
        if (coupon.getStatus() == null) {
            coupon.setStatus("ACTIVE");
        }
        couponMapper.insert(coupon);
        return coupon;
    }

    public Coupon update(Coupon coupon) {
        couponMapper.updateById(coupon);
        return coupon;
    }

    public Coupon get(Long id) {
        return couponMapper.selectById(id);
    }

    public List<Coupon> listAll() {
        return couponMapper.selectList(new QueryWrapper<>());
    }

    public void delete(Long id) {
        couponMapper.deleteById(id);
    }

    public CustomerCoupon assignToCustomer(Long customerId, Long couponId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("会员不存在");
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (!isValid(coupon)) {
            throw new IllegalStateException("优惠券不可领取或已过期");
        }
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setCouponId(couponId);
        customerCoupon.setStatus("AVAILABLE");
        customerCoupon.setAssignedAt(LocalDateTime.now());
        customerCouponMapper.insert(customerCoupon);
        return customerCoupon;
    }

    public List<CustomerCoupon> listCustomerCoupons(Long customerId, String status) {
        List<CustomerCoupon> customerCoupons = customerCouponMapper.selectList(new QueryWrapper<CustomerCoupon>()
                .eq("customer_id", customerId));

        Map<Long, Coupon> couponMap = customerCoupons.stream()
                .map(CustomerCoupon::getCouponId)
                .distinct()
                .map(couponMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Coupon::getId, c -> c));

        LocalDateTime now = LocalDateTime.now();
        return customerCoupons.stream().filter(cc -> {
            Coupon coupon = couponMap.get(cc.getCouponId());
            boolean expired = coupon != null && coupon.getEndTime() != null && now.isAfter(coupon.getEndTime());
            if ("EXPIRED".equalsIgnoreCase(status)) {
                return expired || (coupon != null && !"ACTIVE".equalsIgnoreCase(coupon.getStatus()));
            }
            if ("USED".equalsIgnoreCase(status)) {
                return "USED".equalsIgnoreCase(cc.getStatus());
            }
            if ("AVAILABLE".equalsIgnoreCase(status)) {
                return "AVAILABLE".equalsIgnoreCase(cc.getStatus()) && !expired && isValid(coupon);
            }
            return true;
        }).collect(Collectors.toList());
    }

    public boolean isValid(Coupon coupon) {
        if (coupon == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return "ACTIVE".equalsIgnoreCase(coupon.getStatus())
                && (coupon.getStartTime() == null || !now.isBefore(coupon.getStartTime()))
                && (coupon.getEndTime() == null || !now.isAfter(coupon.getEndTime()));
    }

    public CustomerCoupon redeem(Long customerCouponId, BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额必须大于 0");
        }
        CustomerCoupon customerCoupon = customerCouponMapper.selectById(customerCouponId);
        if (customerCoupon == null) {
            throw new IllegalArgumentException("持券记录不存在");
        }
        Coupon coupon = couponMapper.selectById(customerCoupon.getCouponId());
        validateCouponUsable(customerCoupon, coupon, orderAmount);
        customerCoupon.setStatus("USED");
        customerCouponMapper.updateById(customerCoupon);
        return customerCoupon;
    }

    private void validateCouponUsable(CustomerCoupon customerCoupon, Coupon coupon, BigDecimal orderAmount) {
        if (!"AVAILABLE".equalsIgnoreCase(customerCoupon.getStatus())) {
            throw new IllegalStateException("优惠券不可用或已使用");
        }
        if (!isValid(coupon)) {
            throw new IllegalStateException("优惠券已过期或未激活");
        }
        if (coupon.getThresholdAmount() != null && orderAmount.compareTo(coupon.getThresholdAmount()) < 0) {
            throw new IllegalStateException("未达到优惠券使用门槛");
        }
    }
}
