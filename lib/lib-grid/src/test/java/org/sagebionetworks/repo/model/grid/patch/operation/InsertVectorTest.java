package org.sagebionetworks.repo.model.grid.patch.operation;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InsertVectorTest {

    @Test
    public void testConstructorWithValidArguments() {
        new InsertVector(
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                Collections.singletonMap(0, new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L))
        );
    }

    @Test
    public void testConstructorWithNullOperationId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertVector(
                    null,
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    Collections.singletonMap(0, new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L))
            );
        });
    }

    @Test
    public void testConstructorWithNullVectorId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertVector(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    null,
                    Collections.singletonMap(0, new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L))
            );
        });
    }

    @Test
    public void testConstructorWithNullMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertVector(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    null
            );
        });
    }

    @Test
    public void testConstructorWithEmptyMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertVector(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    Collections.emptyMap()
            );
        });
    }

    @Test
    public void testGetSpan() {
        InsertVector op = new InsertVector(
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                Collections.singletonMap(0, new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L))
        );
        assertEquals(1, op.getSpan());
    }
}

