package com.census.qa.pages;

import com.census.qa.model.PersonRecord;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Component object for one person card on the dashboard.
 *
 * <p>The card is identified by a stable business value (the person's full name, or the
 * relationship) instead of its position in the list, and every value is looked up
 * inside that card only.
 */
public class RecordCard extends BasePage {

    /** The card prints the date with the browser locale; the framework starts browsers in en-US. */
    private static final DateTimeFormatter CARD_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private static final By RELATIONSHIP = By.xpath(".//p[b[starts-with(normalize-space(),'Relationship')]]");
    private static final By FULL_NAME = By.cssSelector("p.full-name");
    private static final By DOB = By.cssSelector("p.dob");
    private static final By GENDER = By.cssSelector("p.gender");
    private static final By TELEPHONE = By.cssSelector("p#telephone");
    private static final By HISPANIC = By.cssSelector("p.hispanic");
    private static final By HISPANIC_OTHER = By.cssSelector("p.hispanic-other");
    private static final By RACE = By.cssSelector("p.race");
    private static final By RACE_OTHER = By.cssSelector("p.race-other");
    private static final By OTHER_STAY = By.cssSelector("p.other-stay");
    private static final By EDIT_BUTTON = By.xpath(".//button[normalize-space()='Edit']");
    private static final By DELETE_BUTTON = By.xpath(".//button[normalize-space()='Delete']");

    private final By card;

    private RecordCard(WebDriver driver, By card) {
        super(driver);
        this.card = card;
    }

    static RecordCard byFullName(WebDriver driver, String firstName, String lastName) {
        return new RecordCard(driver, By.xpath("//div[contains(@class,'record-card')]"
                + "[.//p[contains(@class,'full-name')][normalize-space()="
                + xpathText("Full name: " + firstName + " " + lastName) + "]]"));
    }

    static RecordCard byRelationship(WebDriver driver, String relationship) {
        return new RecordCard(driver, By.xpath("//div[contains(@class,'record-card')]"
                + "[.//p[normalize-space()=" + xpathText("Relationship: " + relationship) + "]]"));
    }

    public boolean isDisplayed() {
        List<WebElement> cards = driver.findElements(card);
        return !cards.isEmpty() && cards.get(0).isDisplayed();
    }

    public RecordCard waitUntilDisplayed() {
        visible(card);
        return this;
    }

    public RecordDialog openEditDialog() {
        clickInCard(EDIT_BUTTON);
        return new RecordDialog(driver).waitUntilOpen();
    }

    public DeleteRecordDialog openDeleteDialog() {
        clickInCard(DELETE_BUTTON);
        return new DeleteRecordDialog(driver).waitUntilOpen();
    }

    /**
     * Reads every value shown on the card into the canonical model, ready to be compared.
     * Runs inside a wait, so a re-render of the list in the middle of reading is simply retried.
     */
    public PersonRecord read() {
        return wait.until(d -> {
            WebElement root = d.findElement(card);
            String[] fullName = labelled(root, FULL_NAME).split(" ", 2);
            return new PersonRecord()
                    .setRelationship(labelled(root, RELATIONSHIP))
                    .setFirstName(fullName[0])
                    .setLastName(fullName.length > 1 ? fullName[1] : null)
                    .setDob(LocalDate.parse(labelled(root, DOB), CARD_DATE))
                    .setGender(labelled(root, GENDER))
                    .setTelephone(optional(root, TELEPHONE, true))
                    .setHispanic(labelled(root, HISPANIC))
                    .setHispanicOther(optional(root, HISPANIC_OTHER, false))
                    .setRace(labelled(root, RACE))
                    .setRaceOther(optional(root, RACE_OTHER, false))
                    .setOtherStay(labelled(root, OTHER_STAY));
        });
    }

    // --- helpers -------------------------------------------------------------

    private void clickInCard(By inner) {
        wait.until(d -> {
            WebElement button = d.findElement(card).findElement(inner);
            if (!button.isDisplayed() || !button.isEnabled()) {
                return false;
            }
            button.click();
            return true;
        });
    }

    /** Text of a line without its bold label: "Gender: MALE" -> "MALE". */
    private static String labelled(WebElement root, By line) {
        return stripLabel(root.findElement(line).getText());
    }

    /** Optional lines (telephone, "other" texts) are not rendered at all when they are empty. */
    private static String optional(WebElement root, By line, boolean hasLabel) {
        List<WebElement> found = root.findElements(line);
        if (found.isEmpty()) {
            return null;
        }
        String text = found.get(0).getText().trim();
        if (hasLabel) {
            text = stripLabel(text);
        }
        return text.isEmpty() ? null : text;
    }

    private static String stripLabel(String text) {
        return text.trim().replaceFirst("^[A-Za-z ]+:\\s*", "").trim();
    }
}
