package org.sagebionetworks.repo.model.grid.patch.operation;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InsertArrayTest {

    @Test
    public void testConstructorWithValidArguments() {
        new InsertArray(
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L),
                Collections.singletonList(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L))
        );
    }

    @Test
    public void testConstructorWithNullOperationId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertArray(
                    null,
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L),
                    Collections.singletonList(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L))
            );
        });
    }

    @Test
    public void testConstructorWithNullArrayId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertArray(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    null,
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L),
                    Collections.singletonList(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L))
            );
        });
    }

    @Test
    public void testConstructorWithNullReferenceId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertArray(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    null,
                    Collections.singletonList(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L))
            );
        });
    }

    @Test
    public void testConstructorWithNullElementIds() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertArray(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L),
                    null
            );
        });
    }

    @Test
    public void testGetSpan() {
        InsertArray op = new InsertArray(
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L),
                Collections.nCopies(5, new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L))
        );
        assertEquals(5, op.getSpan());
    }
}

