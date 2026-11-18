package com.census.qa.steps;

import com.census.qa.context.TestContext;
import com.census.qa.model.PersonRecord;
import com.census.qa.utils.PersonStorage;
import io.cucumber.java.en.Then;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Database steps: verify with JDBC that the data was really persisted (or removed). */
public class RecordDbSteps {

    private final TestContext context;

    public RecordDbSteps(TestContext context) {
        this.context = context;
    }

    @Then("the database contains the person with the submitted details")
    public void theDatabaseContainsThePersonWithTheSubmittedDetails() {
        PersonRecord expected = context.person();
        String email = context.user().getEmail();

        List<PersonRecord> rows = context.records()
                .findByUserAndName(email, expected.getFirstName(), expected.getLastName());
        context.attach("Database rows (Record + Relative)", PersonStorage.toJson(
                context.records().findRowsByUserAndName(email, expected.getFirstName(), expected.getLastName())));

        assertThat(rows)
                .as("rows for %s linked to user %s", expected.fullName(), email)
                .hasSize(1);
        PersonRecord actual = rows.get(0);

        assertThat(actual)
                .as("database row of %s", expected.fullName())
                .usingRecursiveComparison()
                .ignoringFields("id", "householdId")
                .isEqualTo(expected);
        assertThat(actual.getHouseholdId())
                .as("\"Record\".\"householdId\" equals the household of user %s", email)
                .isEqualTo(context.records().householdIdOfUser(email));
        if (expected.getId() != null) {
            assertThat(actual.getId())
                    .as("same record id in the API and in the database")
                    .isEqualTo(expected.getId());
        }
        context.rememberForCleanup(actual.getId());
    }

    @Then("the database no longer contains the person")
    public void theDatabaseNoLongerContainsThePerson() {
        int id = context.person().getId();

        assertThat(context.records().countRecordsById(id))
                .as("rows in \"Record\" with id %s", id)
                .isZero();
        assertThat(context.records().countRelativesByRecordId(id))
                .as("rows in \"Relative\" for record %s (must be removed by ON DELETE CASCADE)", id)
                .isZero();
    }

    @Then("no person record is saved in the database")
    public void noPersonRecordIsSavedInTheDatabase() {
        PersonRecord person = context.person();

        assertThat(context.records().countRecordsByFirstOrLastName(person.getFirstName(), person.getLastName()))
                .as("rows in \"Record\" named %s or %s", person.getFirstName(), person.getLastName())
                .isZero();
    }

    @Then("the database has exactly {int} {word} record for the user")
    public void theDatabaseHasExactlyRecordsForTheUser(int count, String relationship) {
        assertThat(context.records().findByUserAndRelationship(context.user().getEmail(), relationship))
                .as("%s records of user %s", relationship, context.user().getEmail())
                .hasSize(count);
    }
}
