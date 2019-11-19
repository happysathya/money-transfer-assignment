package com.happysathya.moneytransfer.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class AccountResponse {

    private UUID accountId;
    private String accountHolderName;
    private BigDecimal balance;

    private AccountResponse() {
    }

    public AccountResponse(UUID accountId, String accountHolderName, BigDecimal balance) {
        this.accountId = accountId;
        this.accountHolderName = accountHolderName;
        this.balance = balance;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public BigDecimal getBalance() {
        return balance.setScale(2, RoundingMode.HALF_DOWN);
    }
}
