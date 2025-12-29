package com.yourcompany.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.supermarket.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
