package com.pandora6ix.backend.api.error;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstraintDiagnosticsTests {
    @Test void h2QuotedNamesAreExtractedWithoutSqlOrValues() {
        assertEquals("CONSTRAINT_272", GlobalExceptionHandler.constraintName("Check constraint invalid: \"CONSTRAINT_272: \"; SQL statement: INSERT secret-value"));
        assertEquals("PUBLIC.FK_PERSON", GlobalExceptionHandler.constraintName("Referential integrity constraint violation: \"PUBLIC.FK_PERSON: PUBLIC.APP_USER FOREIGN KEY ...\"; SQL statement: private"));
        assertEquals("PUBLIC.UNIQUE_ACCOUNT", GlobalExceptionHandler.constraintName("Unique index or primary key violation: \"PUBLIC.UNIQUE_ACCOUNT ON PUBLIC.APP_USER ...\"; SQL statement: private"));
        assertEquals("fk_review_decider", GlobalExceptionHandler.constraintName("constraint \"fk_review_decider\" failed"));
    }
    @Test void unknownAndMalformedNamesNeverBecomeGenericWordsOrSqlFragments() {
        assertNull(GlobalExceptionHandler.constraintName(null));
        assertNull(GlobalExceptionHandler.constraintName("Check constraint invalid: no identifier; SQL statement: private"));
        assertNull(GlobalExceptionHandler.constraintName("Check constraint invalid: \"bad;DROP TABLE X: \""));
    }
}
