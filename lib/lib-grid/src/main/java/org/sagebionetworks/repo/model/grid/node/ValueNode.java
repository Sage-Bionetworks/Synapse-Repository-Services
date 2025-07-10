package org.sagebionetworks.repo.model.grid.node;

import java.util.Objects;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;
import org.sagebionetworks.util.ValidateArgument;

public class ValueNode implements Node, HasJsonValue<ValueNode>, CanInsert<InsertValue> {

	private LogicalTimestamp id;
	private LogicalTimestamp value;

	@Override
	public ValueNode setValueFromJson(String json) {
		value = "[]".equals(json) ? null : LogicalTimestampCompactSerializable.deserialize(new JSONArray(json));
		return this;
	}

	@Override
	public String getValueAsJson() {
		if (value == null) {
			return "[]";
		}
		return LogicalTimestampCompactSerializable.serialize(value).toString();
	}

	@Override
	public LogicalTimestamp getId() {
		return id;
	}

	public LogicalTimestamp getValue() {
		return value;
	}

	public ValueNode setValue(LogicalTimestamp value) {
		this.value = value;
		return this;
	}

	public ValueNode setId(LogicalTimestamp id) {
		this.id = id;
		return this;
	}

	@Override
	public boolean attemptInsert(InsertValue change) {
		ValidateArgument.required(change, "change");
		ValidateArgument.required(change.getValueId(), "change.valueId");
		ValidateArgument.required(change.getReferenceId(), "change.referenceId");
		if (!change.getValueId().equals(this.id)) {
			throw new IllegalArgumentException("The ID of the passed change does not match the ID of this value.");
		}
		if (this.value == null || change.getReferenceId().compareTo(this.value) > 0) {
			this.value = LogicalTimestamp.clone(change.getReferenceId());
			return true;
		}
		return false;
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
		ValueNode other = (ValueNode) obj;
		return Objects.equals(id, other.id) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "ValueNode [id=" + id + ", value=" + value + "]";
	}

}
