package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;

public class UpdateRowChange implements IntendedChange {

	private final LogicalTimestamp rowVectorId; // The id of the vector that contains the row to update
	private final List<ConValue> rowData; // The cells that needs updating
	private final Integer[] rowVectorIndex; // For each cell in rowData, the vector index of the cell in the row vector.
	private final LogicalTimestamp metadataObjectId;// The id of the metadata object.
	private final ConValue synapseRow; // For rows that include a SynapseRow

	public UpdateRowChange(LogicalTimestamp rowVectorId, List<ConValue> rowData, Integer[] rowVectorIndex) {
		this(rowVectorId, rowData, rowVectorIndex, null, null);
	}

	public UpdateRowChange(LogicalTimestamp rowVectorId, List<ConValue> rowData, Integer[] rowVectorIndex,
			LogicalTimestamp metadataObjectId, ConValue synapseRow) {
		ValidateArgument.required(rowVectorId, "rowVectorId");
		ValidateArgument.required(rowData, "rowData");
		ValidateArgument.required(rowVectorIndex, "rowVectorIndex");
		ValidateArgument.requirement(rowData.size() == rowVectorIndex.length,
				"rowData and rowVectorIndex must have the same length");
		if (synapseRow != null && metadataObjectId == null) {
			throw new IllegalArgumentException("metadataNodeId must be provided when synapseRow is provided.");
		}
		this.rowVectorId = rowVectorId;
		this.rowData = rowData;
		this.rowVectorIndex = rowVectorIndex;
		this.metadataObjectId = metadataObjectId;
		this.synapseRow = synapseRow;
	}

	public UpdateRowChange(JSONObject json) {
		this.rowVectorId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("r"));
		this.rowData = new ArrayList<>();
		json.getJSONArray("d").forEach(conValCompactArr -> {
			ConValue conValue = ConValue.fromCompact((JSONArray) conValCompactArr);
			this.rowData.add(conValue);
		});
		this.rowVectorIndex = json.getJSONArray("v").toList().stream().map(o -> (Integer) o).toArray(Integer[]::new);
		JSONArray metadataArray = json.optJSONArray("m");
		this.metadataObjectId = metadataArray != null ? LogicalTimestampCompactSerializable.deserialize(metadataArray)
				: null;
		JSONArray synArray = json.optJSONArray("s");
		this.synapseRow = synArray != null ? ConValue.fromCompact(synArray) : null;
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
		if (metadataObjectId != null) {
			json.put("m", LogicalTimestampCompactSerializable.serialize(metadataObjectId));
		}
		if (synapseRow != null) {
			json.put("s", synapseRow.toCompact());
		}
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

	public Optional<ConValue> getSynapseRow() {
		return Optional.ofNullable(synapseRow);
	}

	public Optional<LogicalTimestamp> getMetadataObjectId() {
		return Optional.ofNullable(metadataObjectId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(rowVectorIndex);
		result = prime * result + Objects.hash(metadataObjectId, rowData, rowVectorId, synapseRow);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateRowChange other = (UpdateRowChange) obj;
		return Objects.equals(metadataObjectId, other.metadataObjectId) && Objects.equals(rowData, other.rowData)
				&& Objects.equals(rowVectorId, other.rowVectorId) && Arrays.equals(rowVectorIndex, other.rowVectorIndex)
				&& Objects.equals(synapseRow, other.synapseRow);
	}

	@Override
	public String toString() {
		return "UpdateRowChange [rowVectorId=" + rowVectorId + ", rowData=" + rowData + ", rowVectorIndex="
				+ Arrays.toString(rowVectorIndex) + ", metadataObjectId=" + metadataObjectId + ", synapseRow="
				+ synapseRow + "]";
	}

}
