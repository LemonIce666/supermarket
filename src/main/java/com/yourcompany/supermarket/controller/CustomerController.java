package com.yourcompany.supermarket.controller;

import com.yourcompany.supermarket.entity.Customer;
import com.yourcompany.supermarket.entity.CustomerCoupon;
import com.yourcompany.supermarket.service.CouponService;
import com.yourcompany.supermarket.service.CustomerConsumptionStats;
import com.yourcompany.supermarket.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CouponService couponService;

    @GetMapping
    public List<Customer> listCustomers() {
        return customerService.listAll();
    }

    @GetMapping("/{id}")
    public Customer getCustomer(@PathVariable Long id) {
        Customer customer = customerService.get(id);
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        return customer;
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerService.create(customer);
    }

    @PutMapping("/{id}")
    public Customer updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        return customerService.update(customer);
    }

    @PostMapping("/{id}/wallet/recharge")
    public void recharge(@PathVariable Long id, @RequestBody WalletChangeRequest request) {
        try {
            customerService.rechargeWallet(id, request.getAmount());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/wallet/spend")
    public void spend(@PathVariable Long id, @RequestBody WalletChangeRequest request) {
        try {
            customerService.spend(id, request.getAmount());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/coupons")
    public CustomerCoupon assignCoupon(@PathVariable Long id, @RequestBody AssignCouponRequest request) {
        if (request.getCouponId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "couponId is required");
        }
        try {
            return couponService.assignToCustomer(id, request.getCouponId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/coupons")
    public List<CustomerCoupon> listCustomerCoupons(@PathVariable Long id, @RequestParam(value = "status", required = false) String status) {
        if (customerService.get(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        return couponService.listCustomerCoupons(id, status);
    }

    @GetMapping("/stats/consumption")
    public CustomerConsumptionStats getConsumptionStats() {
        return customerService.getConsumptionStats();
    }
}
