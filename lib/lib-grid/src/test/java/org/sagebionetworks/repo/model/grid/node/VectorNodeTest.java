package org.sagebionetworks.repo.model.grid.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class VectorNodeTest {
	private LogicalTimestamp id;
	private LogicalTimestamp id2;
	private LogicalTimestamp id3;
	private LogicalTimestamp id4;
	private LogicalTimestamp id5;

	@BeforeEach
	public void before() {
		id = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		id2 = new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L);
		id3 = new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L);
		id4 = new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L);
		id5 = new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L);
	}

	@ParameterizedTest
	@MethodSource("validValues")
	public void testToAndFromJSON(Object value) {
		VectorNode vec = new VectorNode().setId(id).setValues(new LinkedHashMap<>());
		vec.getValues().put(1, new ConstantNode().setId(id2).setValue(value));
		vec.getValues().put(2, new ConstantNode().setId(id3).setValue("other value"));
		// call under test
		String json = vec.getValueAsJson();
		// call under test
		VectorNode other = new VectorNode().setId(id).setValueFromJson(json);
		assertEquals(vec, other);
	}

	public static Stream<Object> validValues() {
		Object[] object = { 123, true, false, 4569999999999L, 3.14, "hello", new JSONArray("[1,2,3]"),
				new JSONObject("{\"key\":99}"), null };
		return Stream.of(object);
	}

	@Test
	public void testWithNullMap() {
		VectorNode vec = new VectorNode().setId(id).setValues(null);
		String json = vec.getValueAsJson();
		assertEquals("{}", json);
		VectorNode other = new VectorNode().setId(id).setValueFromJson(json);
		assertEquals(vec, other);
	}

	@Test
	public void testGetValueAsJson() {
		VectorNode vec = new VectorNode().setId(id).setValues(new LinkedHashMap<>());
		vec.getValues().put(1, new ConstantNode().setId(id2).setValue(new ConValue(ConType.JSON_ARRAY, new JSONArray("[1,2,3]"))));
		vec.getValues().put(2, new ConstantNode().setId(id3).setValue(new ConValue(ConType.STRING, "other value")));
		vec.getValues().put(3, new ConstantNode().setId(id4).setValue(new ConValue(ConType.NULL, null)));
		vec.getValues().put(4, new ConstantNode().setId(id5).setValue(new ConValue(ConType.UNDEFINED, null)));
		String json = vec.getValueAsJson();
		assertEquals("{\"c1\":{\"v\":[[1,2,3]],\"i\":[3,4]},\"c2\":{\"v\":[\"other value\"],\"i\":[5,6]},\"c3\":{\"v\":[null],\"i\":[7,8]},\"c4\":{\"v\":[0,0],\"i\":[9,10]}}", json);
		VectorNode other = new VectorNode().setId(id).setValueFromJson(json);
		assertEquals(vec, other);
	}

	@Test
	public void testAttemptInsert() {
		VectorNode vec = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		VectorNode update = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[222],\"i\":[4,4]}}");
		// call under test
		assertTrue(vec.attemptInsert(update));
		VectorNode expected = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[222],\"i\":[4,4]}}");
		assertEquals(expected, vec);
	}

	@Test
	public void testAttemptInsertWithCurrentValuesNull() {
		VectorNode vec = new VectorNode().setId(id).setValues(null);
		VectorNode update = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[222],\"i\":[4,4]}}");
		// call under test
		assertTrue(vec.attemptInsert(update));
		VectorNode expected = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[222],\"i\":[4,4]}}");
		assertEquals(expected, vec);
	}

	@Test
	public void testAttemptInsertWithOtherValuesNull() {
		VectorNode vec = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		VectorNode update = new VectorNode().setId(id).setValues(null);
		// call under test
		assertFalse(vec.attemptInsert(update));
		VectorNode expected = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		assertEquals(expected, vec);
	}

	@Test
	public void testAttemptInsertWithSameId() {
		VectorNode vec = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		VectorNode update = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[222],\"i\":[3,4]}}");
		// call under test
		assertFalse(vec.attemptInsert(update));
		VectorNode expected = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		assertEquals(expected, vec);
	}

	@Test
	public void testAttemptInsertWithOlderId() {
		VectorNode vec = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		VectorNode update = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[222],\"i\":[2,4]}}");
		// call under test
		assertFalse(vec.attemptInsert(update));
		VectorNode expected = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		assertEquals(expected, vec);
	}

	@Test
	public void testAttemptInsertWithNewIndex() {
		VectorNode vec = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		VectorNode update = new VectorNode().setId(id).setValueFromJson("{\"c1\":{\"v\":[222],\"i\":[4,4]}}");
		// call under test
		assertTrue(vec.attemptInsert(update));
		VectorNode expected = new VectorNode().setId(id)
				.setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]},\"c1\":{\"v\":[222],\"i\":[4,4]}}");
		assertEquals(expected, vec);
	}
	
	@Test
	public void testAttemptInsertWithNewNodeIdLesserThanContainerId() {
		VectorNode vec = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		// The new node ID is less than the container node ID
		VectorNode update = new VectorNode().setId(id).setValueFromJson("{\"c1\":{\"v\":[222],\"i\":[1,1]}}");
		// call under test
		assertFalse(vec.attemptInsert(update));
		VectorNode expected = new VectorNode().setId(id)
				.setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		assertEquals(expected, vec);
	}

	@Test
	public void testAttemptInsertWithWrongId() {
		VectorNode vec = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		VectorNode update = new VectorNode().setId(new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(12L))
				.setValues(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			assertTrue(vec.attemptInsert(update));
		}).getMessage();
		assertEquals("The ID of the passed change does not match the ID of this object.", message);
	}

	@Test
	public void testAttemptInsertWithNullValue() {
		VectorNode vec = new VectorNode().setId(id).setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[3,4]}}");
		VectorNode update = new VectorNode().setId(id).setValues(new LinkedHashMap<>());
		update.getValues().put(1, null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			assertTrue(vec.attemptInsert(update));
		}).getMessage();
		assertEquals("Cannot set a vector index to null", message);
	}

	@Test
	public void testStreamReferencedTimestampsWithValues() {
		VectorNode vec = new VectorNode().setId(id).setValues(new LinkedHashMap<>());
		vec.getValues().put(0, new ConstantNode().setId(id2).setValue("value1"));
		vec.getValues().put(1, new ConstantNode().setId(id3).setValue("value2"));
		vec.getValues().put(2, new ConstantNode().setId(id4).setValue("value3"));

		// call under test
		List<LogicalTimestamp> timestamps = vec.streamReferencedTimestamps().collect(Collectors.toList());

		assertEquals(4, timestamps.size());
		assertEquals(id, timestamps.get(0)); // node ID first
		assertEquals(id2, timestamps.get(1));
		assertEquals(id3, timestamps.get(2));
		assertEquals(id4, timestamps.get(3));
	}

	@Test
	public void testStreamReferencedTimestampsWithNullValues() {
		VectorNode vec = new VectorNode().setId(id).setValues(null);

		// call under test
		List<LogicalTimestamp> timestamps = vec.streamReferencedTimestamps().collect(Collectors.toList());

		assertEquals(1, timestamps.size());
		assertEquals(id, timestamps.get(0)); // only node ID
	}

	@Test
	public void testStreamReferencedTimestampsWithEmptyValues() {
		VectorNode vec = new VectorNode().setId(id).setValues(new LinkedHashMap<>());

		// call under test
		List<LogicalTimestamp> timestamps = vec.streamReferencedTimestamps().collect(Collectors.toList());

		assertEquals(1, timestamps.size());
		assertEquals(id, timestamps.get(0)); // only node ID
	}

	@Test
	public void testStreamReferencedTimestampsWithNullConstantNode() {
		VectorNode vec = new VectorNode().setId(id).setValues(new LinkedHashMap<>());
		vec.getValues().put(0, new ConstantNode().setId(id2).setValue("value1"));
		vec.getValues().put(1, null); // null node
		vec.getValues().put(2, new ConstantNode().setId(id3).setValue("value2"));

		// call under test
		List<LogicalTimestamp> timestamps = vec.streamReferencedTimestamps().collect(Collectors.toList());

		assertEquals(3, timestamps.size());
		assertEquals(id, timestamps.get(0));
		assertEquals(id2, timestamps.get(1));
		assertEquals(id3, timestamps.get(2)); // null was filtered out
	}

	@Test
	public void testStreamReferencedTimestampsWithNullConstantId() {
		VectorNode vec = new VectorNode().setId(id).setValues(new LinkedHashMap<>());
		vec.getValues().put(0, new ConstantNode().setId(id2).setValue("value1"));
		vec.getValues().put(1, new ConstantNode().setId(null).setValue("value2")); // null ID
		vec.getValues().put(2, new ConstantNode().setId(id3).setValue("value3"));

		// call under test
		List<LogicalTimestamp> timestamps = vec.streamReferencedTimestamps().collect(Collectors.toList());

		assertEquals(3, timestamps.size());
		assertEquals(id, timestamps.get(0));
		assertEquals(id2, timestamps.get(1));
		assertEquals(id3, timestamps.get(2)); // null ID was filtered out
	}
}
