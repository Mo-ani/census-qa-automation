package com.census.qa.hooks;

import com.census.qa.config.Config;
import com.census.qa.context.TestContext;
import com.census.qa.model.PersonRecord;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.restassured.response.Response;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Scenario life cycle: evidence (screenshot), browser shutdown and test-data clean-up.
 * Runs for every scenario, whether it passed or failed, so one failure never affects the next test.
 */
public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);

    private final TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    @Before
    public void startScenario(Scenario scenario) {
        context.setScenario(scenario);
        LOG.info("START  {} {}", scenario.getName(), scenario.getSourceTagNames());
    }

    /** Higher order runs first: take the screenshot while the browser still shows the final state. */
    @After(order = 300)
    public void attachScreenshot(Scenario scenario) {
        if (!context.hasDriver()) {
            return;
        }
        boolean always = "always".equalsIgnoreCase(Config.get("screenshot.mode"));
        if (scenario.isFailed() || always) {
            try {
                byte[] png = ((TakesScreenshot) context.driver()).getScreenshotAs(OutputType.BYTES);
                scenario.attach(png, "image/png", scenario.isFailed() ? "Failure screenshot" : "Final screenshot");
            } catch (RuntimeException e) {
                LOG.warn("Could not take a screenshot: {}", e.getMessage());
            }
        }
    }

    @After(order = 200)
    public void closeBrowser() {
        context.quitDriver();
    }

    /**
     * Deletes the people this scenario created, through the application's own API, so the
     * suite can be run again and again against the same database. Switch it off with
     * cleanup.created.records=false to inspect the rows after a run.
     */
    @After(order = 100)
    public void removeCreatedRecords(Scenario scenario) {
        try {
            if (Config.getBoolean("cleanup.created.records")) {
                Set<Integer> ids = new LinkedHashSet<>(context.createdRecordIds());
                for (String[] name : context.createdNames()) {
                    for (PersonRecord found : context.records()
                            .findByUserAndName(context.user().getEmail(), name[0], name[1])) {
                        if (!"SELF".equals(found.getRelationship())) {
                            ids.add(found.getId());
                        }
                    }
                }
                if (!ids.isEmpty()) {
                    context.api().login(context.user().getEmail(), context.user().getPassword());
                    for (Integer id : ids) {
                        if (context.records().countRecordsById(id) > 0) {
                            Response response = context.api().deleteRecord(id);
                            LOG.info("Clean-up: deleted record {} -> HTTP {}", id, response.statusCode());
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("Clean-up failed (the scenario result is not affected): {}", e.getMessage());
        } finally {
            context.db().close();
            LOG.info("END    {} -> {}", scenario.getName(), scenario.getStatus());
        }
    }
}
