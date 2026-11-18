package com.census.qa.pages;

import com.census.qa.model.PersonRecord;
import com.census.qa.model.RecordMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.util.List;

/**
 * Component object for the "Add new person to your household" / "Update existing person's data"
 * dialog. The same form is used for both, so one class serves the add and the update scenarios.
 */
public class RecordDialog extends BasePage {

    /** The calendar pop-up is a second element with role=dialog; the form identifies the right one. */
    private static final String DIALOG = "//div[@role='dialog'][.//form]";

    private static final By ROOT = By.xpath(DIALOG);
    private static final By FIRST_NAME = By.xpath(DIALOG + "//input[@name='firstName']");
    private static final By LAST_NAME = By.xpath(DIALOG + "//input[@name='lastName']");
    private static final By DOB = By.xpath(DIALOG + "//input[@placeholder='Pick a date']");
    private static final By CALENDAR = By.cssSelector("[data-slot='calendar']");
    private static final By CALENDAR_YEAR = By.cssSelector("[data-slot='calendar'] select.rdp-years_dropdown");
    private static final By CALENDAR_MONTH = By.cssSelector("[data-slot='calendar'] select.rdp-months_dropdown");
    private static final By TELEPHONE = By.xpath(DIALOG + "//input[@name='telephone']");
    private static final By HISPANIC_OTHER = By.xpath(DIALOG + "//input[@name='hispanicOther']");
    private static final By RACE_OTHER = By.xpath(DIALOG + "//input[@name='raceOther']");
    private static final By SUBMIT = By.xpath(DIALOG + "//button[@type='submit']");
    private static final By CLOSE = By.xpath(DIALOG + "//button[@type='reset']");
    private static final By SUCCESS_MESSAGE = By.xpath(DIALOG + "//*[@data-testid='success-message']");
    private static final By ERROR_MESSAGE = By.xpath(DIALOG + "//*[@data-testid='error-message']");
    private static final By FIELD_MESSAGES = By.xpath(DIALOG + "//p[contains(@id,'-form-item-message')]");
    /** The open option list of a drop-down is rendered outside the dialog, at the end of the page. */
    private static final By OPTION_LIST = By.xpath("//div[@role='listbox']");

    public RecordDialog(WebDriver driver) {
        super(driver);
    }

    RecordDialog waitUntilOpen() {
        visible(ROOT);
        visible(SUBMIT);
        return this;
    }

    /** Fills every field of the form from the model. Optional values that are null are cleared. */
    public RecordDialog fill(PersonRecord person) {
        selectRelationship(person.getRelationship());
        setFirstName(person.getFirstName());
        setLastName(person.getLastName());
        if (person.getDob() != null) {
            pickDob(person.getDob());
        }
        setTelephone(person.getTelephone());
        selectGender(person.getGender());
        selectHispanic(person.getHispanic());
        if ("OTHER".equals(person.getHispanic())) {
            type(HISPANIC_OTHER, person.getHispanicOther());
        }
        selectRace(person.getRace());
        if ("OTHER".equals(person.getRace())) {
            type(RACE_OTHER, person.getRaceOther());
        }
        selectOtherStay(person.getOtherStay());
        return this;
    }

    // --- single fields (also used directly by the validation scenarios) -----------

    public RecordDialog selectRelationship(String relationship) {
        return select("relationship", relationship);
    }

    public RecordDialog setFirstName(String firstName) {
        type(FIRST_NAME, firstName);
        return this;
    }

    public RecordDialog setLastName(String lastName) {
        type(LAST_NAME, lastName);
        return this;
    }

