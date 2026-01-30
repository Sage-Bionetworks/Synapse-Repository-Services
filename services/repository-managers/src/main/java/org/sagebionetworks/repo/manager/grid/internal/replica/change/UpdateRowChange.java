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

public class UpdateRowChange implements IntendedChange {
	
	private LogicalTimestamp rowVectorId;		// The id of the vector that contains the row to update
	private List<ConValue> rowData;					// The cells that needs updating
	private Integer[] rowVectorIndex; 			// For each cell in rowData, the vector index of the cell in the row vector.

	public UpdateRowChange(LogicalTimestamp rowVectorId, List<ConValue> rowData, Integer[] rowVectorIndex) {
		ValidateArgument.required(rowVectorId, "rowVectorId");
		ValidateArgument.required(rowData, "rowData");
		ValidateArgument.required(rowVectorIndex, "rowVectorIndex");
		ValidateArgument.requirement(rowData.size() == rowVectorIndex.length, "rowData and rowVectorIndex must have the same length");
		this.rowVectorId = rowVectorId;
		this.rowData = rowData;
		this.rowVectorIndex = rowVectorIndex;
	}
	
	public UpdateRowChange(JSONObject json) {
		this.rowVectorId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("r"));
		this.rowData = new ArrayList<>();
		json.getJSONArray("d").forEach(conValCompactArr -> {
			ConValue conValue = ConValue.fromCompact((JSONArray) conValCompactArr);
			this.rowData.add(conValue);
		});
		this.rowVectorIndex= json.getJSONArray("v").toList().stream().map(o -> (Integer)o).toArray(Integer[]::new);
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.update_row;
	}

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		
		json.put("r", LogicalTimestampCompactSerializable.serialize(rowVectorId));
		JSONArray data = new JSONArray();
		rowData.forEach(o -> data.put(o.toCompact()));
		json.put("d", data);
		json.put("v", new JSONArray(rowVectorIndex));
		
		return json;
	}

	public LogicalTimestamp getRowVectorId() {
		return rowVectorId;
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
		result = prime * result + ((rowData == null) ? 0 : rowData.toString().hashCode());
		result = prime * result + ((rowVectorId == null) ? 0 : rowVectorId.hashCode());
		result = prime * result + Arrays.hashCode(rowVectorIndex);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UpdateRowChange)) {
			return false;
		}
		UpdateRowChange other = (UpdateRowChange) obj;
		if (rowData == null) {
			if (other.rowData != null) {
				return false;
			}
		} else if (!rowData.toString().equals(other.rowData.toString())) {
			return false;
		}
		if (rowVectorId == null) {
			if (other.rowVectorId != null) {
				return false;
			}
		} else if (!rowVectorId.equals(other.rowVectorId)) {
			return false;
		}
		if (!Arrays.equals(rowVectorIndex, other.rowVectorIndex)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "UpdateRowChange [rowVectorId=" + rowVectorId + ", rowData=" + rowData + ", rowVectorIndex=" + Arrays.toString(rowVectorIndex) + "]";
	}


}
