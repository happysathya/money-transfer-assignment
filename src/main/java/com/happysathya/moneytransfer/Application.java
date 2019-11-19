package com.happysathya.moneytransfer;

import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class Application {

    public static void main(String[] args) {
        Javalin app = new Application().registerRoutesAndStartApp(7000);
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    public Javalin registerRoutesAndStartApp(int port) {
        AccountController accountController = new AccountController();

        JavalinJackson.configure(JavalinJackson.getObjectMapper());
        return Javalin.create(config -> {
            config.defaultContentType = "application/json";
        }).routes(() -> {
            path("accounts", () -> {
                get(accountController::getAccounts);
                post(accountController::createNewAccount);
                path(":accountId", () -> {
                    get(accountController::getAccount);
                    path("deposit", () -> {
                        post(accountController::depositAmount);
                    });
                    path("withdraw", () -> {
                        post(accountController::withdrawAmount);
                    });
                    path("transfer", () -> {
                        post(accountController::transferAmount);
                    });
                });
            });
        }).start(port);
    }
}
