package org.sagebionetworks.repo.model.grid.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class ConstantNodeTest {

	private LogicalTimestamp id;

	@BeforeEach
	public void before() {
		id = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
	}

	@ParameterizedTest
	@MethodSource("validValues")
	public void testToAndFromJSON(Object value) {
		ConstantNode con = new ConstantNode().setId(id).setValue(value);
		// call under test
		String json = con.getValueAsJson();
		// call under test
		ConstantNode other = new ConstantNode().setId(id).setValueFromJson(json);
		assertEquals(con, other);
	}

	public static Stream<Object> validValues() {
		Object[] object = { 123, true, false, 4569999999999L, 3.14, "hello", new JSONArray("[1,2,3]"),
				new JSONObject("{\"key\":99}"), null };
		return Stream.of(object);
	}
	
	@Test
	public void testGetValueAsJson() {
		ConstantNode con = new ConstantNode().setId(id).setValue(new JSONObject("{\"key\":99}"));
		// call under test
		assertEquals("[{\"key\":99}]", con.getValueAsJson());
	}
	
	@Test
	public void testGetValueAsJsonWithNull() {
		ConstantNode con = new ConstantNode().setId(id).setValue(null);
		// call under test
		assertEquals("[]", con.getValueAsJson());
	}

}
