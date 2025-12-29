package com.yourcompany.supermarket.controller;

public class CheckoutRequest {
    private Long customerId;
    private Long customerCouponId;
    private String payMethod; // WALLET or CASH

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getCustomerCouponId() {
        return customerCouponId;
    }

    public void setCustomerCouponId(Long customerCouponId) {
        this.customerCouponId = customerCouponId;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }
}
