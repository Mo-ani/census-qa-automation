@record @delete
Feature: Delete a person record
  As a census respondent
  I want to remove a person who no longer lives in my household
  So that the person is not counted here

  Background:
    Given a registered user with a household and a SELF record

  @ui @api @db
  Scenario: Delete a person on the UI and verify it is gone from the API and the database
    Given the household already has an OTHER_NON_RELATIVE record
    And the user is logged in to the Census app
    When the user deletes that record
    Then the success message "Successfully deleted record." is displayed
    And the person is no longer listed on the dashboard
    And the records API no longer returns the person
    And the database no longer contains the person

  @negative @ui @db
  Scenario: The user's own SELF record cannot be deleted
    Given the user is logged in to the Census app
    When the user tries to delete the SELF record
    Then the error message "Deleting SELF record is prohibited. You can only edit SELF record." is displayed
    And the SELF record is still listed on the dashboard
    And the database has exactly 1 SELF record for the user
