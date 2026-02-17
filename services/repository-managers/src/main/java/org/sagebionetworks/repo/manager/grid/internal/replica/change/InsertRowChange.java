package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;

public class InsertRowChange implements IntendedChange {

	private LogicalTimestamp rowsArrayId; // The id of the rows array to add the row to
	private LogicalTimestamp nodeRefId; // The optional id of the node in the array after which the row should be
										// inserted
	private List<ConValue> rowData; // The actual row data
	private Integer[] rowVectorIndex; // The vector index of each column in the row data
	private SynapseRow synapseRow;

	public InsertRowChange(LogicalTimestamp rowsArrayId, LogicalTimestamp nodeRefId, List<ConValue> rowData,
			Integer[] rowVectorIndex) {
		this(rowsArrayId, nodeRefId, rowData, rowVectorIndex, null);
	}

	public InsertRowChange(LogicalTimestamp rowsArrayId, LogicalTimestamp nodeRefId, List<ConValue> rowData,
			Integer[] rowVectorIndex, SynapseRow synRow) {
		ValidateArgument.required(rowsArrayId, "rowsArrayId");
		ValidateArgument.required(rowData, "rowData");
		ValidateArgument.required(rowVectorIndex, "rowVectorIndex");
		ValidateArgument.requirement(rowData.size() == rowVectorIndex.length,
				"rowData and rowVectorIndex must have the same length");

		this.rowsArrayId = rowsArrayId;
		this.nodeRefId = nodeRefId;
		this.rowData = rowData;
		this.rowVectorIndex = rowVectorIndex;
		this.synapseRow = synRow;
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
		this.rowVectorIndex = json.getJSONArray("v").toList().stream().map(v -> (Integer) v).toArray(Integer[]::new);
		JSONArray synArray = json.optJSONArray("s");
		if(synArray != null) {
			this.synapseRow = new SynapseRow().setFromJSONArray(synArray);
		}
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

		if (synapseRow != null) {
			json.put("s", synapseRow.toJSONArray());
		}

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

	public SynapseRow getSynapseRow() {
		return synapseRow;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(rowVectorIndex);
		result = prime * result + Objects.hash(nodeRefId, rowData, rowsArrayId, synapseRow);
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
		InsertRowChange other = (InsertRowChange) obj;
		return Objects.equals(nodeRefId, other.nodeRefId) && Objects.equals(rowData, other.rowData)
				&& Arrays.equals(rowVectorIndex, other.rowVectorIndex) && Objects.equals(rowsArrayId, other.rowsArrayId)
				&& Objects.equals(synapseRow, other.synapseRow);
	}

	@Override
	public String toString() {
		return "InsertRowChange [rowsArrayId=" + rowsArrayId + ", nodeRefId=" + nodeRefId + ", rowData=" + rowData
				+ ", rowVectorIndex=" + Arrays.toString(rowVectorIndex) + ", synapseRow=" + synapseRow + "]";
	}

}
