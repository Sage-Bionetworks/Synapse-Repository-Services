package org.sagebionetworks.repo.model.grid.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;

public class PatchTest {

	private List<LogicalTimestamp> listOne;
	private List<LogicalTimestamp> listTwo;

	@BeforeEach
	public void before() {
		listOne = Arrays.asList(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		listTwo = Arrays.asList(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L));
	}

	@Test
	public void testAddNewOperation() {
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		// call under test
		NewConstant con = patch.addNewOperation(NewConstant.class);
		NewConstant expected = new NewConstant().setOperationId(patch.getPatchId());
		assertEquals(expected, con);

		// call under test
		NewConstant con2 = patch.addNewOperation(NewConstant.class);
		NewConstant expected2 = new NewConstant()
				.setOperationId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L));
		assertEquals(expected2, con2);

		assertEquals(Arrays.asList(con, con2), patch.getOperations());
		assertEquals(2L, patch.getSpan());
	}

	@Test
	public void testAddNewOperationWithInsertArrays() {
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		// call under test
		InsertArray op = patch.addNewOperation(InsertArray.class).setElementIds(listOne);
		InsertArray expected = new InsertArray().setElementIds(listOne)
				.setOperationId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		assertEquals(expected, op);

		// call under test
		InsertArray op2 = patch.addNewOperation(InsertArray.class).setElementIds(listTwo);
		InsertArray expected2 = new InsertArray().setElementIds(listTwo)
				.setOperationId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L));
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
