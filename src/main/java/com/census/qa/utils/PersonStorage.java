package com.census.qa.utils;

import com.census.qa.config.Config;
import com.census.qa.model.PersonRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stores the demographic data a test submitted on the UI as a JSON file (one file per person,
 * under target/test-data) and reads it back into a POJO.
 *
 * <p>This is the "save the added person to storage" requirement: the API and database checks
 * compare against what was really submitted, and the files remain as evidence after the run.
 */
public final class PersonStorage {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private PersonStorage() {
    }

    public static Path save(PersonRecord person) {
        Path file = fileOf(person);
        try {
            Files.createDirectories(file.getParent());
            JSON.writeValue(file.toFile(), person);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store " + file, e);
        }
        return file;
    }

    public static PersonRecord load(Path file) {
        try {
            return JSON.readValue(file.toFile(), PersonRecord.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    public static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path fileOf(PersonRecord person) {
        return Paths.get(Config.get("test.data.dir"),
                person.getFirstName() + "_" + person.getLastName() + ".json");
    }
}
