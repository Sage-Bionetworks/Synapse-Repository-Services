package org.sagebionetworks.repo.model.grid.patch.operation;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InsertValueTest {

    @Test
    public void testConstructorWithValidArguments() {
        new InsertValue(
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L)
        );
    }

    @Test
    public void testConstructorWithNullOperationId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertValue(
                    null,
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L)
            );
        });
    }

    @Test
    public void testConstructorWithNullValueId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertValue(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    null,
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L)
            );
        });
    }

    @Test
    public void testConstructorWithNullReferenceId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertValue(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    null
            );
        });
    }

    @Test
    public void testGetSpan() {
        InsertValue op = new InsertValue(
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L)
        );
        assertEquals(1L, op.getSpan());
    }
}

