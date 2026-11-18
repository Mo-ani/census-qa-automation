# Census App – QA Automation Framework

A BDD test automation framework for the **Census** QA challenge application. It verifies one
business flow – *a person is added to a household* – on all three layers of the system:

| Layer    | Tool                    | What is verified                                             |
| -------- | ----------------------- | ------------------------------------------------------------ |
| UI       | Selenium WebDriver 4    | The person is added, updated and deleted in the browser      |
| API      | REST Assured            | The REST API returns exactly the values submitted on the UI  |
| Database | JDBC (PostgreSQL)       | The rows in `"Record"` and `"Relative"` hold the same values |

Scenarios are written in Gherkin (Cucumber 7, JUnit 4 runner) and built with Maven.
Language level: Java 25 (runs on any JDK 25 or newer).

---

## 1. Acceptance criteria → tests

| # | Acceptance criterion                                    | Covered by                                                                                   |
| - | ------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| 1 | Add a new person record to a household on the UI        | `add_record.feature` – Scenario Outline, 4 data rows                                         |
|   | *Optional:* save the added person to storage as a POJO  | Step *"the submitted person data is saved to storage"* → `target/test-data/<name>.json`      |
| 2 | Added person is returned by the API with correct values | Step *"the records API returns the person with the submitted details"*                       |
| 3 | Added person is found in the database with correct data | Step *"the database contains the person with the submitted details"*                         |
| 4 | *Bonus:* update and delete records on the UI            | `update_record.feature`, `delete_record.feature`                                             |
| + | Negative and API-only checks                            | `record_validation.feature`, `records_api.feature`, SELF record cannot be deleted/duplicated |

18 scenarios in total; the full suite takes one to two minutes.

## 2. Project structure

```
census-qa-automation
├── pom.xml                         dependencies, Surefire, HTML report plugin
├── mvnw / mvnw.cmd                 Maven Wrapper – no Maven installation needed
├── docker/docker-compose.yml       copy of the compose file that starts the app under test
├── postman/                        Postman collection with the API requests
└── src
    ├── main/java/com/census/qa     THE FRAMEWORK (reusable, knows nothing about Cucumber)
    │   ├── config/Config           configuration.properties + -D / environment overrides
    │   ├── driver/DriverFactory    Chrome / Edge / Firefox, headless, time zone
    │   ├── pages/                  page objects: LoginPage, DashboardPage and the
    │   │                           components RecordCard, RecordDialog, DeleteRecordDialog
    │   ├── api/CensusApi           REST Assured client (cookie session like Postman)
    │   ├── db/                     DbClient (JDBC) and RecordRepository (the SQL)
    │   ├── model/                  PersonRecord POJO, RecordMapper (UI/API/DB → POJO), TestUser
    │   └── utils/                  TestDataFactory (FAKE data), PersonStorage (POJO ⇄ JSON file)
    └── test
        ├── java/com/census/qa      THE TESTS
        │   ├── runners/TestRunner  JUnit entry point (@CucumberOptions)
        │   ├── steps/              PreconditionSteps, RecordUiSteps, RecordApiSteps, RecordDbSteps
        │   ├── hooks/Hooks         screenshot, browser shutdown, test-data clean-up
        │   └── context/TestContext per-scenario shared state (dependency injection)
        └── resources
            ├── features/           the Gherkin feature files
            └── configuration.properties
```

## 3. Running the tests

### Prerequisites

- **JDK 25+**
- **Docker Desktop** running (for the application under test)
- **Google Chrome** (default), Microsoft Edge or Firefox – set `browser` in the configuration.
  The browser driver is downloaded automatically by Selenium Manager.
  The full suite was run on Chrome; the end-to-end "add" scenario also on Edge and Firefox.

### Start the application

```bash
cd docker
docker compose up -d
```

Wait a few minutes for the first build, then check http://localhost:3000.
`docker compose ps` must list four running containers: `postgres`, `census_app`, `pgadmin`
and `swagger_ui`.

If it does not start:

- *"Docker Desktop is unable to start"* – the engine is not running. Docker Desktop needs WSL 2
  (`wsl --install` in an administrator PowerShell, then restart Windows); afterwards restart
  Docker Desktop (`docker desktop restart`) and wait until `docker info` answers.
- *The build stops at `npm ci`* – the image is built from GitHub and downloads its packages
  during the build; a network hiccup fails it. Run `docker compose up -d` again.
- *Port already in use* – something else listens on 3000 or 5432 (for example a locally
  installed PostgreSQL). Stop it, then start the stack again.
Swagger UI: http://localhost:5051 · pgAdmin: http://localhost:5050 (`admin@admin.com` / `admin`).

### Run

Windows (`cmd` or PowerShell):

```bash
.\mvnw.cmd clean test
```

macOS / Linux:

```bash
./mvnw clean test
```

Useful variations (quotes make the commands work in PowerShell too):

