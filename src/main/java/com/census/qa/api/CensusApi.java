package com.census.qa.api;

import com.census.qa.config.Config;
import com.census.qa.model.PersonRecord;
import com.census.qa.model.RecordMapper;
import com.census.qa.model.TestUser;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for the Census REST API (see the Swagger UI on http://localhost:5051).
 *
 * <p>Authentication works exactly like in Postman: {@code POST /api/auth/login} answers with a
 * session cookie, and the {@link CookieFilter} of this instance stores it and sends it with
 * every later request. One instance therefore equals one logged-in API session.
 *
 * <p>The methods return the raw {@link Response}; checking status codes and bodies is the job
 * of the step definitions, so the same call can be reused for positive and negative tests.
 */
public class CensusApi {

    private final CookieFilter session = new CookieFilter();

    private RequestSpecification request() {
        return RestAssured.given()
                .baseUri(Config.baseUrl())
                .filter(session)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    // --- Auth ---------------------------------------------------------------------

    public Response register(TestUser user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", user.getName());
        body.put("email", user.getEmail());
        body.put("password", user.getPassword());
        body.put("image", "/account.svg");
        return request().body(body).post("/api/auth/register");
    }

    public Response login(String email, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        return request().body(body).post("/api/auth/login");
    }

    /** Tells who the current session belongs to; used to learn the id of the logged-in user. */
    public Response whoAmI() {
        return request().get("/api/auth/whoami");
    }

    // --- Household ----------------------------------------------------------------

    public Response getHouseholdByUserEmail(String email) {
        return request().queryParam("email", email).get("/api/household");
    }

    public Response createHousehold(Map<String, Object> household) {
        return request().body(household).put("/api/household");
    }

    // --- Records ------------------------------------------------------------------

    /** GET /api/record/user/email/[email] - endpoint 2 of the challenge documentation. */
    public Response getRecordsByUserEmail(String email) {
        return request().get("/api/record/user/email/" + email);
    }

    /** GET /api/record/user?email=... or ?id=... - endpoint 3 of the challenge documentation. */
    public Response getRecordsByQuery(String parameter, Object value) {
        return request().queryParam(parameter, value).get("/api/record/user");
    }

    /** GET /api/record/user without any query parameter (negative test). */
    public Response getRecordsWithoutQuery() {
        return request().get("/api/record/user");
    }

    /** Records together with the relationship of each person to the user. */
    public Response getRecordsWithRelationship(String email) {
        return request().queryParam("email", email).get("/api/record/relative");
    }

    /** Creates a person directly through the API - used to seed data for update/delete scenarios. */
    public Response createRecord(PersonRecord record, int userId) {
        return request().body(RecordMapper.toApiPayload(record, userId)).put("/api/record/relative");
    }

    public Response deleteRecord(int recordId) {
        return request().delete("/api/record/id/" + recordId);
    }
}
