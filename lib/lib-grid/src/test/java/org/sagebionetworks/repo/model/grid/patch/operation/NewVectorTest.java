package org.sagebionetworks.repo.model.grid.patch.operation;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NewVectorTest {

    @Test
    public void testConstructorWithValidArguments() {
        new NewVector(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L));
    }

    @Test
    public void testConstructorWithNullOperationId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new NewVector(null);
        });
    }

    @Test
    public void testGetSpan() {
        NewVector op = new NewVector(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L));
        assertEquals(1L, op.getSpan());
    }
}

