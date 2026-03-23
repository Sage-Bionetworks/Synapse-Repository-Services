package org.sagebionetworks.repo.model.grid.node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;

public class VectorNode implements Node, HasJsonValue<VectorNode>, CanInsert<VectorNode> {

	private LogicalTimestamp id;
	private Map<Integer, ConstantNode> values;

	@Override
	public LogicalTimestamp getId() {
		return id;
	}

	@Override
	public Stream<LogicalTimestamp> streamReferencedTimestamps() {
		Stream<LogicalTimestamp> nodeIdStream = Stream.of(getId());

		if (values == null || values.isEmpty()) {
			return nodeIdStream;
		}

		Stream<LogicalTimestamp> constantIdStream = values.values().stream()
				.filter(Objects::nonNull)
				.map(ConstantNode::getId)
				.filter(Objects::nonNull);

		return Stream.concat(nodeIdStream, constantIdStream);
	}

	public Map<Integer, ConstantNode> getValues() {
		return values;
	}

	public VectorNode setValues(Map<Integer, ConstantNode> values) {
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
			JSONObject constantNodeAsJson = ob.getJSONObject(k);
			Integer intKey = Integer.valueOf(k.substring(1));
			values.put(intKey, getConstantNodeFromVectorNodeJson(constantNodeAsJson));
		});
		return this;
	}

	public static ConstantNode getConstantNodeFromVectorNodeJson(JSONObject constantNode) {
		return new ConstantNode().setId(LogicalTimestampCompactSerializable.deserialize(constantNode.getJSONArray("i")))
				.setValue(ConValue.fromCompact(constantNode.optJSONArray("v")));
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
				ob.put(String.format("c%s", k), sub);
				sub.put("v", v.getConValue().toCompact());
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
		for (Map.Entry<Integer, ConstantNode> entry : change.getValues().entrySet()) {
			Integer key = entry.getKey();
			ConstantNode changeNode = entry.getValue();			
			if (changeNode == null) {
				throw new IllegalArgumentException("Cannot set a vector index to null");
			}			
			// The ID of the new value must be greater than the container node (to avoid circular references), otherwise the insertion is ignored (See https://sagebionetworks.jira.com/browse/PLFM-9273).
			if (changeNode.getId().compareTo(this.id) <= 0) {
				continue;
			}	
			ConstantNode thisNode = this.values.get(key);
			// The ID of the new value must be greater than the logical clock of the current value, otherwise the insertion is ignored.
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
