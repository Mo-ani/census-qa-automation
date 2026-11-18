package com.census.qa.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Entry point of the suite.
 *
 * <ul>
 *   <li>IDE: right-click this class - Run As - JUnit Test</li>
 *   <li>Maven: {@code mvn clean test}</li>
 *   <li>Subset by tag: {@code mvn clean test -Dcucumber.filter.tags="@smoke"}</li>
 * </ul>
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.census.qa.steps", "com.census.qa.hooks"},
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json",
                "junit:target/cucumber-reports/cucumber.xml",
                "rerun:target/rerun.txt"
        },
        tags = "not @wip",
        monochrome = true
)
public class TestRunner {
}
