package org.sagebionetworks.repo.model.grid.patch.operation;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NewValueTest {

    @Test
    public void testConstructorWithValidArguments() {
        new NewValue(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L));
    }

    @Test
    public void testConstructorWithNullOperationId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new NewValue(null);
        });
    }

    @Test
    public void testGetSpan() {
        NewValue op = new NewValue(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L));
        assertEquals(1L, op.getSpan());
    }
}