```bash
.\mvnw.cmd clean test "-Dcucumber.filter.tags=@smoke"
.\mvnw.cmd clean test "-Dcucumber.filter.tags=@api and not @ui"
.\mvnw.cmd clean test "-Dheadless=true"
.\mvnw.cmd clean test "-Dbrowser=edge"
.\mvnw.cmd clean test "-Dcleanup.created.records=false"
```

From an IDE: import as *Existing Maven Project*, then run
`src/test/java/com/census/qa/runners/TestRunner.java` as a JUnit test.

Tags: `@smoke` `@record` `@add` `@update` `@delete` `@validation` `@negative` `@ui` `@api` `@db`.

**No manual test-data setup is needed.** The challenge README asks to register a user, add a
household and add a SELF record by hand. The first step of every feature
(*"a registered user with a household and a SELF record"*) does this through the API, and only
if it is missing – so the suite also runs against a brand-new database (`docker compose down -v`).
To use an account you created by hand, put its e-mail and password into `configuration.properties`.

### Reports

| File                                               | Content                                                 |
| -------------------------------------------------- | ------------------------------------------------------- |
| `target/cucumber-html-reports/overview-features.html` | Rich HTML report (features, tags, steps, failures)   |
| `target/cucumber-reports/cucumber.html`            | Cucumber's built-in single-page report                  |
| `target/cucumber-reports/cucumber.json` / `.xml`   | Machine-readable results for CI                         |
| `target/test-data/*.json`                          | The person data each "add" scenario submitted           |

Each scenario's report entry contains its evidence: a screenshot of the final page, the stored
POJO, the API responses and the database row.

`mvnw test` fails the build when a scenario fails. (Surefire itself is set to
`testFailureIgnore` so that the HTML report is always generated; the report plugin then fails
the build through `checkBuildResult`.)

### Postman

`postman/Census-App.postman_collection.json` contains the API requests of the challenge
(login, records by e-mail, records by query parameter) plus the calls needed around them.
Import it in Postman (*Import → File*), then send **1. Auth / Login** first: the session cookie
is stored by Postman and sent with all later requests. The collection can also be run top to
bottom with the Collection Runner; it cleans up the person it adds. E-mail, password and base
URL are collection variables.

## 4. Configuration

`src/test/resources/configuration.properties` holds every environment-specific value: base URL,
browser, timeouts, test user, database connection. Nothing is hard-coded in the tests.

Lookup order for a key such as `base.url`:

1. JVM system property – `-Dbase.url=http://host:3000`
2. Environment variable – `BASE_URL`
3. The properties file

So the same build runs locally and in a pipeline without editing files, and real credentials
never need to be committed.

## 5. Design decisions

**One model, three adapters.** A person is form fields on the UI, JSON in the API and two table
rows in the database. `RecordMapper` converts each of them into the same `PersonRecord` POJO, and
the tests compare whole objects with AssertJ's recursive comparison
(`usingRecursiveComparison().ignoringFields("id", "householdId")`). A failure names the exact
field that differs, and adding a field to the application means changing one mapper, not every
assertion. The differences between the layers are normalised in that one class:
empty text vs `null`, the date of birth as a calendar date vs a UTC timestamp, and the
space-padded `CHAR(10)` telephone column.

**Framework and tests are separated.** `src/main` contains the reusable framework and has no
dependency on Cucumber; `src/test` contains only steps, hooks and features. Step definitions say
*what* happens, page objects know *how*.

