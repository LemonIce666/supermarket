package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yourcompany.supermarket.entity.Customer;
import com.yourcompany.supermarket.entity.CustomerTransaction;
import com.yourcompany.supermarket.mapper.CustomerMapper;
import com.yourcompany.supermarket.mapper.CustomerTransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerTransactionMapper transactionMapper;

    public Customer create(Customer customer) {
        if (customer.getWalletBalance() == null) {
            customer.setWalletBalance(BigDecimal.ZERO);
        }
        if (customer.getTotalSpent() == null) {
            customer.setTotalSpent(BigDecimal.ZERO);
        }
        customerMapper.insert(customer);
        return customer;
    }

    public Customer update(Customer customer) {
        if (customer.getWalletBalance() == null) {
            customer.setWalletBalance(BigDecimal.ZERO);
        }
        if (customer.getTotalSpent() == null) {
            customer.setTotalSpent(BigDecimal.ZERO);
        }
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
    public void rechargeWallet(Long customerId, BigDecimal amount) {
        validateAmount(amount);
        Customer customer = findCustomer(customerId);
        customer.setWalletBalance(customer.getWalletBalance().add(amount));
        customerMapper.updateById(customer);

        CustomerTransaction transaction = buildTransaction(customerId, amount, "RECHARGE", "Wallet recharge");
        transactionMapper.insert(transaction);
    }

    @Transactional
    public void spend(Long customerId, BigDecimal amount) {
        validateAmount(amount);
        Customer customer = findCustomer(customerId);
        if (customer.getWalletBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("余额不足");
        }
        customer.setWalletBalance(customer.getWalletBalance().subtract(amount));
        customer.setTotalSpent(customer.getTotalSpent().add(amount));
        customerMapper.updateById(customer);

        CustomerTransaction transaction = buildTransaction(customerId, amount.negate(), "SPEND", "Wallet spend");
        transactionMapper.insert(transaction);
    }

    public CustomerConsumptionStats getConsumptionStats() {
        QueryWrapper<CustomerTransaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", "SPEND");
        List<CustomerTransaction> spendTransactions = transactionMapper.selectList(queryWrapper);

        BigDecimal total = spendTransactions.stream()
                .map(CustomerTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
        long count = spendTransactions.size();
        BigDecimal average = count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);

        CustomerConsumptionStats stats = new CustomerConsumptionStats();
        stats.setTotalAmount(total);
        stats.setTransactionCount(count);
        stats.setAverageTicket(average);
        return stats;
    }

    public List<CustomerTransaction> listTransactions(Long customerId) {
        QueryWrapper<CustomerTransaction> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_id", customerId);
        return transactionMapper.selectList(wrapper);
    }

    private Customer findCustomer(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("会员不存在");
        }
        if (customer.getWalletBalance() == null) {
            customer.setWalletBalance(BigDecimal.ZERO);
        }
        if (customer.getTotalSpent() == null) {
            customer.setTotalSpent(BigDecimal.ZERO);
        }
        return customer;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
    }

    private CustomerTransaction buildTransaction(Long customerId, BigDecimal amount, String type, String description) {
        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setCustomerId(customerId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }
}
