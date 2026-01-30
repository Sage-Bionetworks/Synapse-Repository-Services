package org.sagebionetworks.repo.model.grid.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

public class ConstantNodeTest {

	private LogicalTimestamp id;

	@BeforeEach
	public void before() {
		id = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
	}

	@ParameterizedTest
	@MethodSource("validValues")
	public void testToAndFromJSON(Object value) {
		ConstantNode con = new ConstantNode().setId(id).setValue(new ConValue(ConType.fromValue(value), value));
		// call under test
		String json = con.getValueAsJson();
		// call under test
		ConstantNode other = new ConstantNode().setId(id).setValueFromJson(json);
		assertEquals(con, other);
	}

	public static Stream<Object> validValues() {
		Object[] object = { null, JSONObject.NULL, 123, true, false, 4569999999999L, 3.14, "hello", new JSONArray("[1,2,3]"),
				new JSONObject("{\"key\":99}"), null };
		return Stream.of(object);
	}

	@Test
	public void testGetValueAsJsonWithObject() {
		ConstantNode con = new ConstantNode().setId(id).setValue(new ConValue(ConType.JSON_OBJECT, new JSONObject("{\"key\":99}")));
		// call under test
		assertEquals("[0,[1,2],{\"key\":99}]", con.getValueAsJson());
	}

	@Test
	public void testGetValueAsJsonWithString() {
		ConstantNode con = new ConstantNode().setId(id).setValue(new ConValue(ConType.STRING, "foo"));
		// call under test
		assertEquals("[0,[1,2],\"foo\"]", con.getValueAsJson());

	}

	@Test
	public void testGetValueAsJsonWithNull() {
		ConstantNode con = new ConstantNode().setId(id).setValue(new ConValue(ConType.NULL, JSONObject.NULL));
		// call under test
		assertEquals("[0,[1,2],null]", con.getValueAsJson());
	}

	@Test
	public void testGetValueAsJsonWithUndefined() {
		ConstantNode con = new ConstantNode().setId(id).setValue(new ConValue(ConType.UNDEFINED, null));
		// call under test
		assertEquals("[0,[1,2],0,0]", con.getValueAsJson());
	}

}
