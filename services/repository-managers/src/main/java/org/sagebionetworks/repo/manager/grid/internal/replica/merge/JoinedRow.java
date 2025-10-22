package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class JoinedRow {

	private JSONArray csvData;
	private LogicalTimestamp gridRowVecId;

	public JoinedRow(JSONArray csvData, LogicalTimestamp rowVectorId) {
		this.csvData = csvData;
		this.gridRowVecId = rowVectorId;
	}

	public JSONArray getCsvData() {
		return csvData;
	}

	public LogicalTimestamp getGridRowVecId() {
		return gridRowVecId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((csvData == null) ? 0 : csvData.toString().hashCode());
		result = prime * result + ((gridRowVecId == null) ? 0 : gridRowVecId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof JoinedRow)) {
			return false;
		}
		JoinedRow other = (JoinedRow) obj;
		if (csvData == null) {
			if (other.csvData != null) {
				return false;
			}
		} else if (!csvData.toString().equals(other.csvData.toString())) {
			return false;
		}
		if (gridRowVecId == null) {
			if (other.gridRowVecId != null) {
				return false;
			}
		} else if (!gridRowVecId.equals(other.gridRowVecId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "JoinedRow [csvData=" + csvData + ", gridRowVecId=" + gridRowVecId + "]";
	}

}
