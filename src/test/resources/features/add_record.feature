@record @add
Feature: Add a person record to the household
  As a census respondent
  I want to add the people who live in my household
  So that every person is counted with the correct demographic data

  Background:
    Given a registered user with a household and a SELF record

  # One scenario covers acceptance criteria 1-3: the person is added on the UI, then the same
  # submitted values are verified on the dashboard, in the REST API and in the database.
  # First and last name are generated per run (FAKE + random letters), so the data is unique.
  @smoke @ui @api @db
  Scenario Outline: Add a <relationship> on the UI and verify it in the API and the database
    Given the user is logged in to the Census app
    When the user adds a new person record with:
      | relationship  | <relationship>  |
      | dob           | <dob>           |
      | gender        | <gender>        |
      | telephone     | <telephone>     |
      | hispanic      | <hispanic>      |
      | hispanicOther | <hispanicOther> |
      | race          | <race>          |
      | raceOther     | <raceOther>     |
      | otherStay     | <otherStay>     |
    Then the success message "Successfully saved new record for '<relationship>'" is displayed
    And the person is listed on the dashboard with the submitted details
    And the submitted person data is saved to storage
    And the records API returns the person with the submitted details
    And the database contains the person with the submitted details

    Examples: typical household members
      | relationship       | dob        | gender | telephone  | hispanic     | hispanicOther | race         | raceOther | otherStay |
      | SPOUSE             | 03/15/1990 | FEMALE | 2025550147 | NO           |               | WHITE        |           | NO        |
      | BIOLOGICAL_CHILD   | 11/02/2015 | MALE   |            | MEXICAN      |               | ASIAN_INDIAN |           | COLLEGE   |

    Examples: free-text "other" answers and the last option of each list
      | relationship       | dob        | gender | telephone  | hispanic     | hispanicOther | race         | raceOther | otherStay |
      | ROOMMATE_HOUSEMATE | 12/31/1975 | MALE   | 3015550123 | OTHER        | FAKEHispanic  | OTHER        | FAKERace  | OTHER     |
      | GRANDPARENT        | 02/29/1948 | FEMALE | 7035550188 | NO_ANSWER    |               | NO_ANSWER    |           | NURSING_HOME |
