package com.pandora6ix.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CrmMigrationTests {
    @TempDir Path temp;

    @Test
    void migratesAnExistingV17CopyWithoutChangingLegacyData() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:file:" + temp.resolve("fixture"), "sa", "");
        var baseline = Flyway.configure().dataSource(dataSource).load();
        assertEquals(17, baseline.migrate().migrationsExecuted);
        var jdbc = new JdbcTemplate(dataSource);
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE 'CRM_%'
                """, Integer.class));
        jdbc.update("""
                INSERT INTO enterprise (id,login_domain,name,business_timezone) VALUES
                    ('fixture-company','fixture-domain','Existing company','Asia/Taipei')
                """);
        jdbc.update("""
                INSERT INTO app_user(id,enterprise_id,login_name,password_hash,display_name)
                VALUES ('fixture-user','fixture-company','employee','unused-fixture','Existing employee')
                """);
        jdbc.update("""
                INSERT INTO enterprise_tag(id,enterprise_id,name,color_hex,creator_user_id)
                VALUES ('fixture-tag','fixture-company','Existing tag','#1177AA','fixture-user')
                """);

        var crm = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration", "classpath:db/crm").load();
        assertEquals(4, crm.migrate().migrationsExecuted);
        assertEquals(0, crm.migrate().migrationsExecuted);
        assertEquals("21", crm.info().current().getVersion().getVersion());
        assertEquals("Existing company", jdbc.queryForObject("SELECT name FROM enterprise", String.class));
        assertEquals("Existing employee", jdbc.queryForObject("SELECT display_name FROM app_user", String.class));
        assertEquals("Existing tag", jdbc.queryForObject("SELECT name FROM enterprise_tag", String.class));
        assertEquals(7, jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE 'CRM_%'
                """, Integer.class));
    }
}
