package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;

public class InsertRowChange implements IntendedChange {

	private LogicalTimestamp rowsArrayId; // The id of the rows array to add the row to
	private LogicalTimestamp nodeRefId; // The optional id of the node in the array after which the row should be inserted
	private List<ConValue> rowData; // The actual row data
	private Integer[] rowVectorIndex; // The vector index of each column in the row data

	public InsertRowChange(LogicalTimestamp rowsArrayId, LogicalTimestamp nodeRefId, List<ConValue> rowData, Integer[] rowVectorIndex) {
		ValidateArgument.required(rowsArrayId, "rowsArrayId");
		ValidateArgument.required(rowData, "rowData");
		ValidateArgument.required(rowVectorIndex, "rowVectorIndex");
		ValidateArgument.requirement(rowData.size() == rowVectorIndex.length, "rowData and rowVectorIndex must have the same length");
		
		this.rowsArrayId = rowsArrayId;
		this.nodeRefId = nodeRefId;
		this.rowData = rowData;
		this.rowVectorIndex = rowVectorIndex;
	}
	
	public InsertRowChange(JSONObject json) {
		this.rowsArrayId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("a"));
		JSONArray nodeRefId = json.optJSONArray("n");
		if (nodeRefId != null) {
			this.nodeRefId = LogicalTimestampCompactSerializable.deserialize(nodeRefId);
		}
		this.rowData = new ArrayList<>();
		json.getJSONArray("d").forEach(conValCompactArr -> {
			ConValue conValue = ConValue.fromCompact((JSONArray) conValCompactArr);
			this.rowData.add(conValue);
		});
		this.rowVectorIndex = json.getJSONArray("v").toList().stream().map(v -> (Integer)v).toArray(Integer[]::new);
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.insert_row;
	}

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		
		json.put("a", LogicalTimestampCompactSerializable.serialize(rowsArrayId));
		
		if (nodeRefId != null) {
			json.put("n", LogicalTimestampCompactSerializable.serialize(nodeRefId));
		}

		JSONArray data = new JSONArray();
		rowData.forEach(o -> data.put(o.toCompact()));
		json.put("d", data);
		json.put("v", new JSONArray(rowVectorIndex));
		
		return json;
	}
	
	public LogicalTimestamp getRowsArrayId() {
		return rowsArrayId;
	}
	
	public LogicalTimestamp getNodeRefId() {
		return nodeRefId;
	}
	
	public List<ConValue> getRowData() {
		return rowData;
	}
	
	public Integer[] getRowVectorIndex() {
		return rowVectorIndex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodeRefId == null) ? 0 : nodeRefId.hashCode());
		result = prime * result + ((rowData == null) ? 0 : rowData.toString().hashCode());
		result = prime * result + Arrays.hashCode(rowVectorIndex);
		result = prime * result + ((rowsArrayId == null) ? 0 : rowsArrayId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof InsertRowChange)) {
			return false;
		}
		InsertRowChange other = (InsertRowChange) obj;
		if (nodeRefId == null) {
			if (other.nodeRefId != null) {
				return false;
			}
		} else if (!nodeRefId.equals(other.nodeRefId)) {
			return false;
		}
		if (rowData == null) {
			if (other.rowData != null) {
				return false;
			}
		} else if (!rowData.toString().equals(other.rowData.toString())) {
			return false;
		}
		if (!Arrays.equals(rowVectorIndex, other.rowVectorIndex)) {
			return false;
		}
		if (rowsArrayId == null) {
			if (other.rowsArrayId != null) {
				return false;
			}
		} else if (!rowsArrayId.equals(other.rowsArrayId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "InsertRowChange [rowsArrayId=" + rowsArrayId + ", nodeRefId=" + nodeRefId + ", rowData=" + rowData + ", rowVectorIndex=" + Arrays.toString(rowVectorIndex) + "]";
	}
	
}
