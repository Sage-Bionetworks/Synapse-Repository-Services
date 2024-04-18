package org.sagebionetworks.repo.manager.util;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.query.model.QueryExpression;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.manager.util.MaterializedViewUtils.getQuerySpecification;
import static org.sagebionetworks.repo.manager.util.MaterializedViewUtils.getSourceTableIds;

public class MaterializedViewUtilsTest {

    @Test
    public void testGetQuerySpecification() {
        String sql = "SELECT * FROM syn123";

        QueryExpression result = getQuerySpecification(sql);

        assertNotNull(result);
    }

    @Test
    public void testGetQuerySpecificationWithParingException() {
        String sql = "invalid query";

        String message = assertThrows(IllegalArgumentException.class, () -> {
            getQuerySpecification(sql);
        }).getMessage();

        assertTrue(message.startsWith("Encountered \" <regular_identifier>"));
    }

    @Test
    public void testGetQuerySpecificationWithNullQuery() {
        String sql = null;

        String message = assertThrows(IllegalArgumentException.class, () -> {
            getQuerySpecification(sql);
        }).getMessage();

        assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
    }

    @Test
    public void testGetQuerySpecificationWithEmptyQuery() {
        String sql = "";

        String message = assertThrows(IllegalArgumentException.class, () -> {
            getQuerySpecification(sql);
        }).getMessage();

        assertEquals("The definingSQL of the materialized view is required and must not be the empty string.", message);
    }

    @Test
    public void testGetQuerySpecificationWithBlankQuery() {
        String sql = "   ";

        String message = assertThrows(IllegalArgumentException.class, () -> {
            getQuerySpecification(sql);
        }).getMessage();

        assertEquals("The definingSQL of the materialized view is required and must not be a blank string.", message);
    }


    @Test
    public void testGetSourceTableIds() {

        QueryExpression query = getQuerySpecification("SELECT * FROM syn123");

        Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"));
        Set<IdAndVersion> result = getSourceTableIds(query);

        assertEquals(expected, result);
    }

    @Test
    public void testGetSourceTableIdsWithVersion() {

        QueryExpression query = getQuerySpecification("SELECT * FROM syn123.1");

        Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123.1"));
        Set<IdAndVersion> result = getSourceTableIds(query);

        assertEquals(expected, result);
    }

    @Test
    public void testGetSourceTableIdsWithMultiple() {

        QueryExpression query = getQuerySpecification("SELECT * FROM syn123.1 JOIN syn456 JOIN syn123");

        Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn123.1"),
                IdAndVersion.parse("456"));
        Set<IdAndVersion> result = getSourceTableIds(query);

        assertEquals(expected, result);
    }
}
