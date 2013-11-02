package org.sagebionetworks.repo.manager.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Utilities for working with Tables and Row data.
 * 
 * @author jmhill
 * 
 */
public class TableModelUtils {

	private static final String INVALID_VALUE_TEMPLATE = "Value at [%1$s,%2$s] was not a valid %3$s. %4$s";
	/**
	 * Sets the maxiumn string length for a string value in a table.
	 */
	public static final int MAX_STRING_LENGTH = 2000;

	/**
	 * This utility will validate and convert the passed RowSet to an output
	 * CSV.
	 * 
	 * @param models
	 * @param set
	 */
	public static void validateAndWriteToCSV(List<ColumnModel> models,
			RowSet set, CSVWriter out) {
		if (models == null)
			throw new IllegalArgumentException("Models cannot be null");
		if (set == null)
			throw new IllegalArgumentException("RowSet cannot be null");
		;
		if (set.getHeaders() == null)
			throw new IllegalArgumentException("RowSet.headers cannot be null");
		if (set.getRows() == null)
			throw new IllegalArgumentException("RowSet.rows cannot be null");
		if (set.getRows().size() < 1)
			throw new IllegalArgumentException(
					"RowSet.rows must contain at least one row.");
		if (out == null)
			throw new IllegalArgumentException("CSVWriter cannot be null");
		if (models.size() != set.getHeaders().size())
			throw new IllegalArgumentException(
					"RowSet.headers size must be equal to the number of columns in the table.  The table has :"
							+ models.size()
							+ " columns and the passed RowSet.headers has: "
							+ set.getHeaders().size());
		// Now map the index of each column
		Map<String, Integer> columnIndexMap = new HashMap<String, Integer>();
		int index = 0;
		for (String header : set.getHeaders()) {
			columnIndexMap.put(header, index);
			index++;
		}
		// Process each row
		int count = 0;
		for (Row row : set.getRows()) {
			// First convert the values to
			if (row.getValues() == null)
				throw new IllegalArgumentException("Row " + count
						+ " has null list of values");
			if (models.size() != row.getValues().size())
				throw new IllegalArgumentException(
						"Row.value size must be equal to the number of columns in the table.  The table has :"
								+ models.size()
								+ " columns and the passed Row.value has: "
								+ row.getValues().size()
								+ " for row number: "
								+ count);
			// Convert the values to an array for quick lookup
			String[] values = row.getValues().toArray(
					new String[row.getValues().size()]);
			// Prepare the final array which includes the ID and version number
			// as the first two columns.
			String[] finalRow = new String[values.length + 2];
			if (row.getRowId() == null)
				throw new IllegalArgumentException(
						"Row.rowId cannot be null for row number: " + count);
			if (row.getVersionNumber() == null)
				throw new IllegalArgumentException(
						"Row.versionNumber cannot be null for row number: "
								+ count);
			finalRow[0] = row.getRowId().toString();
			finalRow[1] = row.getVersionNumber().toString();
			// Now process all of the columns as defined by the schema
			for (int i = 0; i < models.size(); i++) {
				ColumnModel cm = models.get(i);
				Integer valueIndex = columnIndexMap.get(cm.getId());
				if (valueIndex == null)
					throw new IllegalArgumentException(
							"The Table's ColumnModels includes: name="
									+ cm.getName()
									+ " with id="
									+ cm.getId()
									+ " but "
									+ cm.getId()
									+ " was not found in the headers of the RowResults");
				// Get the value
				String value = values[valueIndex];
				// Validate the value against the model
				value = validateRowValue(value, cm, i, valueIndex);
				// Add the value to the final
				finalRow[i + 2] = value;
			}
			out.writeNext(finalRow);
			count++;
		}
	}

	/**
	 * Validate a value
	 * 
	 * @param value
	 * @param cm
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public static String validateRowValue(String value, ColumnModel cm,
			int rowIndex, int columnIndex) {
		if (cm == null)
			throw new IllegalArgumentException("ColumnModel cannot be null");
		if (cm.getColumnType() == null)
			throw new IllegalArgumentException(
					"ColumnModel.columnType cannot be null");
		// Validate non-null values
		if (value != null) {
			try {
				if (ColumnType.BOOLEAN.equals(cm.getColumnType())) {
					boolean boolValue = Boolean.parseBoolean(value);
					return Boolean.toString(boolValue);
				} else if (ColumnType.LONG.equals(cm.getColumnType())
						|| ColumnType.FILEHANDLEID.equals(cm.getColumnType())) {
					long lv = Long.parseLong(value);
					return Long.toString(lv);
				} else if (ColumnType.DOUBLE.equals(cm.getColumnType())) {
					double dv = Double.parseDouble(value);
					return Double.toString(dv);
				} else if (ColumnType.STRING.equals(cm.getColumnType())) {
					if (value.length() > MAX_STRING_LENGTH)
						throw new IllegalArgumentException(
								"String exceeds the maximum length of "
										+ MAX_STRING_LENGTH
										+ " characters. Consider using a FileHandle to store large strings.");
					return value;
				} else {
					throw new IllegalArgumentException(
							"Unknown ColumModel type: " + cm.getColumnType());
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						INVALID_VALUE_TEMPLATE, rowIndex, columnIndex,
						cm.getColumnType(), e.getLocalizedMessage()));
			}
		} else {
			// the value is null so apply the default if provided
			if (cm.getDefaultValue() != null) {
				value = cm.getDefaultValue();
			}
			return value;
		}
	}
}
