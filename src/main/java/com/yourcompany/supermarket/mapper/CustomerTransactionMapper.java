package com.yourcompany.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.supermarket.entity.CustomerTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerTransactionMapper extends BaseMapper<CustomerTransaction> {
}
