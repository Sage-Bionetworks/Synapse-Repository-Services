package org.sagebionetworks.repo.model.grid.node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;

public class VectorNode implements Node, HasJsonValue<VectorNode>, CanInsert<VectorNode> {

	private LogicalTimestamp id;
	private Map<String, ConstantNode> values;

	@Override
	public LogicalTimestamp getId() {
		return id;
	}

	public Map<String, ConstantNode> getValues() {
		return values;
	}

	public VectorNode setValues(Map<String, ConstantNode> values) {
		this.values = values;
		return this;
	}

	public VectorNode setId(LogicalTimestamp id) {
		this.id = id;
		return this;
	}

	@Override
	public VectorNode setValueFromJson(String json) {
		if ("{}".equals(json)) {
			values = null;
			return this;
		}
		JSONObject ob = new JSONObject(json);
		this.values = new LinkedHashMap<>(ob.length());
		ob.keySet().forEach(k -> {
			JSONObject sub = ob.getJSONObject(k);
			values.put(k,
					new ConstantNode().setId(LogicalTimestampCompactSerializable.deserialize(sub.getJSONArray("i")))
							.setValue(sub.opt("v")));
		});
		return this;
	}

	@Override
	public String getValueAsJson() {
		if (values == null) {
			return "{}";
		}
		JSONObject ob = new JSONObject();
		values.forEach((k, v) -> {
			if (v != null) {
				JSONObject sub = new JSONObject();
				ob.put(k, sub);
				if (v.getValue() != null) {
					sub.put("v", v.getValue());
				}
				sub.put("i", LogicalTimestampCompactSerializable.serialize(v.getId()));
			}
		});
		return ob.toString();
	}

	@Override
	public boolean attemptInsert(VectorNode change) {
		ValidateArgument.required(change, "change");
		ValidateArgument.required(change.getId(), "change.id");
		if (!change.getId().equals(this.id)) {
			throw new IllegalArgumentException("The ID of the passed change does not match the ID of this object.");
		}
		if (change.getValues() == null) {
			return false;
		}
		if (this.values == null) {
			this.values = new LinkedHashMap<>();
		}
		boolean wasChanged = false;
		for (Map.Entry<String, ConstantNode> entry : change.getValues().entrySet()) {
			String key = entry.getKey();
			ConstantNode changeNode = entry.getValue();
			if (changeNode == null) {
				throw new IllegalArgumentException("Cannot set a vector index to null");
			}
			ConstantNode thisNode = this.values.get(key);
			if (thisNode == null || changeNode.getId().compareTo(thisNode.getId()) > 0) {
				this.values.put(key, changeNode);
				wasChanged = true;
			}
		}
		return wasChanged;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, values);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VectorNode other = (VectorNode) obj;
		return Objects.equals(id, other.id) && Objects.equals(values, other.values);
	}

	@Override
	public String toString() {
		return "VectorNode [id=" + id + ", values=" + values + "]";
	}

}
