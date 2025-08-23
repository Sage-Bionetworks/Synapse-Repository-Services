package org.sagebionetworks.repo.model.grid.patch.operation;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class NewConstantTest {

    @Test
    public void testConstructorWithValidArguments() {
        new NewConstant(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L), new ConValue(ConType.STRING, "test"));
    }

    @Test
    public void testConstructorWithNullOperationId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new NewConstant(null, new ConValue(ConType.STRING, "test"));
        });
    }

    @Test
    public void testGetSpan() {
        NewConstant op = new NewConstant(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L), new ConValue(ConType.STRING, "test"));
        assertEquals(1L, op.getSpan());
    }

    @Test
    public void testIsTimestamp() {
        NewConstant op1 = new NewConstant(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L), new ConValue(ConType.TIMESTAMP, new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(2L)));
        assertTrue(op1.isTimestamp());

        NewConstant op2 = new NewConstant(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L), new ConValue(ConType.STRING, "test"));
        assertFalse(op2.isTimestamp());

        NewConstant op3 = new NewConstant(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L), null);
        assertFalse(op3.isTimestamp());
    }
}

