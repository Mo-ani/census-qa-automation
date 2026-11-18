package com.census.qa.pages;

import com.census.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared browser interactions for all page and component objects.
 *
 * <p>The app is a React single-page application: elements are re-rendered all the time.
 * Therefore the framework never keeps WebElement references (no PageFactory) - every
 * action looks the element up again by its locator and waits explicitly for the state
 * it needs. There is no implicit wait and no Thread.sleep anywhere.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Config.explicitWait());
        this.wait.ignoring(StaleElementReferenceException.class);
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /** Replaces the content of a text field (select all + type, which React registers reliably). */
    protected void type(By locator, String text) {
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(locator));
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        if (text != null && !text.isEmpty()) {
            field.sendKeys(text);
        }
    }

    protected String textOf(By locator) {
        return visible(locator).getText().trim();
    }

    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected void waitUntilGone(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected List<String> textsOf(By locator) {
        return driver.findElements(locator).stream()
                .map(element -> element.getText().trim())
                .collect(Collectors.toList());
    }

    /** Quotes a value for use inside an XPath expression. */
    protected static String xpathText(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }
}
