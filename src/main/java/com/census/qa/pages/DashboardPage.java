package com.census.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/** /dashboard - the household card followed by one card per person record. */
public class DashboardPage extends BasePage {

    private static final By HOUSEHOLD_CARD = By.cssSelector(".address-card");
    private static final By RECORDS_CONTAINER = By.cssSelector(".records-container");
    private static final By RECORDS_LOADING = By.cssSelector(".records-container .loading-record-card");
    private static final By ADD_RECORD_BUTTON =
            By.xpath("//div[contains(@class,'records-container')]//button[normalize-space()='Add record']");
    private static final By FULL_NAMES = By.cssSelector(".record-card .full-name");
    private static final By SUCCESS_MESSAGE = By.cssSelector("[data-testid='success-message']");
    private static final By ERROR_MESSAGE = By.cssSelector("[data-testid='error-message']");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    /** The records are fetched after the page renders; wait until the skeleton placeholder is replaced. */
    public DashboardPage waitUntilLoaded() {
        visible(HOUSEHOLD_CARD);
        wait.until(ExpectedConditions.presenceOfElementLocated(RECORDS_CONTAINER));
        waitUntilGone(RECORDS_LOADING);
        visible(ADD_RECORD_BUTTON);
        return this;
    }

    public RecordDialog openAddRecordDialog() {
        click(ADD_RECORD_BUTTON);
        return new RecordDialog(driver).waitUntilOpen();
    }

    /** The card of one person, found by the full name shown on it. */
    public RecordCard recordCard(String firstName, String lastName) {
        return RecordCard.byFullName(driver, firstName, lastName);
    }

    /** The card of the person with the given relationship, e.g. SELF. */
    public RecordCard recordCardByRelationship(String relationship) {
        return RecordCard.byRelationship(driver, relationship);
    }

    public boolean isRecordListed(String firstName, String lastName) {
        return recordCard(firstName, lastName).isDisplayed();
    }

    /** Waits until the list has been refreshed and the person is no longer on it. */
    public void waitUntilRecordIsRemoved(String firstName, String lastName) {
        wait.until(driver -> !recordCard(firstName, lastName).isDisplayed());
        waitUntilGone(RECORDS_LOADING);
    }

    /** Green result banner of whichever dialog is open (record form or delete confirmation). */
    public String successMessage() {
        return textOf(SUCCESS_MESSAGE);
    }

    /** Red result banner of whichever dialog is open (record form or delete confirmation). */
    public String errorMessage() {
        return textOf(ERROR_MESSAGE);
    }

    /** Full names of all listed people, without the "Full name:" label. */
    public List<String> listedFullNames() {
        return textsOf(FULL_NAMES).stream()
                .map(text -> text.replaceFirst("^Full name:\\s*", ""))
                .toList();
    }
}
