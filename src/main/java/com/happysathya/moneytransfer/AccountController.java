package com.happysathya.moneytransfer;

import com.happysathya.moneytransfer.domain.Account;
import com.happysathya.moneytransfer.dto.AccountRequest;
import com.happysathya.moneytransfer.dto.AccountResponse;
import com.happysathya.moneytransfer.dto.DepositRequest;
import com.happysathya.moneytransfer.dto.ErrorResponse;
import com.happysathya.moneytransfer.dto.TransferRequest;
import com.happysathya.moneytransfer.dto.WithdrawRequest;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.happysathya.moneytransfer.domain.Account.AccountBuilder;

public class AccountController {

    private List<Account> accounts = new ArrayList<>();

    private static void logException(RuntimeException ex) {
        System.out.println(ex.getMessage());
    }

    public void createNewAccount(Context context) {
        AccountRequest accountRequest = context.bodyAsClass(AccountRequest.class);
        Account account = new AccountBuilder()
                .setAccountHolderName(accountRequest.getAccountHolderName())
                .setBalance(new BigDecimal(0))
                .build();
        accounts.add(account);
        context.json(mapToAccountResponse(account));
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return new AccountResponse(account.getAccountId().toString(),
                account.getAccountHolderName(),
                account.getBalance());
    }

    public void getAccounts(Context context) {
        context.json(accounts.stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList()));
    }

    public void getAccount(Context context) {
        String accountId = context.pathParam("accountId");
        findAccount(accountId)
                .ifPresentOrElse(account -> context.json(mapToAccountResponse(account)), () -> context.status(404));
    }

    @NotNull
    private Optional<Account> findAccount(String accountId) {
        return accounts.stream()
                .filter(account -> account.getAccountId().toString().equals(accountId))
                .findFirst();
    }

    public void depositAmount(Context context) {
        handle(() -> {
            String accountId = context.pathParam("accountId");
            findAccount(accountId)
                    .ifPresentOrElse(account -> {
                        DepositRequest depositRequest = context.bodyAsClass(DepositRequest.class);
                        account.depositAmount(depositRequest.getAmount());
                        context.json(mapToAccountResponse(account));
                    }, () -> context.status(404));
        }, context);
    }

    public void transferAmount(Context context) {
        handle(() -> {
            String accountId = context.pathParam("accountId");
            findAccount(accountId)
                    .ifPresentOrElse(account -> {
                        TransferRequest transferRequest = context.bodyAsClass(TransferRequest.class);
                        String toAccountId = transferRequest.getToAccountId();
                        findAccount(toAccountId)
                                .ifPresentOrElse(toAccount -> {
                                    account.transferTo(toAccount, transferRequest.getAmount());
                                    context.json(account);
                                }, () -> context.status(404));
                    }, () -> context.status(404));
        }, context);
    }

    public void withdrawAmount(Context context) {
        handle(() -> {
            String accountId = context.pathParam("accountId");
            findAccount(accountId)
                    .ifPresentOrElse(account -> {
                        WithdrawRequest withdrawRequest = context.bodyAsClass(WithdrawRequest.class);
                        account.withdrawAmount(withdrawRequest.getAmount());
                        context.json(mapToAccountResponse(account));
                    }, () -> context.status(404));
        }, context);
    }

    private void handle(Runnable runnable, Context context) {
        try {
            runnable.run();
        } catch (IllegalStateException ex) {
            logException(ex);
            context.status(400).json(new ErrorResponse(ex.getMessage()));
        } catch (RuntimeException ex) {
            logException(ex);
            context.status(500).json(new ErrorResponse(ex.getMessage()));
        }
    }
}
