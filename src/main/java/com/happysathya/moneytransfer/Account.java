package com.happysathya.moneytransfer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class Account {

    private UUID accountId;
    private String accountHolderName;
    private BigDecimal balance;
    private ReentrantLock reentrantLock;

    private Account(AccountBuilder accountBuilder) {
        accountId = UUID.randomUUID();
        reentrantLock = new ReentrantLock();
        accountHolderName = accountBuilder.accountHolderName;
        balance = accountBuilder.balance;
    }

    private static void validate(Account account) {
        if (account.accountHolderName == null || account.accountHolderName.trim().length() == 0)
            throw new IllegalStateException("Account holder name cannot be null or empty");
        if (account.balance.compareTo(new BigDecimal(0.0)) < 0)
            throw new IllegalStateException("Account balance cannot be negative");
    }

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal(0.0)) <= 0) {
            throw new IllegalStateException(String.format("Amount %s cannot be zero or negative", rounded(amount)));
        }
    }

    private static BigDecimal rounded(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_DOWN);
    }

    private static void logEvent(String action, Account account, BigDecimal amount) {
        System.out.printf("%s-%s-%s%n", action, account.accountId, rounded(amount));
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public BigDecimal getBalance() {
        reentrantLock.lock();
        try {
            return balance;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void depositAmount(BigDecimal amount) {
        validatePositiveAmount(amount);
        try {
            reentrantLock.lock();
            logEvent("DEPOSIT", this, amount);
            balance = getBalance().add(amount);
        } finally {
            reentrantLock.unlock();
        }
    }

    public void withdrawAmount(BigDecimal amount) {
        validatePositiveAmount(amount);
        try {
            reentrantLock.lock();
            if (getBalance().compareTo(amount) < 0)
                throw new IllegalStateException(String.format("Withdrawal amount %s is greater than balance %s", rounded(amount), rounded(balance)));
            logEvent("WITHDRAW", this, amount);
            balance = getBalance().subtract(amount);
        } finally {
            reentrantLock.unlock();
        }
    }

    public void transferTo(Account toAccount, BigDecimal amount) {
        withdrawAmount(amount);
        toAccount.depositAmount(amount);
    }

    public static class AccountBuilder {

        private String accountHolderName;
        private BigDecimal balance;

        public AccountBuilder setAccountHolderName(String accountHolderName) {
            this.accountHolderName = accountHolderName;
            return this;
        }

        public AccountBuilder setBalance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public Account build() {
            Account account = new Account(this);
            validate(account);
            return account;
        }

    }
}
