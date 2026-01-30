package org.sagebionetworks.repo.model.grid.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;

public class ObjectNodeTest {

	private List<LogicalTimestamp> ids;
	private LogicalTimestamp insertOperationId;

	@BeforeEach
	public void before() {

		ids = List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L),
				new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));
		insertOperationId = new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L);
	}

	@Test
	public void testJsonValue() {
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		value.put("two", ids.get(2));
		// call under test
		ObjectNode obj = new ObjectNode().setId(ids.get(0)).setValue(value);
		String json = obj.getValueAsJson();
		assertEquals("{\"one\":[3,4],\"two\":[5,6]}", json);
		// call under test
		ObjectNode other = new ObjectNode().setId(ids.get(0)).setValueFromJson(json);
		assertEquals(obj, other);
	}

	@Test
	public void testJsonValueWithNullValue() {
		// call under test
		ObjectNode obj = new ObjectNode().setId(ids.get(0)).setValue(null);
		String json = obj.getValueAsJson();
		assertEquals("{}", json);
		// call under test
		ObjectNode other = new ObjectNode().setId(ids.get(0)).setValueFromJson(json);
		assertEquals(obj, other);
	}

	@Test
	public void testAttemptInsert() {
		ObjectNode node = new ObjectNode().setId(ids.get(0));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		value.put("two", ids.get(2));
		node.setValue(value);

		Map<String, LogicalTimestamp> changeValue = new LinkedHashMap<>();
		changeValue.put("one", ids.get(3));
		InsertObject change = new InsertObject(insertOperationId, ids.get(0), changeValue);

		// call under test
		assertTrue(node.attemptInsert(change));
		ObjectNode expected = new ObjectNode().setId(ids.get(0));
		value = new LinkedHashMap<>();
		value.put("one", ids.get(3));
		value.put("two", ids.get(2));
		expected.setValue(value);
		assertEquals(expected, node);
	}

	@Test
	public void testAttemptInsertWithNewKey() {
		ObjectNode node = new ObjectNode().setId(ids.get(0));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		value.put("two", ids.get(2));
		node.setValue(value);

		Map<String, LogicalTimestamp> changeValue = new LinkedHashMap<>();
		changeValue.put("three", ids.get(1));
		InsertObject change = new InsertObject(insertOperationId, ids.get(0), changeValue);

		// call under test
		assertTrue(node.attemptInsert(change));
		ObjectNode expected = new ObjectNode().setId(ids.get(0));
		value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		value.put("two", ids.get(2));
		value.put("three", ids.get(1));
		expected.setValue(value);
		assertEquals(expected, node);
	}

	@Test
	public void testAttemptInsertWithSameValue() {
		ObjectNode node = new ObjectNode().setId(ids.get(0));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		value.put("two", ids.get(2));
		node.setValue(value);

		Map<String, LogicalTimestamp> changeValue = new LinkedHashMap<>();
		changeValue.put("one", ids.get(1));
		InsertObject change = new InsertObject(insertOperationId, ids.get(0), changeValue);

		// call under test
		assertFalse(node.attemptInsert(change));
		ObjectNode expected = new ObjectNode().setId(ids.get(0));
		value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		value.put("two", ids.get(2));
		expected.setValue(value);
		assertEquals(expected, node);
	}

	@Test
	public void testAttemptInsertWithOlderValue() {
		ObjectNode node = new ObjectNode().setId(ids.get(0));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		value.put("two", ids.get(2));
		node.setValue(value);

		Map<String, LogicalTimestamp> changeValue = new LinkedHashMap<>();
		changeValue.put("two", ids.get(1));
		InsertObject change = new InsertObject(insertOperationId, ids.get(0), changeValue);

		// call under test
		assertFalse(node.attemptInsert(change));
		ObjectNode expected = new ObjectNode().setId(ids.get(0));
		value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		value.put("two", ids.get(2));
		expected.setValue(value);
		assertEquals(expected, node);
	}

	@Test
	public void testAttemptInsertWithStartingNull() {
		ObjectNode node = new ObjectNode().setId(ids.get(0));
		node.setValue(null);

		Map<String, LogicalTimestamp> changeValue = new LinkedHashMap<>();
		changeValue.put("one", ids.get(1));
		InsertObject change = new InsertObject(insertOperationId, ids.get(0), changeValue);

		// call under test
		assertTrue(node.attemptInsert(change));
		ObjectNode expected = new ObjectNode().setId(ids.get(0));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		expected.setValue(value);
		assertEquals(expected, node);
	}
	
	@Test
	public void testAttemptInsertWithNewNodeIdLesserThanContainerId() {
		ObjectNode node = new ObjectNode().setId(ids.get(1));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(2));
		value.put("two", ids.get(3));
		node.setValue(value);

		Map<String, LogicalTimestamp> changeValue = new LinkedHashMap<>();
		// The new node ID is less than the container node ID
		changeValue.put("one", ids.get(0));
		InsertObject change = new InsertObject(insertOperationId, node.getId(), changeValue);

		// call under test
		assertFalse(node.attemptInsert(change));
		
		ObjectNode expected = new ObjectNode().setId(ids.get(1));
		
		value = new LinkedHashMap<>();
		value.put("one", ids.get(2));
		value.put("two", ids.get(3));
		expected.setValue(value);
		
		assertEquals(expected, node);
	}

	@Test
	public void testAttemptInsertWithWrongId() {
		ObjectNode node = new ObjectNode().setId(ids.get(0));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		node.setValue(value);

		Map<String, LogicalTimestamp> changeValue = new LinkedHashMap<>();
		changeValue.put("one", ids.get(1));
		InsertObject change = new InsertObject(ids.get(2), ids.get(2), changeValue);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			node.attemptInsert(change);
		}).getMessage();
		assertEquals("The ID of the passed change does not match the ID of this object.", message);
	}

	@Test
	public void testAttemptInsertWithNullChange() {
		ObjectNode node = new ObjectNode().setId(ids.get(0));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		node.setValue(value);

		InsertObject change = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			node.attemptInsert(change);
		}).getMessage();
		assertEquals("change is required.", message);
	}

	@Test
	public void testAttemptInsertWithSetNull() {
		ObjectNode node = new ObjectNode().setId(ids.get(0));
		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(1));
		node.setValue(value);

		Map<String, LogicalTimestamp> changeValue = new LinkedHashMap<>();
		changeValue.put("one", null);
		InsertObject change = new InsertObject(insertOperationId, ids.get(0), changeValue);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			node.attemptInsert(change);
		}).getMessage();
		assertEquals("Cannot set an object value to null", message);
	}

}
