package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;

public class JoinedRow {

	/**
	 * The grid row matched by the upsert key of a CSV row.
	 * 
	 * @param vectorId   the id of the vector holding the row's cells
	 * @param objectId   the id of the row object, null when unknown
	 * @param metadataId the id of the row's metadata object, null when the row has no metadata
	 * @param rowJson    the JSON document of all the row's cells, as the validation worker validates it; null when
	 *                   the import does not validate rows
	 */
	public record GridRow(LogicalTimestamp vectorId, LogicalTimestamp objectId, LogicalTimestamp metadataId, String rowJson) {

		/**
		 * The number of values {@link #writeValues(Object[], int)} writes.
		 */
		public static final int VALUE_COUNT = 4;

		/**
		 * @param includeRowJson whether to include the row's JSON document, needed only to validate the row
		 */
		public static GridRow fromRowView(RowView row, boolean includeRowJson) {
			return new GridRow(row.getRowObject().getData().getVectorId(), row.getRowObject().getObjectId(),
					row.getRowMetadataNodeId(),
					includeRowJson ? row.getRowObject().getData().getRowJsonDocument().toString() : null);
		}

		/**
		 * Writes the row as {@link #VALUE_COUNT} JSON values into {@code target}, starting at {@code offset}. Absent
		 * values are {@link JSONObject#NULL}; the row JSON stays a string so it is not parsed again.
		 */
		public void writeValues(Object[] target, int offset) {
			target[offset] = LogicalTimestampCompactSerializable.serialize(vectorId);
			target[offset + 1] = serialize(objectId);
			target[offset + 2] = serialize(metadataId);
			target[offset + 3] = rowJson == null ? JSONObject.NULL : rowJson;
		}

		/**
		 * @param values a JSON array of the values written by {@link #writeValues(Object[], int)}
		 */
		public static GridRow fromValues(JSONArray values) {
			return new GridRow(LogicalTimestampCompactSerializable.deserialize(values.getJSONArray(0)),
					deserialize(values.optJSONArray(1)), deserialize(values.optJSONArray(2)),
					values.isNull(3) ? null : values.getString(3));
		}

		public JSONObject rowJsonObject() {
			return new JSONObject(rowJson);
		}

		private static Object serialize(LogicalTimestamp id) {
			return id == null ? JSONObject.NULL : LogicalTimestampCompactSerializable.serialize(id);
		}

		private static LogicalTimestamp deserialize(JSONArray id) {
			return id == null ? null : LogicalTimestampCompactSerializable.deserialize(id);
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