**Page objects and component objects.** The dashboard is split into small components
(`RecordCard`, `RecordDialog`, `DeleteRecordDialog`). A card is located by business data (the
person's full name), never by its position. The same `RecordDialog` serves "add" and "update",
because the application uses one form for both.

**No `Thread.sleep`, no implicit wait, no cached elements.** The app is a React single-page
application that re-renders constantly. Every interaction looks the element up again and waits
explicitly for the state it needs; stale elements are retried by the wait.

**Custom widgets are handled inside the page object.** The drop-downs are Radix UI components
(a button plus a floating list), not `<select>` elements, so Selenium's `Select` class cannot be
used. The date of birth can only be picked in a calendar whose month/year selects are hidden
behind a label (opacity 0). Both are wrapped in one helper method each, so the steps stay simple.

**The suite does not depend on the screen it runs on.** The record dialog of the application
cannot be scrolled, so on a small or scaled laptop screen its buttons fall outside a visible
browser window (finding 12). `DriverFactory` detects a viewport lower than `min.viewport.height`
and zooms the page out just enough to fit – what a user would do with Ctrl and minus. Headless
runs always get the configured window size.

**Dependency injection instead of static state.** PicoContainer gives every scenario its own
`TestContext` (browser, API session, DB connection, the person under test). Scenarios cannot
leak state into each other, the browser is only started by scenarios that use it (API-only
scenarios run without one), and the design is ready for parallel execution.

**Test data.**
- Preconditions are created through the API, idempotently (fast and stable).
- Every person gets a unique name – `FAKE` plus random letters – so runs never collide and a
  person can be found unambiguously on all layers. No real personal data is used.
- Update and delete scenarios seed their record through the API, so they do not depend on the
  "add" UI flow working.
- The `After` hook deletes what a scenario created, through the application's API. The database
  is only ever **read** by the tests (the JDBC connection is read-only): writing test data
  straight into tables would bypass the application's rules.

**Database checks use parameterised SQL** (`PreparedStatement`) and verify more than the row
itself: the record must belong to the *household of the logged-in user*, the relationship must be
in `"Relative"`, and after a delete both rows must be gone (the `ON DELETE CASCADE`).

**Deterministic dates.** The browser's time zone is pinned to the server's (`browser.timezone`),
because the dashboard renders the date of birth in the browser's zone – see finding 1 below.
Chrome and Edge are pinned through the DevTools protocol, Firefox through its `TZ` environment
variable. The pin is a deliberate choice to keep the suite stable on any machine; the defect
itself is documented and reproducible with one command.

## 6. Database notes

The schema is created by Prisma, so table and column names are case-sensitive and must be quoted.

```
"User" (id, email, "householdId")        1 ── 1  "Household" (id, address1, city, state, zip, ...)
"Record" (id, "firstName", "lastName", dob, gender, telephone, hispanic, race, "otherStay", "householdId")
"Relative" (id, "userId", "recordId", relationship)
```

A person added on the UI creates **two** rows: the person in `"Record"` (linked to the
household) and the relationship to the user in `"Relative"`. The query used by the tests:

```sql
SELECT r.*, rel.relationship, u.email
  FROM "Record" r
  JOIN "Relative" rel ON rel."recordId" = r.id
  JOIN "User" u       ON u.id = rel."userId"
 WHERE u.email = 'fake.qa.user@example.com'
 ORDER BY r.id;
```

## 7. Findings in the application under test

Found while building the framework; each one was reproduced against the running application.

| #  | Area     | Finding |
| -- | -------- | ------- |
| 1  | UI       | **Date of birth is shown one day early.** A person saved with `03/15/1990` is listed as `3/14/1990` when the browser is in a time zone behind UTC (e.g. US Eastern): the server stores midnight UTC and the card formats it in the browser's zone. Reproduce: `.\mvnw.cmd clean test "-Dcucumber.filter.tags=@smoke" "-Dbrowser.timezone=America/New_York"`. |
| 2  | UI       | The date-of-birth field cannot be typed into – it is rendered as `<input type="button">` and only opens the calendar. The validation message "Date has to be in MM/dd/yyyy format." can therefore never appear. |
| 3  | UI       | The calendar's year list offers only the last 100 years (1926–2026), although the form is coded to accept dates back to 1900. |
| 4  | API / DB | `telephone` is a `CHAR(10)` column: a valid 5–9 digit number is stored and returned padded with spaces (`"12345     "`). |
| 5  | API      | No server-side range check for `dob`: `PUT /api/record/relative` accepts `01/20/1850` and `01/20/2090`, which the UI calendar does not allow. |
| 6  | API      | A first name longer than 13 characters passes validation and then fails in the database: HTTP 500, and the raw Prisma error is returned in `db_error`. The form has no maximum length either. |
| 7  | API docs | `POST /api/auth/register` requires `image`, but Swagger lists only name, e-mail and password; a duplicate e-mail returns 403 (Swagger: 409); a successful `DELETE` returns 201. |
| 8  | Docs     | The session cookie expires after 2 hours; the challenge documentation says 4. The PDF lists the API as `http://localhost/api/...`, the app listens on port 3000. |
| 9  | Security | Login tells whether an e-mail is registered ("Email does not exist!" vs "Invalid credentials!"). |
| 10 | Cosmetic | Typos: "Welcome bak!" on the login page; "permission to ged someone's household" in an API error. |
| 11 | API      | **Data integrity:** `PUT /api/household` for a user who already has a household creates a second one and re-points the user to it. The user's existing records are then no longer returned (404 "No records found"), and a new SELF record cannot be added either ("You already have SELF record."). The UI avoids this by updating instead; the API does not guard against it. |
| 12 | UI       | The add/edit dialog cannot be scrolled. In a browser window lower than about 750 px (a 1280×720 laptop screen, or any screen with display scaling) the title and the Cancel/Update buttons are outside the visible page and cannot be reached with the mouse. |

## 8. Possible next steps

- Parallel execution (the per-scenario context already isolates browser, session and data).
- A CI pipeline: `docker compose up -d`, wait for the health check, `./mvnw clean test -Dheadless=true`, publish `target/cucumber-html-reports`.
- JSON-schema validation of the API responses against `openapi.yaml`.
- Household create/update scenarios and more boundary-value tests (name lengths, telephone 5–10 digits).
- Run the whole suite on Edge and Firefox in the pipeline (`-Dbrowser=edge|firefox`). The automatic
  zoom-to-fit for short windows is Chrome/Edge only; run Firefox headless or on a larger screen.
