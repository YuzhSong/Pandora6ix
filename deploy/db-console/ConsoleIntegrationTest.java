package com.pandora6ix.ops;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Real file DB, H2 permissions and WebServer checks; no production fixtures or dependencies. */
public final class ConsoleIntegrationTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void denied(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
            throw new AssertionError("Expected permission denial");
        } catch (SQLException expected) {
            check(Set.of(90096, 90040, 50100).contains(expected.getErrorCode()),
                    "Unexpected failure code " + expected.getErrorCode());
        }
    }
    public static void main(String[] args) throws Exception {
        Path run = Files.createTempDirectory("pandora-console-test-");
        String url = "jdbc:h2:file:" + run.resolve("database") + ";DB_CLOSE_ON_EXIT=FALSE";
        String readerPassword = UUID.randomUUID().toString();
        String adminPassword = UUID.randomUUID().toString();
        try (var admin = DriverManager.getConnection(url, "sa", "")) {
            try (var statement = admin.createStatement()) {
                statement.execute("CREATE TABLE app_user(id INT PRIMARY KEY,display_name VARCHAR,password_hash VARCHAR)");
                statement.execute("INSERT INTO app_user VALUES(1,'Fixture','PRIVATE_HASH')");
                statement.execute("CREATE TABLE auth_session(token_hash VARCHAR)");
            }
            check(PandoraDatabaseConsole.configureReader(admin, readerPassword) == 1, "One safe view expected");
            PandoraDatabaseConsole.changePassword(admin, "SA", adminPassword);
            try {
                DriverManager.getConnection(url, "sa", "").close();
                throw new AssertionError("Blank administrator must fail");
            } catch (SQLException expected) { check(expected.getErrorCode() == 28000, "Authentication expected"); }
            try (var reader = DriverManager.getConnection(url, PandoraDatabaseConsole.READER, readerPassword)) {
                try (var rows = reader.createStatement().executeQuery("SELECT * FROM PANDORA_VIEW.APP_USER")) {
                    check(rows.getMetaData().getColumnCount() == 2, "Password hash must be absent");
                    check(rows.next() && rows.getInt(1) == 1, "Existing row must remain readable");
                }
                denied(reader, "SELECT * FROM PUBLIC.APP_USER");
                denied(reader, "SELECT * FROM PUBLIC.AUTH_SESSION");
                denied(reader, "UPDATE PUBLIC.APP_USER SET display_name=display_name WHERE 1=0");
                denied(reader, "DELETE FROM PUBLIC.APP_USER WHERE 1=0");
                denied(reader, "CREATE TABLE PUBLIC.forbidden(id INT)");
                denied(reader, "ALTER USER PANDORA_VIEWER ADMIN TRUE");
                admin.createStatement().execute("INSERT INTO APP_USER VALUES(2,'Committed live row','PRIVATE_HASH')");
                try (var rows = reader.createStatement().executeQuery("SELECT COUNT(*) FROM PANDORA_VIEW.APP_USER")) {
                    rows.next(); check(rows.getInt(1) == 2, "View must reflect committed live changes");
                }
                admin.createStatement().execute("ALTER TABLE APP_USER ADD COLUMN phone_number VARCHAR");
                PandoraDatabaseConsole.configureReader(admin, readerPassword);
                reader.createStatement().executeQuery("SELECT PHONE_NUMBER FROM PANDORA_VIEW.APP_USER").close();
            }
            int port;
            try (var socket = new java.net.ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                port = socket.getLocalPort();
            }
            var server = PandoraDatabaseConsole.startConsole(url, UUID.randomUUID().toString(), port, run.resolve("state"));
            try {
                var response = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port)).build(),
                        HttpResponse.BodyHandlers.ofString());
                check(response.statusCode() == 200 && response.body().contains("H2 Console"), "Console HTML expected");
            } finally { server.stop(); }
        }
        try (var restarted = DriverManager.getConnection(url, PandoraDatabaseConsole.READER, readerPassword)) {
            var rows = restarted.createStatement().executeQuery("SELECT COUNT(*) FROM PANDORA_VIEW.APP_USER");
            rows.next(); check(rows.getInt(1) == 2, "Reader and data persist after DB close/reopen");
        }
        System.out.println("Console integration passed: live file reads, private projection, denied writes/DDL, password rotation, refresh, HTTP and reopen.");
    }
}
