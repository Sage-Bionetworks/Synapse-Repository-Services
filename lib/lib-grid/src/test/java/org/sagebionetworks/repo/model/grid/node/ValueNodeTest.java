package org.sagebionetworks.repo.model.grid.node;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;

public class ValueNodeTest {

	private LogicalTimestamp insertValOperationId;
	private List<LogicalTimestamp> ids;

	@BeforeEach
	public void before() {

		ids = List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L),
				new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L)
		);
		insertValOperationId = new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L);
	}

	@Test
	public void testJsonValue() {
		// call under test
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		String json = val.getValueAsJson();
		assertEquals("[3,4]", json);
		// call under test
		ValueNode other = new ValueNode().setId(ids.get(0)).setValueFromJson(json);
		assertEquals(val, other);
	}

	@Test
	public void testJsonValueWithNullValue() {
		// call under test
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(null);
		String json = val.getValueAsJson();
		assertEquals("[]", json);
		// call under test
		ValueNode other = new ValueNode().setId(ids.get(0)).setValueFromJson(json);
		assertEquals(val, other);
		;
	}

	@Test
	public void testAttemptInsert() {
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		// call under test
		assertTrue(val.attemptInsert(new InsertValue(insertValOperationId, ids.get(0), ids.get(2))));
		ValueNode expected = new ValueNode().setId(ids.get(0)).setValue(ids.get(2));
		assertEquals(expected, val);
	}

	@Test
	public void testAttemptInsertWithCurrentNull() {
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(null);
		// call under test
		assertTrue(val.attemptInsert(new InsertValue(insertValOperationId, ids.get(0), ids.get(2))));
		ValueNode expected = new ValueNode().setId(ids.get(0)).setValue(ids.get(2));
		assertEquals(expected, val);
	}

	@Test
	public void testAttemptInsertWithEqualValue() {
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		// call under test
		assertFalse(val.attemptInsert(new InsertValue(insertValOperationId, ids.get(0), ids.get(1))));
		ValueNode expected = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		assertEquals(expected, val);
	}

	@Test
	public void testAttemptInsertWithOlderValue() {
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		// call under test
		assertFalse(val.attemptInsert(new InsertValue(insertValOperationId, ids.get(0), ids.get(0))));
		ValueNode expected = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		assertEquals(expected, val);
	}

	@Test
	public void testAttemptInsertWithNullChange() {
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			assertFalse(val.attemptInsert(null));
		}).getMessage();
		assertEquals("change is required.", message);
	}

	@Test
	public void testAttemptInsertWithNullValueId() {
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			assertFalse(val.attemptInsert(new InsertValue(insertValOperationId, null, ids.get(0))));
		}).getMessage();
		assertEquals("valueId is required.", message);
	}

	@Test
	public void testAttemptInsertWithNullRef() {
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			assertFalse(val.attemptInsert(new InsertValue(insertValOperationId, ids.get(0), null)));
		}).getMessage();
		assertEquals("referenceId is required.", message);
	}

	@Test
	public void testAttemptInsertWithWrongId() {
		ValueNode val = new ValueNode().setId(ids.get(0)).setValue(ids.get(1));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			assertFalse(val.attemptInsert(new InsertValue(insertValOperationId, ids.get(2), ids.get(1))));
		}).getMessage();
		assertEquals("The ID of the passed change does not match the ID of this value.", message);
	}
}
