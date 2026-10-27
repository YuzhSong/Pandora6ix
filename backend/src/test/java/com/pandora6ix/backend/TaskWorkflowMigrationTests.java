package com.pandora6ix.backend;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TaskWorkflowMigrationTests {
    @TempDir Path directory;

    @Test void upgradePreservesOldReviewsAndCompletedAssignmentsAfterReopen() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("upgrade").toAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE";
        Flyway.configure().dataSource(url, "sa", "").target("10").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.executeUpdate("INSERT INTO enterprise(id,login_domain,name) VALUES('company','upgrade','Upgrade fixture')");
            sql.executeUpdate("INSERT INTO app_user(id,enterprise_id,login_name,password_hash,display_name) VALUES('user','company','user','unused-fixture-hash','User')");
            sql.executeUpdate("INSERT INTO task(id,enterprise_id,creator_user_id,task_kind,title,priority_level,start_date,end_date,time_kind,review_status,version) VALUES('task','company','user','PERSONAL','Retained task',3,CURRENT_DATE,CURRENT_DATE,'ALL_DAY','APPROVED',7)");
            sql.executeUpdate("INSERT INTO task_review(id,enterprise_id,task_id,reviewer_user_id,submitted_by_user_id,task_version,submitted_snapshot,decision) VALUES('review','company','task','user','user',0,'{}','APPROVED')");
            sql.executeUpdate("INSERT INTO task_assignment(id,enterprise_id,task_id,recipient_user_id,execution_status,completion_source,completed_by_user_id,completed_at,version) VALUES('done','company','task','user','COMPLETED','SELF','user',CURRENT_TIMESTAMP,4)");
            sql.executeUpdate("INSERT INTO task(id,enterprise_id,creator_user_id,task_kind,title,priority_level,start_date,end_date,time_kind) VALUES('pending','company','user','PERSONAL','Pending task',3,CURRENT_DATE,CURRENT_DATE,'ALL_DAY')");
            sql.executeUpdate("INSERT INTO task_assignment(id,enterprise_id,task_id,recipient_user_id) VALUES('waiting','company','pending','user')");
        }
        assertEquals(1, Flyway.configure().dataSource(url, "sa", "").load().migrate().migrationsExecuted);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            try (var row = sql.executeQuery("SELECT review_kind,submitted_snapshot FROM task_review WHERE id='review'")) {
                assertTrue(row.next()); assertEquals("CREATE", row.getString(1)); assertEquals("{}", row.getString(2));
            }
            try (var row = sql.executeQuery("SELECT work_status,execution_status,version FROM task_assignment WHERE id='done'")) {
                assertTrue(row.next()); assertEquals("COMPLETED", row.getString(1)); assertEquals("COMPLETED", row.getString(2)); assertEquals(4, row.getLong(3));
            }
            try (var row = sql.executeQuery("SELECT work_status,execution_status FROM task_assignment WHERE id='waiting'")) {
                assertTrue(row.next()); assertEquals("IN_PROGRESS", row.getString(1)); assertEquals("PENDING_APPROVAL", row.getString(2));
            }
            try (var row = sql.executeQuery("SELECT title,version FROM task WHERE id='task'")) {
                assertTrue(row.next()); assertEquals("Retained task", row.getString(1)); assertEquals(7, row.getLong(2));
            }
        }
    }
}
