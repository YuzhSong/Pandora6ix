import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Offline, operator-run maintenance. No cloud credentials are embedded. */
class RenameEmployeeAccounts {
    record Mapping(String oldLogin, String newLogin, String displayName) {}
    record Target(Mapping mapping, String id, long version) {}

    static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing environment variable: " + name);
        return value;
    }

    static List<Mapping> mappings(Path file) throws Exception {
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.getFirst().equals("oldLoginName\tnewLoginName\texpectedDisplayName"))
            throw new IllegalArgumentException("Mapping header must be oldLoginName/newLoginName/expectedDisplayName (tab separated).");
        var result = new ArrayList<Mapping>();
        var oldNames = new HashSet<String>();
        var newNames = new HashSet<String>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            var fields = line.split("\t", -1);
            if (fields.length != 3 || fields[0].isBlank() || fields[2].isBlank()
                    || !fields[1].matches("[a-z][a-z0-9-]{0,127}")
                    || !oldNames.add(fields[0]) || !newNames.add(fields[1]))
                throw new IllegalArgumentException("Invalid or duplicate mapping row.");
            result.add(new Mapping(fields[0], fields[1], fields[2]));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Mapping is empty.");
        return result;
    }

    static void update(Connection db, String sql, Object... values) throws SQLException {
        try (var statement = db.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Account changed during maintenance; rollback required.");
        }
    }

    static void run(String domain, List<Mapping> mappings, boolean apply) throws Exception {
        String url = required("PANDORA_DB_URL");
        if (!url.startsWith("jdbc:h2:file:") || url.toUpperCase(Locale.ROOT).contains("INIT="))
            throw new IllegalArgumentException("Only an existing local H2 file database is supported.");
        String password = apply ? required("PANDORA_RESET_PASSWORD") : null;
        if (apply && (password.length() < 10 || password.getBytes(StandardCharsets.UTF_8).length > 72))
            throw new IllegalArgumentException("Reset password must be at least 10 characters and at most 72 UTF-8 bytes.");
        try (var db = DriverManager.getConnection(url + ";IFEXISTS=TRUE", System.getenv().getOrDefault("PANDORA_DB_USER", "sa"), System.getenv().getOrDefault("PANDORA_DB_PASSWORD", ""))) {
            db.setAutoCommit(false);
            try {
                String enterprise;
                try (var statement = db.prepareStatement("SELECT id FROM enterprise WHERE login_domain=? FOR UPDATE")) {
                    statement.setString(1, domain);
                    try (var rows = statement.executeQuery()) {
                        if (!rows.next()) throw new IllegalArgumentException("Company login domain not found.");
                        enterprise = rows.getString(1);
                    }
                }
                var targets = new ArrayList<Target>();
                for (var mapping : mappings) {
                    try (var statement = db.prepareStatement("SELECT id,display_name,version FROM app_user WHERE enterprise_id=? AND login_name=? FOR UPDATE")) {
                        statement.setString(1, enterprise); statement.setString(2, mapping.oldLogin());
                        try (var rows = statement.executeQuery()) {
                            if (!rows.next() || !mapping.displayName().equals(rows.getString("display_name")))
                                throw new IllegalArgumentException("Old account/name mismatch: " + mapping.oldLogin());
                            targets.add(new Target(mapping, rows.getString("id"), rows.getLong("version")));
                        }
                    }
                }
                var ids = new HashSet<String>();
                for (var target : targets) ids.add(target.id());
                for (var target : targets) {
                    try (var statement = db.prepareStatement("SELECT user_id FROM user_role WHERE enterprise_id=? AND user_id=? AND role_code='ADMIN'")) {
                        statement.setString(1, enterprise); statement.setString(2, target.id());
                        try (var rows = statement.executeQuery()) {
                            if (rows.next()) throw new IllegalArgumentException("Administrator accounts cannot be changed by this tool.");
                        }
                    }
                    try (var statement = db.prepareStatement("SELECT id FROM app_user WHERE enterprise_id=? AND login_name=?")) {
                        statement.setString(1, enterprise); statement.setString(2, target.mapping().newLogin());
                        try (var rows = statement.executeQuery()) {
                            if (rows.next() && !ids.contains(rows.getString(1))) throw new IllegalArgumentException("Target login is occupied: " + target.mapping().newLogin());
                        }
                    }
                }
                System.out.println("Validated company=" + domain + ", employees=" + targets.size());
                for (var target : targets) System.out.println(target.mapping().oldLogin() + " -> " + target.mapping().newLogin() + " (" + target.mapping().displayName() + ")");
                if (!apply) {
                    db.rollback();
                    System.out.println("PREVIEW ONLY: no account, password or session was changed.");
                    return;
                }
                var encoder = new BCryptPasswordEncoder();
                for (var target : targets) update(db, "UPDATE app_user SET login_name=? WHERE enterprise_id=? AND id=? AND version=?",
                        "rename-" + UUID.randomUUID(), enterprise, target.id(), target.version());
                for (var target : targets) {
                    update(db, "UPDATE app_user SET login_name=?,password_hash=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",
                            target.mapping().newLogin(), encoder.encode(password), enterprise, target.id(), target.version());
                    try (var statement = db.prepareStatement("UPDATE auth_session SET revoked=TRUE WHERE enterprise_id=? AND user_id=?")) {
                        statement.setString(1, enterprise); statement.setString(2, target.id()); statement.executeUpdate();
                    }
                }
                db.commit();
                System.out.println("COMMITTED: employee IDs/profiles/roles/business records preserved; old employee sessions revoked.");
            } catch (Exception failure) { db.rollback(); throw failure; }
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2 || args.length > 3 || (args.length == 3 && !args[2].equals("--apply")))
                throw new IllegalArgumentException("Usage: RenameEmployeeAccounts.java <company-domain> <mapping.tsv> [--apply]");
            run(args[0], mappings(Path.of(args[1])), args.length == 3);
        } catch (IllegalArgumentException failure) {
            System.err.println("NOT APPLIED / ROLLED BACK: " + failure.getMessage()); System.exit(1);
        } catch (Exception failure) {
            System.err.println("NOT APPLIED / ROLLED BACK: database or runtime error (" + failure.getClass().getSimpleName() + "). Check file lock, permissions, schema and Java classpath."); System.exit(1);
        }
    }
}
