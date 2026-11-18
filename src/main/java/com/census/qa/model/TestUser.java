package com.census.qa.model;

import com.census.qa.config.Config;

/** The (fictional) account the suite logs in with. Read from configuration, never hard-coded in tests. */
public class TestUser {

    private final String name;
    private final String email;
    private final String password;

    /** Ids are assigned by the application; they are filled in once the preconditions are ensured. */
    private Integer id;
    private Integer householdId;

    public TestUser(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public static TestUser fromConfig() {
        return new TestUser(
                Config.get("test.user.name"),
                Config.get("test.user.email"),
                Config.get("test.user.password"));
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(Integer householdId) {
        this.householdId = householdId;
    }
}
