package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.List;
import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class JoinedRow {

	/**
	 * The grid row matched by the upsert key of a CSV row.
	 * 
	 * @param vectorId   the id of the vector holding the row's cells
	 * @param objectId   the id of the row object
	 * @param metadataId the id of the row's metadata object, null when the row has no metadata
	 * @param rowJson    the JSON document of all the row's cells, as validated against the grid's schema
	 */
	public record GridRow(LogicalTimestamp vectorId, LogicalTimestamp objectId, LogicalTimestamp metadataId, String rowJson) {

		public JSONObject rowJsonObject() {
			return new JSONObject(rowJson);
		}
	}

	private final List<ConValue> csvData;
	private final GridRow gridRow;

	/**
	 * @param csvData the CSV row's cells, in column mapping order
	 * @param gridRow the matched grid row, null when the CSV row is new
	 */
	public JoinedRow(List<ConValue> csvData, GridRow gridRow) {
		this.csvData = csvData;
		this.gridRow = gridRow;
	}

	public List<ConValue> getCsvData() {
		return csvData;
	}

	public GridRow getGridRow() {
		return gridRow;
	}

	public LogicalTimestamp getGridRowVecId() {
		return gridRow == null ? null : gridRow.vectorId();
	}

	@Override
	public int hashCode() {
		return Objects.hash(csvData == null ? null : csvData.toString(), gridRow);
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
		// ConValue cells are compared by their string form, so that numbers of different Java types compare equal
		return Objects.equals(csvData == null ? null : csvData.toString(), other.csvData == null ? null : other.csvData.toString())
				&& Objects.equals(gridRow, other.gridRow);
	}

	@Override
	public String toString() {
		return "JoinedRow [csvData=" + csvData + ", gridRow=" + gridRow + "]";
	}

}
