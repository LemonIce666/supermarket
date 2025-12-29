package com.yourcompany.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.supermarket.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
