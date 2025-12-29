package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.Customer;
import com.yourcompany.supermarket.mapper.CustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    public Customer create(Customer customer) {
        if (customer.getWalletBalance() == null) {
            customer.setWalletBalance(0.0);
        }
        if (customer.getTotalSpent() == null) {
            customer.setTotalSpent(0.0);
        }
        customerMapper.insert(customer);
        return customer;
    }

    public Customer update(Customer customer) {
        customerMapper.updateById(customer);
        return customer;
    }

    public Customer get(Long id) {
        return customerMapper.selectById(id);
    }

    public List<Customer> listAll() {
        return customerMapper.selectList(new QueryWrapper<>());
    }

    public void delete(Long id) {
        customerMapper.deleteById(id);
    }

    @Transactional
    public void rechargeWallet(Long customerId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("充值金额必须大于 0");
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("会员不存在");
        }
        customer.setWalletBalance(customer.getWalletBalance() + amount);
        customerMapper.updateById(customer);
    }

    @Transactional
    public void spend(Long customerId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("消费金额必须大于 0");
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("会员不存在");
        }
        if (customer.getWalletBalance() < amount) {
            throw new IllegalStateException("余额不足");
        }
        customer.setWalletBalance(customer.getWalletBalance() - amount);
        customer.setTotalSpent(customer.getTotalSpent() + amount);
        customerMapper.updateById(customer);
    }
}
