package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class RowData {

	private JSONArray cells;
	private LogicalTimestamp vectorId;

	public JSONArray getCells() {
		return cells;
	}

	public RowData setCells(JSONArray data) {
		this.cells = data;
		return this;
	}

	String dataToJson() {
		return cells != null ? cells.toString() : null;
	}

	public LogicalTimestamp getVectorId() {
		return vectorId;
	}

	public RowData setVectorId(LogicalTimestamp vectorId) {
		this.vectorId = vectorId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dataToJson(), vectorId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowData other = (RowData) obj;
		return Objects.equals(dataToJson(), other.dataToJson()) && Objects.equals(vectorId, other.vectorId);
	}

	@Override
	public String toString() {
		return "RowData [data=" + cells + ", vectorId=" + vectorId + "]";
	}

}
