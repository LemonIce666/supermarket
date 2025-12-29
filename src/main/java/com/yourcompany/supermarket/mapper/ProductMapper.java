package com.yourcompany.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.supermarket.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 你可以根据需要添加自定义查询方法
}
