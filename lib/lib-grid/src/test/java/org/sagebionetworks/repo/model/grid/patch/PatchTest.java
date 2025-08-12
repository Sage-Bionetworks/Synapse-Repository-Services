package org.sagebionetworks.repo.model.grid.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;

public class PatchTest {

	private List<LogicalTimestamp> listOne;
	private List<LogicalTimestamp> listTwo;
	private LogicalTimestamp arrayId;
	private LogicalTimestamp referenceId;

	@BeforeEach
	public void before() {
		listOne = Arrays.asList(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		listTwo = Arrays.asList(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L));

		arrayId = new LogicalTimestamp().setReplicaId(11L).setSequenceNumber(12L);
		referenceId = new LogicalTimestamp().setReplicaId(13L).setSequenceNumber(14L);
	}

	@Test
	public void testAddNewOperation() {
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		// call under test
		NewConstant con = patch.addNewOperation(Operations.newConstant());
		NewConstant expected = new NewConstant(patch.getPatchId(), null);
		assertEquals(expected, con);

		// call under test
		NewConstant con2 = patch.addNewOperation(Operations.newConstant());
		NewConstant expected2 = new NewConstant(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L), null);
		assertEquals(expected2, con2);

		assertEquals(Arrays.asList(con, con2), patch.getOperations());
		assertEquals(2L, patch.getSpan());
	}

	@Test
	public void testAddNewOperationWithInsertArrays() {
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		// call under test
		InsertArray op = patch.addNewOperation(Operations.insertArray().setElementIds(listOne).setArrayId(arrayId).setReferenceId(referenceId));
		InsertArray expected = new InsertArray(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L), arrayId, referenceId, listOne);
		assertEquals(expected, op);

		// call under test
		InsertArray op2 = patch.addNewOperation(Operations.insertArray().setElementIds(listTwo).setArrayId(arrayId).setReferenceId(referenceId));
		InsertArray expected2 = new InsertArray(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L), arrayId, referenceId, listTwo);
		assertEquals(expected2, op2);

		assertEquals(Arrays.asList(op, op2), patch.getOperations());

		// call under test
		assertEquals(5L, patch.getSpan());
	}

	@Test
	public void testGetSpanWithNullArray() {
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		// call under test
		assertEquals(0L, patch.getSpan());
	}

	@Test
	public void testGetLastOPerationsWithEmptyArray() {
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))
				.setOperations(Collections.emptyList());
		// call under test
		assertEquals(0L, patch.getSpan());
	}

}
