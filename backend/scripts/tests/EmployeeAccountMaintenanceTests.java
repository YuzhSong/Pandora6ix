import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Runs the maintenance source in child JVMs against disposable H2 files only. */
class EmployeeAccountMaintenanceTests {
    static final String PASSWORD = "maintenance-fixture-password";
    record Result(int status, String output) {}
    record Fixture(Path directory, Path mapping, String url) {}
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static Fixture fixture() throws Exception {
        Path directory = Files.createTempDirectory("pandora-maintenance-test-");
        String url = "jdbc:h2:file:" + directory.resolve("fixture").toString().replace('\\', '/');
        Path mapping = directory.resolve("mapping.tsv");
        Files.writeString(mapping, "oldLoginName\tnewLoginName\texpectedDisplayName\noldone\tnewone\tOne\noldtwo\tnewtwo\tTwo\n");
        try (var db = DriverManager.getConnection(url, "sa", ""); var sql = db.createStatement()) {
            sql.execute("CREATE TABLE enterprise(id VARCHAR PRIMARY KEY,login_domain VARCHAR UNIQUE)");
            sql.execute("CREATE TABLE app_user(id VARCHAR PRIMARY KEY,enterprise_id VARCHAR,login_name VARCHAR,display_name VARCHAR,password_hash VARCHAR,version BIGINT,job_title VARCHAR,UNIQUE(enterprise_id,login_name))");
            sql.execute("CREATE TABLE user_role(enterprise_id VARCHAR,user_id VARCHAR,role_code VARCHAR)");
            sql.execute("CREATE TABLE auth_session(enterprise_id VARCHAR,user_id VARCHAR,revoked BOOLEAN)");
            sql.execute("CREATE TABLE task_fixture(author_id VARCHAR REFERENCES app_user(id),content VARCHAR)");
            sql.execute("INSERT INTO enterprise VALUES('e','fixture'),('other','other')");
            sql.execute("INSERT INTO app_user VALUES('admin','e','admin','Admin','admin-hash',3,'Admin'),('one','e','oldone','One','old-hash',4,'Developer'),('two','e','oldtwo','Two','old-hash',8,'Designer'),('foreign','other','newone','Foreign','foreign-hash',2,'Other')");
            sql.execute("INSERT INTO user_role VALUES('e','admin','ADMIN'),('e','one','EMPLOYEE'),('e','two','EMPLOYEE')");
            sql.execute("INSERT INTO auth_session VALUES('e','admin',FALSE),('e','one',FALSE),('e','two',FALSE),('other','foreign',FALSE)");
            sql.execute("INSERT INTO task_fixture VALUES('one','Keep task')");
        }
        return new Fixture(directory, mapping, url);
    }
    static Result execute(Path tool, Fixture f, boolean apply) throws Exception {
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "--class-path", System.getProperty("java.class.path"), tool.toString(), "fixture", f.mapping().toString()));
        if (apply) command.add("--apply");
        var process = new ProcessBuilder(command).redirectErrorStream(true);
        process.environment().put("PANDORA_DB_URL", f.url());
        process.environment().put("PANDORA_DB_USER", "sa");
        process.environment().put("PANDORA_DB_PASSWORD", "");
        process.environment().put("PANDORA_RESET_PASSWORD", PASSWORD);
        var child = process.start();
        String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int status = child.waitFor();
        check(!output.contains(PASSWORD) && !output.contains("$2a$"), "Password/hash leaked to output");
        return new Result(status, output);
    }
    static long count(Fixture f, String query) throws Exception {
        try (var db = DriverManager.getConnection(f.url(), "sa", ""); var sql = db.createStatement(); var rows = sql.executeQuery(query)) { rows.next(); return rows.getLong(1); }
    }
    static void unchanged(Fixture f) throws Exception {
        check(count(f,"SELECT COUNT(*) FROM app_user WHERE id IN('one','two') AND login_name IN('oldone','oldtwo') AND password_hash='old-hash' AND version IN(4,8)") == 2, "Employee data changed despite failure/preview");
        check(count(f,"SELECT COUNT(*) FROM auth_session WHERE revoked=TRUE") == 0, "Sessions revoked despite failure/preview");
    }
    static void sql(Fixture f, String query) throws Exception {
        try (var db = DriverManager.getConnection(f.url(),"sa",""); var statement = db.createStatement()) { statement.execute(query); }
    }
    public static void main(String[] args) throws Exception {
        Path tool = Path.of(args[0]).toAbsolutePath();
        var preview = fixture(); check(execute(tool,preview,false).status()==0,"Preview failed"); unchanged(preview);
        var applied = fixture(); check(execute(tool,applied,true).status()==0,"Apply failed");
        check(count(applied,"SELECT COUNT(*) FROM app_user WHERE (id='one' AND login_name='newone' AND version=5 AND job_title='Developer') OR (id='two' AND login_name='newtwo' AND version=9 AND job_title='Designer')") == 2,"IDs/profile/version not preserved");
        check(count(applied,"SELECT COUNT(*) FROM auth_session WHERE enterprise_id='e' AND user_id IN('one','two') AND revoked=TRUE")==2,"Employee sessions remain active");
        check(count(applied,"SELECT COUNT(*) FROM auth_session WHERE user_id IN('admin','foreign') AND revoked=FALSE")==2,"Unselected sessions changed");
        check(count(applied,"SELECT COUNT(*) FROM app_user WHERE id='admin' AND login_name='admin' AND password_hash='admin-hash' AND version=3")==1,"Admin changed");
        check(count(applied,"SELECT COUNT(*) FROM task_fixture WHERE author_id='one' AND content='Keep task'")==1,"Task association lost");
        check(count(applied,"SELECT COUNT(*) FROM user_role WHERE enterprise_id='e'")==3,"Roles changed");
        try(var db=DriverManager.getConnection(applied.url(),"sa",""); var statement=db.createStatement(); var rows=statement.executeQuery("SELECT password_hash FROM app_user WHERE id IN('one','two')")) {
            var hashes=new HashSet<String>(); var encoder=new BCryptPasswordEncoder();
            while(rows.next()) { String hash=rows.getString(1); check(encoder.matches(PASSWORD,hash),"Wrong reset password"); hashes.add(hash); }
            check(hashes.size()==2,"Passwords did not receive independent salts");
        }
        check(execute(tool,applied,true).status()!=0,"Already-renamed old accounts unexpectedly accepted");
        var collision=fixture(); sql(collision,"INSERT INTO app_user VALUES('collision','e','newone','Collision','untouched',0,'Other')"); check(execute(tool,collision,true).status()!=0,"Collision accepted"); unchanged(collision);
        var admin=fixture(); Files.writeString(admin.mapping(),"oldLoginName\tnewLoginName\texpectedDisplayName\nadmin\trenamedadmin\tAdmin\n"); check(execute(tool,admin,true).status()!=0,"Admin accepted"); unchanged(admin);
        var stale=fixture(); sql(stale,"UPDATE app_user SET display_name='Changed' WHERE id='two'"); check(execute(tool,stale,true).status()!=0,"Stale identity accepted"); unchanged(stale);
        var rollback=fixture(); sql(rollback,"ALTER TABLE app_user ADD CONSTRAINT reject_second CHECK(login_name<>'newtwo')"); check(execute(tool,rollback,true).status()!=0,"Expected SQL rejection"); unchanged(rollback);
        var duplicate=fixture(); Files.writeString(duplicate.mapping(),"oldLoginName\tnewLoginName\texpectedDisplayName\noldone\tnewone\tOne\noldtwo\tnewone\tTwo\n"); check(execute(tool,duplicate,true).status()!=0,"Duplicate target accepted"); unchanged(duplicate);
        var missing=fixture(); var absent=new Fixture(missing.directory(),missing.mapping(),"jdbc:h2:file:"+missing.directory().resolve("absent").toString().replace('\\','/')); check(execute(tool,absent,true).status()!=0,"Missing database accepted"); check(!Files.exists(missing.directory().resolve("absent.mv.db")),"Tool created a missing DB");
        System.out.println("PASS: preview, apply/password hashes/session revocation/identity preservation, rerun refusal, collision, admin protection, stale identity, atomic rollback, duplicate mapping, missing DB refusal.");
    }
}
