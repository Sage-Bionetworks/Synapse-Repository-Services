package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;

/**
 * Utilities for working with Tables and Row data.
 * 
 * @author jmhill
 * 
 */
public class TableModelTestUtils {

	/**
	 * Create one column of each type.
	 * 
	 * @return
	 */
	public static List<ColumnModel> createOneOfEachType() {
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for (int i = 0; i < ColumnType.values().length; i++) {
			ColumnType type = ColumnType.values()[i];
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(type);
			cm.setName("i" + i);
			cm.setId("" + i);
			if (ColumnType.STRING == type) {
				cm.setMaximumSize(47L);
			}
			results.add(cm);
		}
		return results;
	}

	/**
	 * Create the given number of rows.
	 * 
	 * @param cms
	 * @param count
	 * @return
	 */
	public static List<Row> createRows(List<ColumnModel> cms, int count) {
		List<Row> rows = new LinkedList<Row>();
		for (int i = 0; i < count; i++) {
			Row row = new Row();
			// Add a value for each column
			List<String> values = new LinkedList<String>();
			for (ColumnModel cm : cms) {
				if (cm.getColumnType() == null)
					throw new IllegalArgumentException("ColumnType cannot be null");
				switch (cm.getColumnType()) {
				case STRING:
					values.add("string" + i);
					continue;
				case LONG:
				case DATE:
				case FILEHANDLEID:
					values.add("" + i);
					continue;
				case BOOLEAN:
					if (i % 2 > 0) {
						values.add(Boolean.TRUE.toString());
					} else {
						values.add(Boolean.FALSE.toString());
					}
					continue;
				case DOUBLE:
					values.add("" + (i * 3.41));
					continue;
				}
				throw new IllegalArgumentException("Unknown ColumnType: " + cm.getColumnType());
			}
			row.setValues(values);
			rows.add(row);
		}
		return rows;
	}

	public static void updateRow(List<ColumnModel> cms, Row toUpdatet, int i) {
		// Add a value for each column
		List<String> values = new LinkedList<String>();
		for (ColumnModel cm : cms) {
			if (cm.getColumnType() == null)
				throw new IllegalArgumentException("ColumnType cannot be null");
			switch (cm.getColumnType()) {
			case STRING:
				values.add("updateString" + i);
				continue;
			case LONG:
			case DATE:
			case FILEHANDLEID:
				values.add("" + i);
				continue;
			case BOOLEAN:
				if (i % 2 > 0) {
					values.add(Boolean.TRUE.toString());
				} else {
					values.add(Boolean.FALSE.toString());
				}
				continue;
			case DOUBLE:
				values.add("" + (i * 3.41));
				continue;
			}
			throw new IllegalArgumentException("Unknown ColumnType: " + cm.getColumnType());
		}
		toUpdatet.setValues(values);
	}
}
