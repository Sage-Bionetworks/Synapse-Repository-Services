package org.sagebionetworks.repo.model.grid.patch.operation;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InsertObjectTest {

    @Test
    public void testConstructorWithValidArguments() {
        new InsertObject(
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                Collections.singletonMap("key", new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L))
        );
    }

    @Test
    public void testConstructorWithNullOperationId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertObject(
                    null,
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    Collections.singletonMap("key", new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L))
            );
        });
    }

    @Test
    public void testConstructorWithNullObjectId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertObject(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    null,
                    Collections.singletonMap("key", new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L))
            );
        });
    }

    @Test
    public void testConstructorWithNullMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertObject(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    null
            );
        });
    }

    @Test
    public void testConstructorWithEmptyMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InsertObject(
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                    new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                    Collections.emptyMap()
            );
        });
    }

    @Test
    public void testGetSpan() {
        InsertObject op = new InsertObject(
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
                new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
                Collections.singletonMap("key", new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L))
        );
        assertEquals(1, op.getSpan());
    }
}

