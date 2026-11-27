package com.pandora6ix.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class Dev2IsolationTests {
    @Test
    void rejectsAnInheritedPhaseOneDatabaseBeforeOpeningAnyConnection() {
        var inherited = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:file:./data/pandora");
        assertThrows(IllegalStateException.class, () -> Dev2Isolation.requireDedicatedDatabase(inherited)
                .postProcessBeanFactory(new DefaultListableBeanFactory()));
    }

    @Test
    void rejectsAnIndependentFlywayOverride() {
        var overridden = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:pandora-crm-tests;DB_CLOSE_DELAY=-1")
                .withProperty("spring.flyway.url", "jdbc:h2:file:./data/pandora");
        assertThrows(IllegalStateException.class, () -> Dev2Isolation.requireDedicatedDatabase(overridden)
                .postProcessBeanFactory(new DefaultListableBeanFactory()));
    }
}
