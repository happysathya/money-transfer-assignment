package com.happysathya.moneytransfer.domain;

import com.happysathya.moneytransfer.domain.Account;
import com.happysathya.moneytransfer.domain.Account.AccountBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountTest {

    @Test
    public void shouldValidateAndThrowException_ifAccountHolderNameIsNullOrEmpty() {
        IllegalStateException exception1 = assertThrows(IllegalStateException.class, () ->
                new AccountBuilder()
                        .setAccountHolderName(null)
                        .build());
        assertEquals("Account holder name cannot be null or empty", exception1.getMessage());

        IllegalStateException exception2 = assertThrows(IllegalStateException.class, () ->
                new AccountBuilder()
                        .setAccountHolderName("")
                        .build());
        assertEquals("Account holder name cannot be null or empty", exception2.getMessage());
    }

    @Test
    public void shouldValidateAndThrowException_ifInitialAccountBalanceIsNegative() {
        IllegalStateException exception2 = assertThrows(IllegalStateException.class, () ->
                new AccountBuilder()
                        .setAccountHolderName("ZZZ")
                        .setBalance(new BigDecimal(-0.10))
                        .build());
        assertEquals("Account balance cannot be negative", exception2.getMessage());
    }

    @Test
    public void shouldValidateAndThrowException_ifAmountToBeDepositedIsZeroOrNegative() {
        Account account = new AccountBuilder()
                .setAccountHolderName("ZZZ")
                .setBalance(new BigDecimal(10.00))
                .build();

        IllegalStateException exception1 = assertThrows(IllegalStateException.class, () ->
                account.depositAmount(new BigDecimal(0)));
        assertEquals("Amount 0.00 cannot be zero or negative", exception1.getMessage());

        IllegalStateException exception2 = assertThrows(IllegalStateException.class, () ->
                account.depositAmount(new BigDecimal(-1)));
        assertEquals("Amount -1.00 cannot be zero or negative", exception2.getMessage());
    }

    @Test
    public void shouldValidateAndThrowException_ifAmountToBeWithdrawnIsGreaterThanBalance() {
        Account account = new AccountBuilder()
                .setAccountHolderName("ZZZ")
                .setBalance(new BigDecimal(10.00))
                .build();

        IllegalStateException exception1 = assertThrows(IllegalStateException.class, () ->
                account.withdrawAmount(new BigDecimal(12.00)));
        assertEquals("Withdrawal amount 12.00 is greater than balance 10.00", exception1.getMessage());

        IllegalStateException exception2 = assertThrows(IllegalStateException.class, () ->
                account.withdrawAmount(new BigDecimal(10.01)));
        assertEquals("Withdrawal amount 10.01 is greater than balance 10.00", exception2.getMessage());
    }

    @Test
    public void shouldReflectTheBalanceCorrectly_afterDepositAndWithdrawalActions() {
        Account account = new AccountBuilder()
                .setAccountHolderName("ZZZ")
                .setBalance(new BigDecimal(10.00))
                .build();
        account.depositAmount(new BigDecimal(25.50));
        account.withdrawAmount(new BigDecimal(12.00));
        account.withdrawAmount(new BigDecimal(1.25));
        assertEquals(0, account.getBalance().compareTo(new BigDecimal(22.25)));
    }

    @Test
    public void withdrawalWithParallelExecution_ShouldReturnConsistentResult() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        Account account = new AccountBuilder()
                .setAccountHolderName("ZZZ")
                .setBalance(new BigDecimal(10.00))
                .build();
        Callable<Void> withdrawAmount = () -> {
            account.withdrawAmount(new BigDecimal(3.00));
            return null;
        };
        List<Callable<Void>> withdraw100TimesInParallel = IntStream.rangeClosed(1, 100)
                .mapToObj(value -> withdrawAmount)
                .collect(Collectors.toList());
        executor.invokeAll(withdraw100TimesInParallel);
        executor.shutdown();
        assertEquals(0, account.getBalance().compareTo(new BigDecimal(1.00)));
    }

    @Test
    public void sequentialActions_TransferMoneyShouldMaintainTheBalanceCorrectly() {
        Account account1 = new AccountBuilder()
                .setAccountHolderName("ZZZ")
                .setBalance(new BigDecimal(10.00))
                .build();
        Account account2 = new AccountBuilder()
                .setAccountHolderName("YYY")
                .setBalance(new BigDecimal(20.00))
                .build();
        Account account3 = new AccountBuilder()
                .setAccountHolderName("XXX")
                .setBalance(new BigDecimal(20.00))
                .build();
        account1.transferTo(account2, new BigDecimal(5.00));
        account2.transferTo(account3, new BigDecimal(20.00));
        account3.transferTo(account1, new BigDecimal(5.00));
        assertEquals(0, account1.getBalance().compareTo(new BigDecimal(10)));
        assertEquals(0, account2.getBalance().compareTo(new BigDecimal(5.00)));
        assertEquals(0, account3.getBalance().compareTo(new BigDecimal(35.00)));
    }

    @Test
    public void parallelActions_TransferMoneyShouldMaintainTheBalanceCorrectly() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        Account account1 = new AccountBuilder()
                .setAccountHolderName("ZZZ")
                .setBalance(new BigDecimal(10.00))
                .build();
        Account account2 = new AccountBuilder()
                .setAccountHolderName("YYY")
                .setBalance(new BigDecimal(20.00))
                .build();
        Account account3 = new AccountBuilder()
                .setAccountHolderName("XXX")
                .setBalance(new BigDecimal(20.00))
                .build();
        Callable<Void> transfer1 = () -> {
            account1.transferTo(account2, new BigDecimal(5.00));
            account2.transferTo(account3, new BigDecimal(20.00));
            return null;
        };
        Callable<Void> transfer2 = () -> {
            account2.transferTo(account3, new BigDecimal(20.00));
            account3.transferTo(account1, new BigDecimal(5.00));
            return null;
        };
        Callable<Void> transfer3 = () -> {
            account3.transferTo(account1, new BigDecimal(5.00));
            return null;
        };
        executor.invokeAll(Arrays.asList(transfer1, transfer2, transfer3));
        executor.shutdown();
        assertEquals(0, account1.getBalance()
                .add(account2.getBalance())
                .add(account3.getBalance())
                .compareTo(new BigDecimal(50.00)));
    }

}
