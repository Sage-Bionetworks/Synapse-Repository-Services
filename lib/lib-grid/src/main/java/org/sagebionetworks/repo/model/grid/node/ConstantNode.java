package org.sagebionetworks.repo.model.grid.node;

import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONWriter;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

public class ConstantNode implements Node, HasJsonValue<ConstantNode> {

	/** a timestamp, which is the ID of the con node. */
	private LogicalTimestamp id;
	/** an optional JSON/CBOR value, which is the value of the con node when it is not undefined and is not a timestamp.
	 * Implementation note: when the value is `undefined`, this will be a Java `null`. When the value is a JSON `null`, this will be a JSONObject.NULL.
	 */
	private ConValue value;

	public ConValue getConValue() {
		return value;
	}

	public ConstantNode setValue(ConValue value) {
		if (value == null) {
			this.value = new ConValue(ConType.UNDEFINED, null);
			return this;
		}
		this.value = value;
		return this;
	}

	public ConstantNode setValue(Object value) {
		this.value = new ConValue(ConType.fromValue(value), value);
		return this;
	}

	public ConstantNode setId(LogicalTimestamp id) {
		this.id = id;
		return this;
	}

	public JSONArray toCompact() {
		JSONArray compact = new JSONArray();
		// All constants start with 0
		compact.put(0);
		// Next, the node's timestamp
		JSONArray nodeTimestamp = new JSONArray();
		nodeTimestamp.put(id.getReplicaId());
		nodeTimestamp.put(id.getSequenceNumber());
		compact.put(nodeTimestamp);
		// The last two values represent the value.
		JSONArray valueCompactArray = this.getConValue().toCompact();
		for (int i = 0; i < valueCompactArray.length(); i++) {
			compact.put(valueCompactArray.get(i));
		}
		return compact;
	}

	public ConstantNode fromCompact(JSONArray compactArray) {
		ValidateArgument.required(compactArray, "compactArray");
		ValidateArgument.requirement(compactArray.length() == 3 || compactArray.length() == 4, "must be 3 or 4 elements");
		ValidateArgument.requirement(compactArray.getInt(0) == 0, "first element must be 0 for ConstantNode");
		ValidateArgument.requirement(compactArray.getJSONArray(1).length() == 2, "second element must be a timestamp array of length 2");

		JSONArray nodeTimestamp = compactArray.getJSONArray(1);
		this.id = new LogicalTimestamp()
				.setReplicaId(nodeTimestamp.getLong(0))
				.setSequenceNumber(nodeTimestamp.getLong(1));
		// The value starts at index 2
		JSONArray valueCompactArray = new JSONArray();
		for (int i = 2; i < compactArray.length(); i++) {
			valueCompactArray.put(compactArray.get(i));
		}
		this.value = ConValue.fromCompact(valueCompactArray);
		return this;
	}


	@Override
	public ConstantNode setValueFromJson(String jsonString) {
		return fromCompact(new JSONArray(jsonString));
	}

	@Override
	public String getValueAsJson() {
		return JSONWriter.valueToString(this.toCompact());
	}

	@Override
	public LogicalTimestamp getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConstantNode other = (ConstantNode) obj;
		return Objects.equals(id, other.id) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "ConstantNode [id=" + id + ", value=" + value + "]";
	}

}
