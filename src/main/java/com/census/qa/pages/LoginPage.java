package com.census.qa.pages;

import com.census.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** /auth/login */
public class LoginPage extends BasePage {

    private static final By EMAIL = By.id("email");
    private static final By PASSWORD = By.id("password");
    private static final By LOGIN_BUTTON = By.id("login-button");
    private static final By ERROR_MESSAGE = By.cssSelector("[data-testid='error-message']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(Config.baseUrl() + "/auth/login");
        visible(LOGIN_BUTTON);
        return this;
    }

    /** Logs in and waits until the application has redirected to the dashboard. */
    public DashboardPage loginAs(String email, String password) {
        submit(email, password);
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        return new DashboardPage(driver).waitUntilLoaded();
    }

    /** Submits the form without expecting a redirect (for negative login tests). */
    public LoginPage submit(String email, String password) {
        type(EMAIL, email);
        type(PASSWORD, password);
        click(LOGIN_BUTTON);
        return this;
    }

    public String errorMessage() {
        return textOf(ERROR_MESSAGE);
    }
}
