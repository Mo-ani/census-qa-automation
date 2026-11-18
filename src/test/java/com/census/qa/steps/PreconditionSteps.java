package com.census.qa.steps;

import com.census.qa.api.CensusApi;
import com.census.qa.context.TestContext;
import com.census.qa.model.PersonRecord;
import com.census.qa.model.TestUser;
import com.census.qa.utils.TestDataFactory;
import io.cucumber.java.en.Given;
import io.restassured.response.Response;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-data preconditions, created through the API because that is fast and reliable.
 *
 * <p>The challenge README asks to register a user, add a household and add a SELF record by
 * hand before running the tests. This step does the same automatically and only when something
 * is missing (it is idempotent), so the suite also runs against a brand-new, empty database.
 */
public class PreconditionSteps {

    private final TestContext context;

    public PreconditionSteps(TestContext context) {
        this.context = context;
    }

    @Given("a registered user with a household and a SELF record")
    public void aRegisteredUserWithAHouseholdAndASelfRecord() {
        TestUser user = context.user();
        CensusApi api = context.api();

        // 1. account
        Response login = api.login(user.getEmail(), user.getPassword());
        if (login.statusCode() != 200) {
            Response registration = api.register(user);
            assertThat(registration.statusCode())
                    .as("Could not log in as %s (%s) and could not register that account either (%s). "
                                    + "If the account already exists with another password, "
                                    + "set test.user.password in configuration.properties.",
                            user.getEmail(), login.asString(), registration.asString())
                    .isEqualTo(201);
            login = api.login(user.getEmail(), user.getPassword());
        }
        assertThat(login.statusCode()).as("API login: %s", login.asString()).isEqualTo(200);

        String userId = api.whoAmI().then().statusCode(200).extract().path("principal.id");
        user.setId(Integer.valueOf(userId));

        // 2. household
        Response household = api.getHouseholdByUserEmail(user.getEmail());
        if (household.statusCode() == 404) {
            household = api.createHousehold(TestDataFactory.household(user.getId()));
            assertThat(household.statusCode()).as("create household: %s", household.asString()).isEqualTo(201);
        }
        user.setHouseholdId(household.then().extract().path("household.id"));

        // 3. the user's own (SELF) record
        Response records = api.getRecordsWithRelationship(user.getEmail());
        List<String> relationships = records.statusCode() == 200
                ? records.jsonPath().getList("recordsWithRelationship.relative.relationship", String.class)
                : List.of();
        if (!relationships.contains("SELF")) {
            Response self = api.createRecord(TestDataFactory.selfRecord(), user.getId());
            assertThat(self.statusCode()).as("create SELF record: %s", self.asString()).isEqualTo(201);
        }
    }

    /** Seeds a person through the API so update/delete scenarios do not depend on the "add" UI flow. */
    @Given("the household already has a/an {word} record")
    public void aRecordExistsInTheHousehold(String relationship) {
        PersonRecord person = TestDataFactory.validPerson(relationship);
        Response response = context.api().createRecord(person, context.user().getId());
        assertThat(response.statusCode()).as("seed %s record: %s", relationship, response.asString()).isEqualTo(201);

        Integer id = response.then().extract().path("recordWithRelationship.record.id");
        person.setId(id);
        person.setHouseholdId(context.user().getHouseholdId());
        context.setPerson(person);
        context.rememberForCleanup(id);
    }
}
