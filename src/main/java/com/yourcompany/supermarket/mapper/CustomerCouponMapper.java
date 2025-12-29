package com.yourcompany.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.supermarket.entity.CustomerCoupon;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerCouponMapper extends BaseMapper<CustomerCoupon> {
}
