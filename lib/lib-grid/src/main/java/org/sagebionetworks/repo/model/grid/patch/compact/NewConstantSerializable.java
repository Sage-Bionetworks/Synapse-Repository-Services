package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class NewConstantSerializable implements OperationSerializable<NewConstant> {

	@Override
	public NewConstant deserialize(LogicalTimestamp id, JSONArray array) {
		NewConstant con = new NewConstant().setOperationId(id);
		if (array.length() == 1) {
			return con.setValue(new ConValue(ConType.UNDEFINED, null));
		}
		if (array.length() == 3) {
			con.setTimestamp(array.getBoolean(2));
		}
		if (array.isNull(1)) {
			return con.setValue(new ConValue(ConType.NULL, null));
		}

		Object value = array.get(1);
		if (value instanceof Boolean) {
			return con.setValue(new ConValue(ConType.BOOLEAN, value));
		} else if (value instanceof Double) {
			return con.setValue(new ConValue(ConType.DOUBLE, value));
		} else if (value instanceof Float) {
			return con.setValue(new ConValue(ConType.DOUBLE, (Double) value));
		} else if (value instanceof Integer) {
			Long longValue = ((Integer) value).longValue();
			if (con.isTimestamp()) {
				return con.setValue(new ConValue(ConType.TIMESTAMP,
						new LogicalTimestamp().setReplicaId(id.getReplicaId()).setSequenceNumber(longValue)));
			} else {
				return con.setValue(new ConValue(ConType.LONG, longValue));
			}
		} else if (value instanceof Long) {
			if (con.isTimestamp()) {
				return con.setValue(new ConValue(ConType.TIMESTAMP,
						new LogicalTimestamp().setReplicaId(id.getReplicaId()).setSequenceNumber((Long) value)));
			} else {
				return con.setValue(new ConValue(ConType.LONG, value));
			}
		} else if (value instanceof String) {
			return con.setValue(new ConValue(ConType.STRING, value));
		} else if (value instanceof JSONArray) {
			if (con.isTimestamp()) {
				return con.setValue(new ConValue(ConType.TIMESTAMP,
						LogicalTimestampCompactSerializable.deserialize((JSONArray) value)));
			} else {
				return con.setValue(new ConValue(ConType.JSON_ARRAY, value));
			}
		} else if (value instanceof JSONObject) {
			return con.setValue(new ConValue(ConType.JSON_OBJECT, value));
		} else {
			throw new IllegalArgumentException("Unknown constant type: " + array.toString());
		}
	}

	@Override
	public JSONArray serialize(NewConstant con) {
		JSONArray array = new JSONArray().put(OperationType.new_con.getCode());
		ConType type = con.getValue().getType();
		switch (type) {
		case BOOLEAN:
		case DOUBLE:
		case LONG:
		case STRING:
		case JSON_ARRAY:
		case JSON_OBJECT:
			return array.put(con.getValue().getValue());
		case NULL:
			return array.put((Object) null);
		case TIMESTAMP:
			LogicalTimestamp timestamp = (LogicalTimestamp) con.getValue().getValue();
			if (con.getOperationId().getReplicaId().equals(timestamp.getReplicaId())) {
				return array.put(timestamp.getSequenceNumber()).put(true);
			} else {
				return array.put(LogicalTimestampCompactSerializable.serialize(timestamp)).put(true);
			}
		case UNDEFINED:
			return array;
		default:
			throw new IllegalArgumentException("Unknown con type: " + type);

		}
	}

	@Override
	public OperationType getType() {
		return OperationType.new_con;
	}

	@Override
	public Class<? extends NewConstant> getTypeClass() {
		return NewConstant.class;
	}

}
