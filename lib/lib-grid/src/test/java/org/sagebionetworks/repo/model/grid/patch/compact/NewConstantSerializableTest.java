package org.sagebionetworks.repo.model.grid.patch.compact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;

public class NewConstantSerializableTest {

	private LogicalTimestamp id;
	private NewConstantSerializable serializable;

	@BeforeEach
	public void before() {
		id = new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(8L);
		serializable = new NewConstantSerializable();
	}

	@Test
	public void testRoundTripWithUndefined() {
		String json = "[0]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, new ConValue(ConType.UNDEFINED, null));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithNull() {
		String json = "[0,null]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, new ConValue(ConType.NULL, null));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithBoolean() {
		String json = "[0,true]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, new ConValue(ConType.BOOLEAN, true));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithBooleanFalse() {
		String json = "[0,false]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, new ConValue(ConType.BOOLEAN, false));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithDouble() {
		String json = "[0,3.14]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, new ConValue(ConType.DOUBLE, 3.14));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithLong() {
		String json = "[0,12345]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, new ConValue(ConType.LONG, 12345L));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithLongNegative() {
		String json = "[0,-12345]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, (new ConValue(ConType.LONG, -12345L)));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithLongLong() {
		String json = "[0,1234567891011121314]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, new ConValue(ConType.LONG, 1234567891011121314L));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithString() {
		String json = "[0,\"abcdef\"]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		NewConstant expected = new NewConstant(id, new ConValue(ConType.STRING, "abcdef"));
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithArray() {
		String json = "[0,[1,2,3]]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		assertEquals(id, con.getOperationId());
		assertEquals(ConType.JSON_ARRAY, con.getValue().getType());
		assertEquals("[1,2,3]", con.getValue().getValue().toString());

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithJsonObject() {
		String json = "[0,{\"key\":99}]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		assertEquals(id, con.getOperationId());
		assertEquals(ConType.JSON_OBJECT, con.getValue().getType());
		assertEquals("{\"key\":99}", con.getValue().getValue().toString());

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithTimestampWithNoReplicaMatch() {
		String json = "[0,[3,5],true]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		assertEquals(id, con.getOperationId());
		assertEquals(ConType.TIMESTAMP, con.getValue().getType());
		NewConstant expected = new NewConstant(
				id,
				new ConValue(ConType.TIMESTAMP, new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(5L))
		);
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithTimestampWithReplicaMatch() {
		String json = "[0,5,true]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		assertEquals(id, con.getOperationId());
		assertEquals(ConType.TIMESTAMP, con.getValue().getType());
		NewConstant expected = new NewConstant(
				id,
				new ConValue(ConType.TIMESTAMP, new LogicalTimestamp().setReplicaId(id.getReplicaId()).setSequenceNumber(5L))
		);
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}
	
	@Test
	public void testRoundTripWithTimestampWithReplicaMatchLarge() {
		String json = "[0,1234567891011121314,true]";

		// call under test
		NewConstant con = serializable.deserialize(id, new JSONArray(json));
		assertEquals(id, con.getOperationId());
		assertEquals(ConType.TIMESTAMP, con.getValue().getType());
		NewConstant expected = new NewConstant(
				id,
				new ConValue(ConType.TIMESTAMP,
						new LogicalTimestamp().setReplicaId(id.getReplicaId()).setSequenceNumber(1234567891011121314L))
		);
		assertEquals(expected, con);

		// call under test
		String back = serializable.serialize(con).toString();
		assertEquals(json, back);
	}

}
