@api
Feature: Records REST API
  The documented endpoints of the challenge, tested without a browser:
    POST /api/auth/login
    GET  /api/record/user/email/[email]
    GET  /api/record/user?email=...|id=...

  @negative
  Scenario: Records cannot be read without logging in
    When an anonymous client requests the records of the test user
    Then the API responds with status 401
    And the API error message is "Your token has expired or you aren't logged in!"

  @smoke @db
  Scenario: Records are returned for the user's e-mail in the path
    Given a registered user with a household and a SELF record
    When the client requests the user's records by the email path
    Then the API responds with status 200
    And the response contains the user's SELF record

  @db
  Scenario Outline: Records are returned for the query parameter <parameter>
    Given a registered user with a household and a SELF record
    When the client requests the user's records by query parameter <parameter>
    Then the API responds with status 200
    And the response contains the user's SELF record

    Examples:
      | parameter |
      | email     |
      | id        |

  @negative
  Scenario: The query endpoint requires at least one query parameter
    Given a registered user with a household and a SELF record
    When the client requests records without a query parameter
    Then the API responds with status 403
    And the API error message is "At least one query parameter required, id or email"

  @negative
  Scenario: A user cannot read the records of another user
    Given a registered user with a household and a SELF record
    When the client requests the records of another user "someone.else.fake@example.com"
    Then the API responds with status 401
    And the API error message contains "permission"
