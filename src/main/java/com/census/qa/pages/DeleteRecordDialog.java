package com.census.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

/** Component object for the "Are you absolutely sure?" confirmation shown before a record is deleted. */
public class DeleteRecordDialog extends BasePage {

    private static final String DIALOG = "//div[@role='alertdialog']";

    private static final By ROOT = By.xpath(DIALOG);
    private static final By CONFIRM_DELETE = By.xpath(DIALOG + "//button[normalize-space()='Delete']");
    private static final By DISMISS = By.xpath(DIALOG + "//button[normalize-space()='Cancel' or normalize-space()='OK']");
    private static final By SUCCESS_MESSAGE = By.xpath(DIALOG + "//*[@data-testid='success-message']");
    private static final By ERROR_MESSAGE = By.xpath(DIALOG + "//*[@data-testid='error-message']");

    public DeleteRecordDialog(WebDriver driver) {
        super(driver);
    }

    DeleteRecordDialog waitUntilOpen() {
        visible(ROOT);
        visible(CONFIRM_DELETE);
        return this;
    }

    public DeleteRecordDialog confirm() {
        click(CONFIRM_DELETE);
        return this;
    }

    public String successMessage() {
        return textOf(SUCCESS_MESSAGE);
    }

    public String errorMessage() {
        return textOf(ERROR_MESSAGE);
    }

    /**
     * After a successful delete the application refreshes the list by itself about two seconds
     * later, which also removes this dialog; wait for that instead of racing it with a click.
     */
    public DashboardPage waitUntilClosedAfterDelete() {
        waitUntilGone(ROOT);
        return new DashboardPage(driver).waitUntilLoaded();
    }

    /** Closes the dialog without deleting (or after the delete was refused). */
    public DashboardPage dismiss() {
        try {
            click(DISMISS);
        } catch (TimeoutException alreadyClosed) {
            // nothing to dismiss
        }
        waitUntilGone(ROOT);
        return new DashboardPage(driver);
    }
}
