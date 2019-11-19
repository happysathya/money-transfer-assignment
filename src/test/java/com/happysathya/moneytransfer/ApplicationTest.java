package com.happysathya.moneytransfer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.happysathya.moneytransfer.dto.AccountRequest;
import com.happysathya.moneytransfer.dto.AccountResponse;
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
}
