package com.census.qa.model;

import com.census.qa.config.Config;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Adapters that turn each layer's representation of a person into a {@link PersonRecord}.
 *
 * <pre>
 *   feature file / UI form  --fromTable-->
 *   API JSON                --fromApi---->   PersonRecord   (compared with AssertJ)
 *   database row            --fromDb----->
 * </pre>
 *
 * Normalisation rules live here, in one place:
 * <ul>
 *   <li>blank optional values ("" on the form, null in the API/DB) all become {@code null};</li>
 *   <li>"dob" is a calendar date on the form but a UTC timestamp in the API and DB - it is
 *       converted back to a date in the application server's time zone;</li>
 *   <li>"telephone" is a CHAR(10) column, so shorter numbers come back space-padded and are trimmed.</li>
 * </ul>
 */
public final class RecordMapper {

    /** Format the record form expects and the format used in the feature files. */
    public static final DateTimeFormatter FORM_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private RecordMapper() {
    }

    /** Builds a record from a vertical Cucumber data table ({@code | field | value |}). */
    public static PersonRecord fromTable(Map<String, String> row) {
        PersonRecord record = new PersonRecord();
        applyTable(record, row);
        return record;
    }

    /** Overwrites only the fields that are present in the data table (used for updates). */
    public static void applyTable(PersonRecord record, Map<String, String> row) {
        row.forEach((field, rawValue) -> {
            String value = blankToNull(rawValue);
            switch (field) {
                case "relationship" -> record.setRelationship(value);
                case "firstName" -> record.setFirstName(value);
                case "lastName" -> record.setLastName(value);
                case "dob" -> record.setDob(value == null ? null : LocalDate.parse(value, FORM_DATE));
                case "gender" -> record.setGender(value);
                case "telephone" -> record.setTelephone(value);
                case "hispanic" -> record.setHispanic(value);
                case "hispanicOther" -> record.setHispanicOther(value);
                case "race" -> record.setRace(value);
                case "raceOther" -> record.setRaceOther(value);
                case "otherStay" -> record.setOtherStay(value);
                default -> throw new IllegalArgumentException("Unknown person field in data table: '" + field + "'");
            }
        });
        // Business rule of the form: the free-text "other" fields only exist while OTHER is selected.
        if (!"OTHER".equals(record.getHispanic())) {
            record.setHispanicOther(null);
        }
        if (!"OTHER".equals(record.getRace())) {
            record.setRaceOther(null);
        }
    }

    /**
     * Builds a record from one element of the API's {@code records} array.
     * The relationship is not part of that object; callers add it from the "relative" object.
     */
    public static PersonRecord fromApi(Map<String, Object> json) {
        return new PersonRecord()
                .setId(toInteger(json.get("id")))
                .setHouseholdId(toInteger(json.get("householdId")))
                .setFirstName(text(json.get("firstName")))
                .setLastName(text(json.get("lastName")))
                .setDob(dateOf(Instant.parse(String.valueOf(json.get("dob")))))
                .setGender(text(json.get("gender")))
                .setTelephone(text(json.get("telephone")))
                .setHispanic(text(json.get("hispanic")))
                .setHispanicOther(text(json.get("hispanicOther")))
                .setRace(text(json.get("race")))
                .setRaceOther(text(json.get("raceOther")))
                .setOtherStay(text(json.get("otherStay")));
    }

    /** Builds a record from a row of the "Record" table joined with its "Relative" row. */
    public static PersonRecord fromDb(Map<String, Object> row) {
        return new PersonRecord()
                .setId(toInteger(row.get("id")))
                .setHouseholdId(toInteger(row.get("householdId")))
                .setRelationship(text(row.get("relationship")))
                .setFirstName(text(row.get("firstName")))
                .setLastName(text(row.get("lastName")))
                .setDob(dateOf(toUtcInstant(row.get("dob"))))
                .setGender(text(row.get("gender")))
                .setTelephone(text(row.get("telephone")))
                .setHispanic(text(row.get("hispanic")))
                .setHispanicOther(text(row.get("hispanicOther")))
                .setRace(text(row.get("race")))
                .setRaceOther(text(row.get("raceOther")))
                .setOtherStay(text(row.get("otherStay")));
    }

    /** JSON body the application expects when a record is created or updated through the API. */
    public static Map<String, Object> toApiPayload(PersonRecord record, int userId) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (record.getId() != null) {
            body.put("id", record.getId());
        }
        body.put("userId", userId);
        body.put("relationship", record.getRelationship());
        body.put("firstName", record.getFirstName());
        body.put("lastName", record.getLastName());
        body.put("dob", record.getDob().format(FORM_DATE));
        body.put("gender", record.getGender());
        body.put("telephone", nullToEmpty(record.getTelephone()));
        body.put("hispanic", record.getHispanic());
        body.put("hispanicOther", nullToEmpty(record.getHispanicOther()));
        body.put("race", record.getRace());
        body.put("raceOther", nullToEmpty(record.getRaceOther()));
        body.put("otherStay", record.getOtherStay());
        return body;
    }

    // --- helpers -------------------------------------------------------------

    /** The stored value is the instant of "midnight in the server's time zone" on the date of birth. */
    private static LocalDate dateOf(Instant instant) {
        return instant.atZone(Config.appTimeZone()).toLocalDate();
    }

    /** "dob" is a {@code timestamp without time zone} column that the application fills with UTC. */
    private static Instant toUtcInstant(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC);
        }
        throw new IllegalArgumentException("Unexpected type for column dob: " + value);
    }

    private static String text(Object value) {
        return value == null ? null : blankToNull(value.toString());
    }

    private static Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
