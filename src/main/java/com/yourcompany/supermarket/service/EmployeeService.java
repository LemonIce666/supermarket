package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.Employee;
import com.yourcompany.supermarket.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    public Employee createEmployee(Employee employee) {
        validateRole(employee.getRole());
        if (employee.getEnabled() == null) {
            employee.setEnabled(Boolean.TRUE);
        }
        employeeMapper.insert(employee);
        return employee;
    }

    public Employee updateEmployee(Employee employee) {
        validateRole(employee.getRole());
        employeeMapper.updateById(employee);
        return employee;
    }

    public Employee getEmployee(Long id) {
        return employeeMapper.selectById(id);
    }

    public List<Employee> listAll() {
        return employeeMapper.selectList(new QueryWrapper<>());
    }

    public void toggleStatus(Long id, boolean enabled) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        employee.setEnabled(enabled);
        employeeMapper.updateById(employee);
    }

    public void delete(Long id) {
        employeeMapper.deleteById(id);
    }

    private void validateRole(String role) {
        if (!"BOSS".equalsIgnoreCase(role) && !"STAFF".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("角色必须为 BOSS 或 STAFF");
        }
    }
}
