package com.yourcompany.supermarket.controller;

import java.math.BigDecimal;

public class WalletChangeRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
