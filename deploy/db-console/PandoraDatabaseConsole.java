package com.pandora6ix.ops;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.h2.server.web.WebServer;
import org.h2.tools.Server;

/** Optional operations entry point; the released application and embedded DB stay in one JVM. */
public final class PandoraDatabaseConsole {
    public static final String READER = "PANDORA_VIEWER";
    public static final String SCHEMA = "PANDORA_VIEW";
    private static final Set<String> TABLES = Set.of(
            "ENTERPRISE", "ORG_UNIT", "APP_USER", "ROLE", "USER_ROLE", "ENTERPRISE_TAG",
            "TASK", "TASK_TAG", "TASK_REVIEW", "TASK_ASSIGNMENT", "TASK_PROGRESS_RECORD",
            "TASK_COMPLETION_REQUEST", "WORK_LOG", "WORK_LOG_ENTRY", "WORK_LOG_TAG",
            "COMPANY_IMPORTANT_ITEM", "PERSONAL_IMPORTANT_SELECTION_STATE", "PERSONAL_IMPORTANT_SELECTION");
    private static final Set<String> PRIVATE_COLUMNS = Set.of("PASSWORD_HASH", "TOKEN_HASH");

    private PandoraDatabaseConsole() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("h2.bindAddress", "127.0.0.1");
        if (args.length == 1 && "check-admin".equals(args[0])) {
            try (var connection = adminConnection()) {
                businessCounts(connection);
                System.out.println("Administrator connection verified.");
            } catch (SQLException e) {
                System.exit(1);
            }
            return;
        }
        if (args.length == 1 && "prepare".equals(args[0])) {
            try (var connection = adminConnection()) {
                var before = businessCounts(connection);
                int count = configureReader(connection, required("PANDORA_VIEWER_PASSWORD"));
                if (!before.equals(businessCounts(connection)))
                    throw new IllegalStateException("Business row counts changed during preparation");
                changePassword(connection, connection.getMetaData().getUserName(),
                        required("PANDORA_NEW_DB_PASSWORD"));
                System.out.println("Console database preparation complete; views=" + count);
                System.out.println("Unchanged business counts=" + before);
            } catch (SQLException e) {
                // H2 exception text can include SQL; never log a credential-bearing command.
                System.err.println("Console preparation failed; SQLState=" + e.getSQLState()
                        + ", code=" + e.getErrorCode());
                System.exit(1);
            }
            return;
        }
        try {
            Class.forName("com.pandora6ix.backend.Pandora6ixBackendApplication")
                    .getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Backend startup failed", e.getCause());
        }
        if (!"true".equals(System.getenv("PANDORA_CONSOLE_ENABLED"))) return;
        try {
            required("PANDORA_DB_PASSWORD");
            try (var connection = adminConnection()) {
                configureReader(connection, required("PANDORA_VIEWER_PASSWORD"));
            }
            var server = startConsole(required("PANDORA_DB_URL"),
                    required("PANDORA_CONSOLE_ADMIN_PASSWORD"),
                    Integer.parseInt(System.getenv().getOrDefault("PANDORA_CONSOLE_PORT", "18082")),
                    Path.of(required("PANDORA_CONSOLE_STATE")));
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "pandora-console-stop"));
            System.out.println("Pandora read-only database console listening on loopback.");
        } catch (Exception e) {
            System.err.println("Pandora database console unavailable; failure type="
                    + e.getClass().getSimpleName());
        }
    }

    private static Map<String, Long> businessCounts(Connection connection) throws SQLException {
        Map<String, Long> result = new TreeMap<>();
        for (String table : List.of("APP_USER", "ORG_UNIT", "TASK", "WORK_LOG", "WORK_LOG_ENTRY")) {
            try (var statement = connection.createStatement();
                    var rows = statement.executeQuery("SELECT COUNT(*) FROM PUBLIC." + table)) {
                rows.next(); result.put(table, rows.getLong(1));
            }
        }
        return result;
    }

    private static Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(required("PANDORA_DB_URL"),
                required("PANDORA_DB_USER"), System.getenv().getOrDefault("PANDORA_DB_PASSWORD", ""));
    }

    public static void changePassword(Connection connection, String user, String password)
            throws SQLException {
        if (password.length() < 24) throw new IllegalArgumentException("A generated password is required");
        try (var statement = connection.prepareStatement("ALTER USER " + quote(user) + " SET PASSWORD ?")) {
            statement.setString(1, password);
            statement.execute();
        }
    }

    public static int configureReader(Connection connection, String password) throws SQLException {
        if (password.length() < 24) throw new IllegalArgumentException("A generated reader password is required");
        try (var statement = connection.prepareStatement("CREATE USER IF NOT EXISTS " + READER + " PASSWORD ?")) {
            statement.setString(1, password);
            statement.execute();
        }
        changePassword(connection, READER, password);
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER USER " + READER + " ADMIN FALSE");
            statement.execute("REVOKE ALL ON SCHEMA PUBLIC FROM " + READER);
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA + " AUTHORIZATION "
                    + quote(connection.getMetaData().getUserName()));
            int count = 0;
            for (String table : new TreeSet<>(TABLES)) {
                var columns = new ArrayList<String>();
                try (var query = connection.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_NAME=? ORDER BY ORDINAL_POSITION")) {
                    query.setString(1, table);
                    try (var rows = query.executeQuery()) {
                        while (rows.next()) {
                            String column = rows.getString(1);
                            if (!PRIVATE_COLUMNS.contains(column)) columns.add(quote(column));
                        }
                    }
                }
                if (columns.isEmpty()) continue;
                statement.execute("CREATE OR REPLACE VIEW " + SCHEMA + "." + quote(table)
                        + " AS SELECT " + String.join(",", columns) + " FROM PUBLIC." + quote(table));
                count++;
            }
            statement.execute("GRANT SELECT ON SCHEMA " + SCHEMA + " TO " + READER);
            return count;
        }
    }

    public static Server startConsole(String jdbcUrl, String consoleAdminPassword, int port, Path state)
            throws Exception {
        System.setProperty("h2.bindAddress", "127.0.0.1");
        Files.createDirectories(state);
        var settings = new Properties();
        settings.setProperty("webAllowOthers", "false");
        settings.setProperty("webAdminPassword", WebServer.encodeAdminPassword(consoleAdminPassword));
        settings.setProperty("0", "Pandora read-only|org.h2.Driver|" + jdbcUrl.split(";", 2)[0]
                + ";IFEXISTS=TRUE;SCHEMA=" + SCHEMA + "|" + READER);
        try (OutputStream output = Files.newOutputStream(state.resolve(".h2.server.properties"))) {
            settings.store(output, "Pandora local operations console; no database passwords stored here");
        }
        return Server.createWebServer("-webPort", Integer.toString(port), "-webDaemon",
                "-ifExists", "-properties", state.toString()).start();
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + key);
        return value;
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
