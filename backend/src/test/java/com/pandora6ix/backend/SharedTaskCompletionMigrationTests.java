package com.pandora6ix.backend;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SharedTaskCompletionMigrationTests {
    @TempDir Path directory;

    @Test void mixedLegacyTasksShareTheVerifiedOrPendingResultAndRetainHistory() throws Exception {
        String url="jdbc:h2:file:"+directory.resolve("shared").toAbsolutePath()+";DB_CLOSE_ON_EXIT=FALSE";
        Flyway.configure().dataSource(url,"sa","").target("16").load().migrate();
        try(var connection=DriverManager.getConnection(url,"sa","");var sql=connection.createStatement()) {
            sql.executeUpdate("INSERT INTO enterprise(id,login_domain,name) VALUES('e','shared-fixture','Fixture')");
            for(String user:new String[]{"owner","a","b","removed"}) sql.executeUpdate("INSERT INTO app_user(id,enterprise_id,login_name,password_hash,display_name) VALUES('"+user+"','e','"+user+"','unused-fixture','Fixture')");
            for(String task:new String[]{"done","pending","active"}) {
                sql.executeUpdate("INSERT INTO task(id,enterprise_id,creator_user_id,task_kind,title,priority_level,start_date,end_date,time_kind,review_status) VALUES('"+task+"','e','owner','DELEGATED','Fixture',3,CURRENT_DATE,CURRENT_DATE,'ALL_DAY','APPROVED')");
                for(String user:new String[]{"a","b"}) sql.executeUpdate("INSERT INTO task_assignment(id,enterprise_id,task_id,recipient_user_id,execution_status) VALUES('"+task+user+"','e','"+task+"','"+user+"','ACTIVE')");
            }
            sql.executeUpdate("UPDATE task_assignment SET execution_status='COMPLETED',work_status='COMPLETED',completion_source='CREATOR_ACCEPTANCE',completed_by_user_id='owner',completed_at=TIMESTAMP WITH TIME ZONE '2026-11-01 10:00:00+08' WHERE id='donea'");
            sql.executeUpdate("UPDATE task_assignment SET execution_status='AWAITING_ACCEPTANCE' WHERE id IN ('doneb','pendinga')");
            sql.executeUpdate("INSERT INTO task_assignment(id,enterprise_id,task_id,recipient_user_id,execution_status,current_member,completion_source,completed_by_user_id,completed_at) VALUES('removed','e','active','removed','COMPLETED',FALSE,'CREATOR_ACCEPTANCE','owner',CURRENT_TIMESTAMP)");
            sql.executeUpdate("INSERT INTO task_completion_request(id,enterprise_id,assignment_id,requested_by_user_id,submitted_note) VALUES('rd','e','doneb','b','Retained request'),('rp','e','pendinga','a','Pending note')");
            sql.executeUpdate("INSERT INTO task_progress_record(id,enterprise_id,assignment_id,actor_user_id,progress_percent,note) VALUES('history','e','pendingb','b',10,'Keep history')");
        }
        assertEquals(1,Flyway.configure().dataSource(url,"sa","").load().migrate().migrationsExecuted);
        try(var connection=DriverManager.getConnection(url,"sa","");var sql=connection.createStatement()) {
            try(var rows=sql.executeQuery("SELECT completion_status FROM task ORDER BY id")) {
                assertTrue(rows.next());assertEquals("NOT_COMPLETED",rows.getString(1));
                assertTrue(rows.next());assertEquals("COMPLETED",rows.getString(1));
                assertTrue(rows.next());assertEquals("AWAITING_ACCEPTANCE",rows.getString(1));
            }
            try(var row=sql.executeQuery("SELECT execution_status,completed_by_user_id,completion_source,completed_at FROM task_assignment WHERE id='doneb'")) {
                assertTrue(row.next());assertEquals("COMPLETED",row.getString(1));assertEquals("owner",row.getString(2));assertEquals("CREATOR_ACCEPTANCE",row.getString(3));assertNotNull(row.getObject(4));
            }
            try(var row=sql.executeQuery("SELECT COUNT(*) FROM task_assignment WHERE task_id='pending' AND execution_status='AWAITING_ACCEPTANCE' AND work_status='COMPLETED'")) {assertTrue(row.next());assertEquals(2,row.getInt(1));}
            try(var row=sql.executeQuery("SELECT decision,decided_by_user_id,submitted_note FROM task_completion_request WHERE id='rd'")) {assertTrue(row.next());assertEquals("ACCEPTED",row.getString(1));assertEquals("owner",row.getString(2));assertEquals("Retained request",row.getString(3));}
            try(var row=sql.executeQuery("SELECT decision FROM task_completion_request WHERE id='rp'")) {assertTrue(row.next());assertEquals("PENDING",row.getString(1));}
            try(var row=sql.executeQuery("SELECT note FROM task_progress_record WHERE id='history'")) {assertTrue(row.next());assertEquals("Keep history",row.getString(1));}
        }
    }
}
