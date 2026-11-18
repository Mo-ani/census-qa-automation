package com.census.qa.context;

import com.census.qa.api.CensusApi;
import com.census.qa.db.DbClient;
import com.census.qa.db.RecordRepository;
import com.census.qa.driver.DriverFactory;
import com.census.qa.model.PersonRecord;
import com.census.qa.model.TestUser;
import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Everything one scenario needs to share between its step classes.
 *
 * <p>Cucumber (through PicoContainer) creates ONE new instance per scenario and passes it to
 * the constructor of every step and hook class. That replaces static variables: scenarios
 * cannot leak state into each other, and the browser, API session and database connection
 * all have the same life span as the scenario.
 *
 * <p>The browser and the database connection are opened lazily, so an API-only scenario
 * never starts a browser.
 */
public class TestContext {

    private final TestUser user = TestUser.fromConfig();
    private final CensusApi api = new CensusApi();
    private final DbClient db = new DbClient();
    private final RecordRepository records = new RecordRepository(db);
    /** Ids of records this scenario created; the After hook deletes them again. */
    private final Set<Integer> createdRecordIds = new LinkedHashSet<>();
    /** Names of people this scenario tried to create on the UI (their ids are not known there). */
    private final Set<String[]> createdNames = new LinkedHashSet<>();

    private WebDriver driver;
    private Scenario scenario;

    /** The person the scenario works with - the single source of truth for all three layers. */
    private PersonRecord person;
    /** Where the submitted person was stored as JSON. */
    private Path storedPersonFile;

    public WebDriver driver() {
        if (driver == null) {
            driver = DriverFactory.create();
        }
        return driver;
    }

    public boolean hasDriver() {
        return driver != null;
    }

    public void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    public TestUser user() {
        return user;
    }

    public CensusApi api() {
        return api;
    }

    public DbClient db() {
        return db;
    }

    public RecordRepository records() {
        return records;
    }

    public Scenario scenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public PersonRecord person() {
        if (person == null) {
            throw new IllegalStateException("No person has been created in this scenario yet");
        }
        return person;
    }

    public void setPerson(PersonRecord person) {
        this.person = person;
        rememberForCleanup(person.getFirstName(), person.getLastName());
    }

    public Path storedPersonFile() {
        return storedPersonFile;
    }

    public void setStoredPersonFile(Path storedPersonFile) {
        this.storedPersonFile = storedPersonFile;
    }

    public void rememberForCleanup(int recordId) {
        createdRecordIds.add(recordId);
    }

    public void rememberForCleanup(String firstName, String lastName) {
        createdNames.add(new String[] {firstName, lastName});
    }

    public Set<Integer> createdRecordIds() {
        return createdRecordIds;
    }

    public Set<String[]> createdNames() {
        return createdNames;
    }

    /** Adds text (an API response, a database row, ...) to the Cucumber report of this scenario. */
    public void attach(String title, String text) {
        if (scenario != null) {
            scenario.attach(text, "text/plain", title);
        }
    }
}
