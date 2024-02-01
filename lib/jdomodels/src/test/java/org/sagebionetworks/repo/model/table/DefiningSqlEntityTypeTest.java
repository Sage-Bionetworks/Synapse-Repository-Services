package org.sagebionetworks.repo.model.table;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;

public class DefiningSqlEntityTypeTest {
    @Test
    public void testDefiningSqlEntityTypeIsSubsetOfEntityType() {
        // Attempt to get each corresponding entity type, will throw an exception one doesn't exist
        Arrays.stream(DefiningSqlEntityType.values()).forEach(type -> EntityType.valueOf(type.name()));
    }
}
