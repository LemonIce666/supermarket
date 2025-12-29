package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.Coupon;
import com.yourcompany.supermarket.entity.CustomerCoupon;
import com.yourcompany.supermarket.mapper.CouponMapper;
import com.yourcompany.supermarket.mapper.CustomerCouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerCouponService {

    @Autowired
    private CustomerCouponMapper customerCouponMapper;

    @Autowired
    private CouponMapper couponMapper;

    public CustomerCoupon assignToCustomer(Long customerId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException("优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new IllegalStateException("优惠券尚未生效");
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            throw new IllegalStateException("优惠券已过期");
        }
        if (!"ACTIVE".equalsIgnoreCase(coupon.getStatus())) {
            throw new IllegalStateException("优惠券不可领取");
        }

        CustomerCoupon cc = new CustomerCoupon();
        cc.setCustomerId(customerId);
        cc.setCouponId(couponId);
        cc.setStatus("AVAILABLE");
        cc.setAssignedAt(LocalDateTime.now());
        customerCouponMapper.insert(cc);
        return cc;
    }

    public CustomerCoupon get(Long id) {
        return customerCouponMapper.selectById(id);
    }

    public List<CustomerCoupon> listByCustomer(Long customerId) {
        return customerCouponMapper.selectList(new QueryWrapper<CustomerCoupon>().eq("customer_id", customerId));
    }

    public void delete(Long id) {
        customerCouponMapper.deleteById(id);
    }

    public void useCustomerCoupon(Long customerCouponId) {
        CustomerCoupon customerCoupon = customerCouponMapper.selectById(customerCouponId);
        if (customerCoupon == null) {
            throw new IllegalArgumentException("持券记录不存在");
        }
        Coupon coupon = couponMapper.selectById(customerCoupon.getCouponId());
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null) {
            throw new IllegalStateException("优惠券不存在");
        }
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new IllegalStateException("优惠券尚未生效");
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            throw new IllegalStateException("优惠券已失效");
        }
        if (!"ACTIVE".equalsIgnoreCase(coupon.getStatus())) {
            throw new IllegalStateException("优惠券状态不可用");
        }
        if (!"AVAILABLE".equalsIgnoreCase(customerCoupon.getStatus())) {
            throw new IllegalStateException("优惠券已使用或不可用");
        }
        customerCoupon.setStatus("USED");
        customerCouponMapper.updateById(customerCoupon);
    }
}
