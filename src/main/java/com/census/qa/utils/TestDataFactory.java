package com.census.qa.utils;

import com.census.qa.model.PersonRecord;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates fictional test data. Every name starts with FAKE, as the challenge asks,
 * and gets a random suffix so each run works on its own, uniquely identifiable person:
 * tests never depend on (or collide with) data left behind by an earlier run.
 */
public final class TestDataFactory {

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";

    private TestDataFactory() {
    }

    /** e.g. FAKEQwhzkp - letters only and at most 13 characters (limit of "Record"."firstName"). */
    public static String uniqueFirstName() {
        return "FAKE" + capitalised(randomLetters(6));
    }

    /** e.g. FAKEZbtrmwqa - letters only and at most 18 characters (limit of "Record"."lastName"). */
    public static String uniqueLastName() {
        return "FAKE" + capitalised(randomLetters(8));
    }

    /** A complete, valid person; scenarios override the fields they care about. */
    public static PersonRecord validPerson(String relationship) {
        return new PersonRecord()
                .setRelationship(relationship)
                .setFirstName(uniqueFirstName())
                .setLastName(uniqueLastName())
                .setDob(LocalDate.of(1988, 7, 4))
                .setGender("FEMALE")
                .setTelephone("2025550100")
                .setHispanic("NO")
                .setRace("WHITE")
                .setOtherStay("NO");
    }

    /** The user's own record - a precondition of the UI tests (challenge README, step 3). */
    public static PersonRecord selfRecord() {
        return validPerson("SELF")
                .setFirstName("FAKESelf")
                .setLastName("FAKEUser")
                .setDob(LocalDate.of(1985, 1, 20))
                .setGender("MALE")
                .setTelephone("2025550101");
    }

    /** Fictional household for the test user - another precondition of the UI tests. */
    public static Map<String, Object> household(int userId) {
        Map<String, Object> household = new LinkedHashMap<>();
        household.put("userId", userId);
        household.put("homeType", "APARTMENT");
        household.put("ownership", "RENT");
        household.put("lienholderId", null);
        household.put("address1", "100 FAKE Test Street");
        household.put("address2", "Apt 1");
        household.put("city", "Faketown");
        household.put("state", "VA");
        household.put("zip", "20170");
        return household;
    }

    public static String randomLetters(int length) {
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            text.append(LETTERS.charAt(ThreadLocalRandom.current().nextInt(LETTERS.length())));
        }
        return text.toString();
    }

    private static String capitalised(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