    /**
     * Picks the date of birth in the calendar pop-up: year, month, then the day.
     *
     * <p>The field looks like a text box but is rendered as a button that only opens the calendar,
     * so the date cannot be typed. Year and month are native select elements that the app hides
     * behind a styled label (opacity 0); Selenium treats them as not interactable, therefore the
     * value is set through the DOM together with the "change" event a real selection fires.
     * The day itself is a normal click.
     */
    public RecordDialog pickDob(LocalDate dob) {
        click(DOB);
        visible(CALENDAR);
        chooseInHiddenSelect(CALENDAR_YEAR, String.valueOf(dob.getYear()));
        chooseInHiddenSelect(CALENDAR_MONTH, String.valueOf(dob.getMonthValue() - 1));
        click(By.cssSelector("[data-slot='calendar'] td[data-day='" + dob + "'] button"));
        waitUntilGone(CALENDAR);
        wait.until(ExpectedConditions.attributeToBe(DOB, "value", dob.format(RecordMapper.FORM_DATE)));
        return this;
    }

    /** Value currently shown in the date-of-birth field (MM/dd/yyyy), empty when nothing was picked. */
    public String dobValue() {
        String value = visible(DOB).getAttribute("value");
        return value == null ? "" : value;
    }

    public RecordDialog setTelephone(String telephone) {
        type(TELEPHONE, telephone);
        return this;
    }

    public RecordDialog selectGender(String gender) {
        if (gender != null) {
            click(By.xpath(DIALOG + "//button[@role='radio'][@value=" + xpathText(gender) + "]"));
        }
        return this;
    }

    public RecordDialog selectHispanic(String hispanic) {
        return select("hispanic", hispanic);
    }

    public RecordDialog selectRace(String race) {
        return select("race", race);
    }

    public RecordDialog selectOtherStay(String otherStay) {
        return select("otherStay", otherStay);
    }

    // --- actions and results ------------------------------------------------------

    public RecordDialog submit() {
        click(SUBMIT);
        return this;
    }

    /** Text of the green banner, e.g. "Successfully saved new record for 'SPOUSE'". */
    public String successMessage() {
        return textOf(SUCCESS_MESSAGE);
    }

    /** Text of the red banner returned by the server, e.g. "You already have SELF record." */
    public String errorMessage() {
        return textOf(ERROR_MESSAGE);
    }

    /** Client-side validation messages shown under the individual fields. */
    public List<String> fieldValidationMessages() {
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(FIELD_MESSAGES, 0));
        return textsOf(FIELD_MESSAGES);
    }

    /**
     * Closes the dialog with its OK/Cancel button. That button - not the X icon - is what makes
     * the dashboard reload the list of records, so the page object always uses it.
     */
    public DashboardPage close() {
        click(CLOSE);
        waitUntilGone(ROOT);
        return new DashboardPage(driver).waitUntilLoaded();
    }

    // --- helpers ------------------------------------------------------------------

    /**
     * Picks an option of a custom (Radix UI) drop-down: it is a button plus a floating list,
     * not a native select element, so Selenium's Select class cannot be used.
     *
     * @param fieldId id of the form item that wraps the drop-down, e.g. "relationship"
     */
    private RecordDialog select(String fieldId, String option) {
        if (option == null) {
            return this;
        }
        By trigger = By.xpath(DIALOG + "//div[@id='" + fieldId + "']//button[@role='combobox']");
        By item = By.xpath("//div[@role='listbox']//div[@role='option'][normalize-space()=" + xpathText(option) + "]");

        click(trigger);
        WebElement choice = wait.until(ExpectedConditions.presenceOfElementLocated(item));
        scrollIntoView(choice);
        wait.until(ExpectedConditions.elementToBeClickable(choice)).click();
        waitUntilGone(OPTION_LIST);
        wait.until(ExpectedConditions.textToBe(trigger, option));
        return this;
    }

    private void chooseInHiddenSelect(By select, String value) {
        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(select));
        Boolean optionExists = (Boolean) js().executeScript(
                "const select = arguments[0], value = arguments[1];"
                        + "if (![...select.options].some(option => option.value === value)) return false;"
                        + "select.value = value;"
                        + "select.dispatchEvent(new Event('change', { bubbles: true }));"
                        + "return true;",
                element, value);
        if (!Boolean.TRUE.equals(optionExists)) {
            throw new IllegalArgumentException("The calendar offers no option '" + value + "' in " + select);
        }
        wait.until(ExpectedConditions.attributeToBe(select, "value", value));
    }

    private void scrollIntoView(WebElement element) {
        js().executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }
}
