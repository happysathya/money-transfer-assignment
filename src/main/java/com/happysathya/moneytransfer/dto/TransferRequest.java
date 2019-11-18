package com.happysathya.moneytransfer.dto;

import java.math.BigDecimal;

public class TransferRequest {

    private BigDecimal amount;
    private String toAccountId;

    public BigDecimal getAmount() {
        return amount;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }
}
