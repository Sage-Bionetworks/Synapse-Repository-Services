package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;

public class UpdateColumnNamesChange implements IntendedChange {

	private final LogicalTimestamp colunNamesVecId;
	private final Map<Integer, ConValue> indexToNameMap;

	public UpdateColumnNamesChange(LogicalTimestamp colunNamesVecId, Map<Integer, String> indexToNameMap) {
		ValidateArgument.required(colunNamesVecId, "colunNamesVecId");
		ValidateArgument.required(indexToNameMap, "indexToNameMap");
		this.colunNamesVecId = colunNamesVecId;
		this.indexToNameMap = indexToNameMap.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> new ConValue(ConType.STRING, e.getValue())));
	}

	public UpdateColumnNamesChange(JSONObject json) {
		ValidateArgument.required(json, "json");
		this.colunNamesVecId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("v"));
		JSONObject mapJson = json.getJSONObject("m");
		this.indexToNameMap = mapJson.keySet().stream()
				.collect(Collectors.toMap(Integer::parseInt, key -> ConValue.fromCompact(mapJson.getJSONArray(key))));
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.update_column_names;
	}

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("v", LogicalTimestampCompactSerializable.serialize(colunNamesVecId));
		JSONObject mapJson = new JSONObject();
		indexToNameMap.forEach((index, conValue) -> mapJson.put(index.toString(), conValue.toCompact()));
		json.put("m", mapJson);
		return json;
	}

	public LogicalTimestamp getColunNamesVecId() {
		return colunNamesVecId;
	}

	public Map<Integer, ConValue> getIndexToNameMap() {
		return indexToNameMap;
	}

	@Override
	public int hashCode() {
		return Objects.hash(colunNamesVecId, indexToNameMap);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateColumnNamesChange other = (UpdateColumnNamesChange) obj;
		return Objects.equals(colunNamesVecId, other.colunNamesVecId)
				&& Objects.equals(indexToNameMap, other.indexToNameMap);
	}

	@Override
	public String toString() {
		return "UpdateColumnNames [colunNamesVecId=" + colunNamesVecId + ", indexToNameMap=" + indexToNameMap + "]";
	}

}
