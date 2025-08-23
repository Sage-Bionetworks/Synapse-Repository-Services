package org.sagebionetworks.repo.model.grid.patch.operation;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NewArrayTest {

    @Test
    public void testConstructorWithValidArguments() {
        new NewArray(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L));
    }

    @Test
    public void testConstructorWithNullOperationId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new NewArray(null);
        });
    }

    @Test
    public void testGetSpan() {
        NewArray op = new NewArray(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L));
        assertEquals(1L, op.getSpan());
    }
}

