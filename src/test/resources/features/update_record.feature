@record @update
Feature: Update a person record
  As a census respondent
  I want to correct the data of a person in my household
  So that the census holds accurate information

  Background:
    Given a registered user with a household and a SELF record

  # The record to edit is created through the API (fast, independent of the "add" UI flow);
  # the update itself is done on the UI and verified on all three layers.
  @ui @api @db
  Scenario: Update an existing person on the UI and verify it in the API and the database
    Given the household already has a PARENT record
    And the user is logged in to the Census app
    When the user updates that record with:
      | relationship  | OTHER_RELATIVE |
      | lastName      | FAKEUpd{random} |
      | dob           | 06/21/1962     |
      | gender        | MALE           |
      | telephone     | 5715550166     |
      | hispanic      | OTHER          |
      | hispanicOther | FAKEHispanic   |
      | race          | KOREAN         |
      | otherStay     | JOB_OR_BUSINESS |
    Then the success message "Successfully updated record for 'OTHER_RELATIVE'" is displayed
    And the person is listed on the dashboard with the submitted details
    And the records API returns the person with the submitted details
    And the database contains the person with the submitted details
