package com.happysathya.moneytransfer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.happysathya.moneytransfer.dto.AccountRequest;
import com.happysathya.moneytransfer.dto.AccountResponse;
import com.happysathya.moneytransfer.dto.DepositRequest;
import com.happysathya.moneytransfer.dto.ErrorResponse;
import com.happysathya.moneytransfer.dto.TransferRequest;
import com.happysathya.moneytransfer.dto.WithdrawRequest;
import io.javalin.Javalin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApplicationTest {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static Javalin app = new Application().registerRoutesAndStartApp(7001);
    private static OkHttpClient httpClient = new OkHttpClient();

    @AfterAll
    public static void after() {
        app.stop();
    }

    @Test
    @Order(0)
    public void shouldReturnEmptyAccountsToStartWith() throws IOException {
        Request request = new Request.Builder().url("http://localhost:7001/accounts").build();
        Response response = httpClient.newCall(request).execute();
        List<AccountResponse> accounts = objectMapper.readValue(response.body().string(), new TypeReference<List<AccountResponse>>() {
        });
        assertTrue(accounts.isEmpty());
    }

    @Test
    @Order(1)
    public void shouldAddNewAccounts_andVerifyTheAddedAccountsWithEmptyBalance() throws IOException {
        AccountRequest accountRequest1 = new AccountRequest();
        accountRequest1.setAccountHolderName("Revolut XXX");

        AccountRequest accountRequest2 = new AccountRequest();
        accountRequest2.setAccountHolderName("Revolut YYY");

        httpClient.newCall(new Request.Builder().url("http://localhost:7001/accounts")
                .post(RequestBody.create(objectMapper.writeValueAsBytes(accountRequest1))).build()).execute();
        httpClient.newCall(new Request.Builder().url("http://localhost:7001/accounts")
                .post(RequestBody.create(objectMapper.writeValueAsBytes(accountRequest2))).build()).execute();

        Request request = new Request.Builder().url("http://localhost:7001/accounts").build();
        Response response = httpClient.newCall(request).execute();
        List<AccountResponse> accounts = objectMapper.readValue(response.body().bytes(), new TypeReference<List<AccountResponse>>() {
        });
        assertEquals(2, accounts.size());
        assertTrue(accounts.stream()
                .allMatch(accountResponse -> accountResponse.getBalance().compareTo(new BigDecimal(0)) == 0));
    }

    @Test
    @Order(2)
    public void shouldReturn404_forAnInvalidAccount() throws IOException {
        Request request = new Request.Builder().url("http://localhost:7001/accounts/invalidAccount").build();
        Response response = httpClient.newCall(request).execute();
        assertEquals(404, response.code());
    }

    @Test
    @Order(3)
    public void shouldDepositMoney_andReturnCorrectBalance() throws IOException {
        AccountResponse accountResponse = createAccount();

        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal(10.00));

        String depositUrl = String.format("http://localhost:7001/accounts/%s/deposit", accountResponse.getAccountId());
        Response depositResponse = httpClient.newCall(new Request.Builder().url(depositUrl)
                .post(RequestBody.create(objectMapper.writeValueAsBytes(depositRequest))).build()).execute();


        AccountResponse afterDepositResponse = objectMapper.readValue(depositResponse.body().bytes(), AccountResponse.class);
        assertEquals(0, afterDepositResponse.getBalance().compareTo(new BigDecimal(10.00)));
    }

    @Test
    @Order(3)
    public void shouldWithdrawMoney_andReturnCorrectBalance() throws IOException {
        AccountResponse accountResponse = createAccount();

        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal(10.00));

        String depositUrl = String.format("http://localhost:7001/accounts/%s/deposit", accountResponse.getAccountId());
        Response depositResponse = httpClient.newCall(new Request.Builder().url(depositUrl)
                .post(RequestBody.create(objectMapper.writeValueAsBytes(depositRequest))).build()).execute();

        WithdrawRequest withdrawRequest = new WithdrawRequest();
        withdrawRequest.setAmount(new BigDecimal(5.00));

        String withdrawUrl = String.format("http://localhost:7001/accounts/%s/withdraw", accountResponse.getAccountId());
        Response withdrawResponse = httpClient.newCall(new Request.Builder().url(withdrawUrl)
                .post(RequestBody.create(objectMapper.writeValueAsBytes(withdrawRequest))).build()).execute();

        AccountResponse afterWithdrawResponse = objectMapper.readValue(withdrawResponse.body().bytes(), AccountResponse.class);
        assertEquals(0, afterWithdrawResponse.getBalance().compareTo(new BigDecimal(5.00)));
    }

    @Test
    @Order(4)
    public void shouldReturn400_ifWithdrawAmountIsBiggerThanBalance() throws IOException {
        AccountResponse accountResponse = createAccount();

        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal(10.00));

        String depositUrl = String.format("http://localhost:7001/accounts/%s/deposit", accountResponse.getAccountId());
        httpClient.newCall(new Request.Builder().url(depositUrl)
                .post(RequestBody.create(objectMapper.writeValueAsBytes(depositRequest))).build()).execute();

        WithdrawRequest withdrawRequest = new WithdrawRequest();
        withdrawRequest.setAmount(new BigDecimal(15.00));

        String withdrawUrl = String.format("http://localhost:7001/accounts/%s/withdraw", accountResponse.getAccountId());
        Response withdrawResponse = httpClient.newCall(new Request.Builder().url(withdrawUrl)
                .post(RequestBody.create(objectMapper.writeValueAsBytes(withdrawRequest))).build()).execute();

        assertEquals(400, withdrawResponse.code());
        ErrorResponse errorResponse = objectMapper.readValue(withdrawResponse.body().bytes(), ErrorResponse.class);
        assertEquals("Withdrawal amount 15.00 is greater than balance 10.00", errorResponse.getErrorMessage());
    }

    @Test
    @Order(5)
    public void shouldTransferMoney_andReflectBalanceOnBothSenderAndReceiverCorrectly() throws IOException {
        AccountResponse accountResponse1 = createAccount();
        AccountResponse accountResponse2 = createAccount();

        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal(10.00));

        String depositUrl = String.format("http://localhost:7001/accounts/%s/deposit", accountResponse1.getAccountId());
        httpClient.newCall(new Request.Builder().url(depositUrl)
                .post(RequestBody.create(objectMapper.writeValueAsBytes(depositRequest))).build()).execute();

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setAmount(new BigDecimal(3.00));
        transferRequest.setToAccountId(accountResponse2.getAccountId());

        String transferUrl = String.format("http://localhost:7001/accounts/%s/transfer", accountResponse1.getAccountId());
        httpClient.newCall(new Request.Builder().url(transferUrl)
                .post(RequestBody.create(objectMapper.writeValueAsBytes(transferRequest))).build()).execute();

        assertEquals(0, getAccount(accountResponse1.getAccountId()).getBalance().compareTo(new BigDecimal(7.00)));
        assertEquals(0, getAccount(accountResponse2.getAccountId()).getBalance().compareTo(new BigDecimal(3.00)));
    }

    private AccountResponse createAccount() throws IOException {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountHolderName("Revolut MMM");

        Response response = httpClient.newCall(new Request.Builder().url("http://localhost:7001/accounts")
                .post(RequestBody.create(objectMapper.writeValueAsBytes(accountRequest))).build()).execute();

        return objectMapper.readValue(response.body().bytes(), AccountResponse.class);
    }

    private AccountResponse getAccount(String accountId) throws IOException {
        String accountUrl = String.format("http://localhost:7001/accounts/%s", accountId);
        Response response = httpClient.newCall(new Request.Builder().url(accountUrl).build()).execute();

        return objectMapper.readValue(response.body().bytes(), AccountResponse.class);
    }


}
