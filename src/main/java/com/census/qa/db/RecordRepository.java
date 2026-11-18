package com.census.qa.db;

import com.census.qa.model.PersonRecord;
import com.census.qa.model.RecordMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Database queries for person records.
 *
 * <p>Schema (created by Prisma, hence the quoted mixed-case identifiers):
 * <pre>
 *   "User" (id, email, "householdId")  --1:1--  "Household" (id, address...)
 *   "Record" (id, "firstName", ..., "householdId")   a person; belongs to a household
 *   "Relative" (id, "userId", "recordId", relationship)   how that person is related to a user
 * </pre>
 * A person added on the UI is therefore TWO rows: one in "Record" and one in "Relative".
 */
public class RecordRepository {

    /** One row per person: the record columns plus the relationship and the owning user. */
    private static final String SELECT_PERSON = """
            SELECT r.id,
                   r."householdId",
                   r."firstName",
                   r."lastName",
                   r.dob,
                   r.gender::text        AS gender,
                   r.telephone,
                   r.hispanic::text      AS hispanic,
                   r."hispanicOther",
                   r.race::text          AS race,
                   r."raceOther",
                   r."otherStay"::text   AS "otherStay",
                   rel.relationship::text AS relationship,
                   u."householdId"       AS "userHouseholdId"
              FROM "Record" r
              JOIN "Relative" rel ON rel."recordId" = r.id
              JOIN "User" u       ON u.id = rel."userId"
            """;

    private final DbClient db;

    public RecordRepository(DbClient db) {
        this.db = db;
    }

    /** All people with this name that belong to the given user (normally exactly one). */
    public List<PersonRecord> findByUserAndName(String userEmail, String firstName, String lastName) {
        return db.query(SELECT_PERSON + """
                         WHERE u.email = ? AND r."firstName" = ? AND r."lastName" = ?
                         ORDER BY r.id
                        """, userEmail, firstName, lastName)
                .stream().map(RecordMapper::fromDb).collect(Collectors.toList());
    }

    /** Raw joined rows - kept for attaching the "evidence" to the test report. */
    public List<Map<String, Object>> findRowsByUserAndName(String userEmail, String firstName, String lastName) {
        return db.query(SELECT_PERSON + """
                 WHERE u.email = ? AND r."firstName" = ? AND r."lastName" = ?
                 ORDER BY r.id
                """, userEmail, firstName, lastName);
    }

    public List<PersonRecord> findByUserAndRelationship(String userEmail, String relationship) {
        return db.query(SELECT_PERSON + """
                         WHERE u.email = ? AND rel.relationship::text = ?
                         ORDER BY r.id
                        """, userEmail, relationship)
                .stream().map(RecordMapper::fromDb).collect(Collectors.toList());
    }

    /** Household the user lives in - a new person must be saved under exactly this household. */
    public Integer householdIdOfUser(String userEmail) {
        List<Map<String, Object>> rows = db.query(
                "SELECT \"householdId\" FROM \"User\" WHERE email = ?", userEmail);
        Object id = rows.isEmpty() ? null : rows.get(0).get("householdId");
        return id == null ? null : ((Number) id).intValue();
    }

    public long countRecordsById(int recordId) {
        return db.count("SELECT count(*) FROM \"Record\" WHERE id = ?", recordId);
    }

    /** The "Relative" row must disappear together with its record (ON DELETE CASCADE). */
    public long countRelativesByRecordId(int recordId) {
        return db.count("SELECT count(*) FROM \"Relative\" WHERE \"recordId\" = ?", recordId);
    }

    /** Generated names are unique, so this tells whether a rejected form was saved anyway. */
    public long countRecordsByFirstOrLastName(String firstName, String lastName) {
        return db.count("SELECT count(*) FROM \"Record\" WHERE \"firstName\" = ? OR \"lastName\" = ?",
                firstName, lastName);
    }
}
