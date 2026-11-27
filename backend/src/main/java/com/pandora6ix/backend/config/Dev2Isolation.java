package com.pandora6ix.backend.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Profile("dev2")
public class Dev2Isolation {
    public static Path dataRoot() {
        return Path.of(System.getProperty("user.home"), ".pandora6ix-dev2").toAbsolutePath();
    }

    @Bean
    static BeanFactoryPostProcessor requireDedicatedDatabase(Environment env) {
        return factory -> {
            String expected = "jdbc:h2:file:" + System.getProperty("user.home")
                    + "/.pandora6ix-dev2/data/crm;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0";
            String effective = env.getProperty("spring.datasource.url");
            boolean testDb = "jdbc:h2:mem:pandora-crm-tests;DB_CLOSE_DELAY=-1".equals(effective);
            if ((!expected.equals(effective) && !testDb) || List.of(
                    "spring.flyway.url", "spring.flyway.user", "spring.flyway.password",
                    "spring.datasource.jndi-name", "spring.datasource.hikari.jdbc-url",
                    "spring.datasource.hikari.data-source-class-name",
                    "spring.datasource.hikari.data-source-jndi").stream().anyMatch(env::containsProperty)) {
                throw new IllegalStateException("The dev2 profile requires the dedicated CRM database.");
            }
            verifyLocalPath(dataRoot());
            verifyLocalPath(dataRoot().resolve("data"));
            verifyLocalPath(dataRoot().resolve("data/crm.mv.db"));
            verifyLocalPath(dataRoot().resolve(testDb ? "test-attachments" : "attachments"));
        };
    }

    public static void verifyLocalPath(Path path) {
        try {
            if (Files.isSymbolicLink(path) || Files.exists(path) && !path.toRealPath()
                    .equals(Path.of(System.getProperty("user.home")).toRealPath()
                            .resolve(Path.of(System.getProperty("user.home")).relativize(path)))) {
                throw new IllegalStateException("The CRM storage path must not be redirected: " + path);
            }
            // Reject a second hard link to the database where the filesystem exposes link counts.
            if (Files.isRegularFile(path) && Files.getFileStore(path).supportsFileAttributeView("posix")
                    && ((Number) Files.getAttribute(path, "unix:nlink")).intValue() != 1) {
                throw new IllegalStateException("The CRM database must not be shared: " + path);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot verify CRM storage: " + path, ex);
        }
    }
}
