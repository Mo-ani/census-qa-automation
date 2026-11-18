package com.census.qa.db;

import com.census.qa.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin JDBC helper: opens the connection on first use, runs parameterised queries and
 * returns plain rows (column label -> value). It knows nothing about the Census tables;
 * the SQL lives in {@link RecordRepository}.
 *
 * <p>The framework only ever READS from the database. Test data is created and removed
 * through the application (UI or API) so its business rules are never bypassed.
 */
public class DbClient implements AutoCloseable {

    private Connection connection;

    private Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                    Config.get("db.url"), Config.get("db.user"), Config.get("db.password"));
            connection.setReadOnly(true);
        }
        return connection;
    }

    /** Runs a SELECT with positional {@code ?} parameters (never string concatenation). */
    public List<Map<String, Object>> query(String sql, Object... parameters) {
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData meta = resultSet.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int column = 1; column <= meta.getColumnCount(); column++) {
                        Object value = resultSet.getObject(column);
                        // "timestamp without time zone": keep the stored wall-clock value as it is,
                        // exactly what pgAdmin/DBeaver show, instead of a JVM-time-zone dependent instant
                        if (value instanceof Timestamp timestamp) {
                            value = timestamp.toLocalDateTime();
                        }
                        row.put(meta.getColumnLabel(column), value);
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database query failed: " + sql, e);
        }
    }

    /** Runs a {@code SELECT count(*)} style query and returns the single number. */
    public long count(String sql, Object... parameters) {
        Object value = query(sql, parameters).get(0).values().iterator().next();
        return ((Number) value).longValue();
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // closing a test connection must never fail a scenario
            }
            connection = null;
        }
    }
}
