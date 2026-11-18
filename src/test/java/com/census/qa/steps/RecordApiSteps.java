package com.census.qa.steps;

import com.census.qa.api.CensusApi;
import com.census.qa.context.TestContext;
import com.census.qa.model.PersonRecord;
import com.census.qa.model.RecordMapper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** API steps: verify through the REST API what was done on the UI, plus a few API-only checks. */
public class RecordApiSteps {

    private final TestContext context;

    /** Response of the last API-only request, checked by the generic "Then" steps below. */
    private Response lastResponse;

    public RecordApiSteps(TestContext context) {
        this.context = context;
    }

    // --- cross-layer verification of a person ------------------------------------

    @Then("the records API returns the person with the submitted details")
    public void theRecordsApiReturnsThePersonWithTheSubmittedDetails() {
        PersonRecord expected = context.person();
        String email = context.user().getEmail();

        // Documented endpoint: GET /api/record/user/email/[email]
        Response response = context.api().getRecordsByUserEmail(email);
        context.attach("GET /api/record/user/email/" + email, response.asPrettyString());
        assertThat(response.statusCode()).as("status of GET records by e-mail").isEqualTo(200);

        List<PersonRecord> matches = findInRecords(response, expected);
        assertThat(matches)
                .as("records named %s returned by the API", expected.fullName())
                .hasSize(1);
        PersonRecord actual = matches.get(0);

        // That endpoint has no relationship; it is exposed by GET /api/record/relative
        actual.setRelationship(relationshipOf(actual.getId(), email));

        assertThat(actual)
                .as("API data of %s", expected.fullName())
                .usingRecursiveComparison()
                .ignoringFields("id", "householdId")
                .isEqualTo(expected);
        assertThat(actual.getHouseholdId())
                .as("the person is saved under the household of the logged-in user")
                .isEqualTo(context.user().getHouseholdId());

        // The id is only known from now on - keep it for the database check and the clean-up
        expected.setId(actual.getId());
        context.rememberForCleanup(actual.getId());
    }

    @Then("the records API no longer returns the person")
    public void theRecordsApiNoLongerReturnsThePerson() {
        Response response = context.api().getRecordsByUserEmail(context.user().getEmail());
        context.attach("GET records after delete", response.asPrettyString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(findInRecords(response, context.person()))
                .as("records named %s returned by the API", context.person().fullName())
                .isEmpty();
    }

    // --- API-only scenarios ------------------------------------------------------

    @When("an anonymous client requests the records of the test user")
    public void anAnonymousClientRequestsTheRecordsOfTheTestUser() {
        lastResponse = new CensusApi().getRecordsByUserEmail(context.user().getEmail());
    }

    @When("the client requests the user's records by the email path")
    public void theClientRequestsTheUsersRecordsByTheEmailPath() {
        lastResponse = context.api().getRecordsByUserEmail(context.user().getEmail());
    }

    @When("the client requests the user's records by query parameter {word}")
    public void theClientRequestsTheUsersRecordsByQueryParameter(String parameter) {
        Object value = switch (parameter) {
            case "email" -> context.user().getEmail();
            case "id" -> context.user().getId();
            default -> throw new IllegalArgumentException("Unsupported query parameter: " + parameter);
        };
        lastResponse = context.api().getRecordsByQuery(parameter, value);
    }

    @When("the client requests records without a query parameter")
    public void theClientRequestsRecordsWithoutAQueryParameter() {
        lastResponse = context.api().getRecordsWithoutQuery();
    }

    @When("the client requests the records of another user {string}")
    public void theClientRequestsTheRecordsOfAnotherUser(String email) {
        lastResponse = context.api().getRecordsByUserEmail(email);
    }

    @Then("the API responds with status {int}")
    public void theApiRespondsWithStatus(int status) {
        context.attach("API response", lastResponse.asPrettyString());
        assertThat(lastResponse.statusCode()).as("body: %s", lastResponse.asString()).isEqualTo(status);
    }

    @Then("the API error message is {string}")
    public void theApiErrorMessageIs(String message) {
        assertThat(lastResponse.jsonPath().getString("error")).isEqualTo(message);
    }

    @Then("the API error message contains {string}")
    public void theApiErrorMessageContains(String text) {
        assertThat(lastResponse.jsonPath().getString("error")).contains(text);
    }

    @Then("the response contains the user's {word} record")
    public void theResponseContainsTheUsersRecord(String relationship) {
        List<PersonRecord> inDatabase = context.records()
                .findByUserAndRelationship(context.user().getEmail(), relationship);
        assertThat(inDatabase).as("%s records of the user in the database", relationship).isNotEmpty();

        List<Integer> returnedIds = lastResponse.jsonPath().getList("records.id", Integer.class);
        assertThat(returnedIds).contains(inDatabase.get(0).getId());
    }

    // --- helpers -------------------------------------------------------------------

    private static List<PersonRecord> findInRecords(Response response, PersonRecord person) {
        List<Map<String, Object>> records = response.jsonPath().getList("records");
        return records.stream()
                .map(RecordMapper::fromApi)
                .filter(record -> person.getFirstName().equals(record.getFirstName())
                        && person.getLastName().equals(record.getLastName()))
                .collect(Collectors.toList());
    }

    private String relationshipOf(Integer recordId, String email) {
        Response response = context.api().getRecordsWithRelationship(email);
        context.attach("GET /api/record/relative?email=" + email, response.asPrettyString());
        assertThat(response.statusCode()).as("status of GET records with relationship").isEqualTo(200);

        List<Map<String, Map<String, Object>>> entries = response.jsonPath().getList("recordsWithRelationship");
        return entries.stream()
                .filter(entry -> recordId.equals(((Number) entry.get("record").get("id")).intValue()))
                .map(entry -> entry.get("relative") == null ? null : (String) entry.get("relative").get("relationship"))
                .findFirst()
                .orElse(null);
    }
}
