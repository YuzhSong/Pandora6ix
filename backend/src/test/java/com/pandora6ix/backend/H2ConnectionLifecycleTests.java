package com.pandora6ix.backend;

import java.sql.*;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class H2ConnectionLifecycleTests {
    @TempDir Path directory;

    @Test void enumChecksSurviveCreatorConnectionClosing() throws Exception {
        String url = "jdbc:h2:mem:lifecycle-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection survivor = DriverManager.getConnection(url)) {
            try (Connection creator = DriverManager.getConnection(url)) {
                creator.createStatement().execute("CREATE TABLE task(id VARCHAR PRIMARY KEY, kind VARCHAR CHECK(kind IN('PERSONAL','DELEGATED')))");
                creator.createStatement().execute("INSERT INTO task VALUES('initial','PERSONAL')");
            }
            survivor.createStatement().execute("INSERT INTO task VALUES('replacement','DELEGATED')");
            SQLException invalid = assertThrows(SQLException.class, () -> survivor.createStatement().execute("INSERT INTO task VALUES('invalid','UNSUPPORTED')"));
            assertEquals("23513", invalid.getSQLState());
            try (Connection replacement = DriverManager.getConnection(url)) {
                replacement.createStatement().execute("INSERT INTO task VALUES('third','PERSONAL')");
                try (ResultSet rows = replacement.createStatement().executeQuery("SELECT COUNT(*) FROM task")) {
                    assertTrue(rows.next()); assertEquals(3, rows.getInt(1));
                }
            }
            survivor.createStatement().execute("SHUTDOWN");
        }
    }

    @Test void fileDatabaseKeepsChecksAfterReopenAndConnectionRetirement() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("lifecycle").toAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE";
        try (Connection initial = DriverManager.getConnection(url)) {
            initial.createStatement().execute("CREATE TABLE task(id VARCHAR PRIMARY KEY,kind VARCHAR CHECK(kind IN('PERSONAL','DELEGATED')))");
            initial.createStatement().execute("INSERT INTO task VALUES('persisted','PERSONAL')");
        }
        try (Connection surviving = DriverManager.getConnection(url)) {
            try (Connection first = DriverManager.getConnection(url)) {
                first.createStatement().execute("SELECT * FROM task");
            }
            surviving.createStatement().execute("INSERT INTO task VALUES('after-restart','DELEGATED')");
            try (ResultSet rows = surviving.createStatement().executeQuery("SELECT COUNT(*) FROM task")) {
                assertTrue(rows.next()); assertEquals(2,rows.getInt(1));
            }
        }
    }
}
