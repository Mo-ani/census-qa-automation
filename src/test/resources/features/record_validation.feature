@record @validation @negative
Feature: Record form validation
  As a census respondent
  I want to be told when I enter invalid data
  So that only clean data is saved

  Background:
    Given a registered user with a household and a SELF record
    And the user is logged in to the Census app

  @ui @db
  Scenario Outline: A record with an invalid <field> is rejected and not saved
    When the user submits a new person record with <field> "<value>"
    Then the field validation message "<message>" is displayed
    And no person record is saved in the database

    Examples:
      | field     | value      | message                                |
      | firstName | FAKE J@ne  | Only alphabets and digits are allowed. |
      | lastName  | FAKE-Doe!  | Only alphabets and digits are allowed. |
      | dob       |            | Required                               |
      | telephone | 12ab       | must be 5 to 10 digits or empty        |

  @ui @db
  Scenario: A second SELF record cannot be added
    When the user adds a new person record with:
      | relationship | SELF |
    Then the error message "You already have SELF record." is displayed
    And the person is not listed on the dashboard
    And the database has exactly 1 SELF record for the user
