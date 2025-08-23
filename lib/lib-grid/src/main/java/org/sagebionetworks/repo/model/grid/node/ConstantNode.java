package org.sagebionetworks.repo.model.grid.node;

import java.util.Objects;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class ConstantNode implements Node, HasJsonValue<ConstantNode> {

	private LogicalTimestamp id;
	private Object value;

	public Object getValue() {
		return value;
	}

	public ConstantNode setValue(Object value) {
		this.value = value;
		return this;
	}

	public ConstantNode setId(LogicalTimestamp id) {
		this.id = id;
		return this;
	}

	@Override
	public ConstantNode setValueFromJson(String json) {
		this.value = "[]".equals(json) ? null : new JSONArray(json).get(0);
		return this;
	}

	@Override
	public String getValueAsJson() {
		return ConstantUtils.constantValueToJson(value);
	}

	@Override
	public LogicalTimestamp getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, getValueAsJson());
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
		return Objects.equals(id, other.id) && Objects.equals(getValueAsJson(), other.getValueAsJson());
	}

	@Override
	public String toString() {
		return "ConstantNode [id=" + id + ", value=" + value + "]";
	}

}
