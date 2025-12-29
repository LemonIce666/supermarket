package com.yourcompany.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.supermarket.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
}
