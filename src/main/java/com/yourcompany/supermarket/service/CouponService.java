package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.Coupon;
import com.yourcompany.supermarket.mapper.CouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {

    @Autowired
    private CouponMapper couponMapper;

    public Coupon create(Coupon coupon) {
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

    public boolean isValid(Coupon coupon) {
        if (coupon == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return "ACTIVE".equalsIgnoreCase(coupon.getStatus())
                && (coupon.getStartTime() == null || !now.isBefore(coupon.getStartTime()))
                && (coupon.getEndTime() == null || !now.isAfter(coupon.getEndTime()));
    }

    public Coupon redeem(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (!isValid(coupon)) {
            throw new IllegalStateException("优惠券不可用或已过期");
        }
        coupon.setStatus("USED");
        couponMapper.updateById(coupon);
        return coupon;
    }
}
