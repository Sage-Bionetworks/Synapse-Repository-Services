package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.util.csv.CsvNullReader;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.collect.Lists;

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
		return createRows(cms, count, true);
	}

	public static List<Row> createRows(List<ColumnModel> cms, int count, boolean useDateStrings) {
		List<Row> rows = new LinkedList<Row>();
		for (int i = 0; i < count; i++) {
			Row row = new Row();
			// Add a value for each column
			updateRow(cms, row, i, false, useDateStrings);
			rows.add(row);
		}
		return rows;
	}

	/**
	 * Create the given number of rows with all nulls.
	 * 
	 * @param cms
	 * @param count
	 * @return
	 */
	public static List<Row> createNullRows(List<ColumnModel> cms, int count) {
		List<Row> rows = new LinkedList<Row>();
		for (int i = 0; i < count; i++) {
			Row row = new Row();
			List<String> values = new LinkedList<String>();
			for (ColumnModel cm : cms) {
				values.add(null);
			}
			row.setValues(values);
			rows.add(row);
		}
		return rows;
	}
	
	/**
	 * Create the given number of rows with all values as empty strings.
	 * 
	 * @param cms
	 * @param count
	 * @return
	 */
	public static List<Row> createEmptyRows(List<ColumnModel> cms, int count) {
		List<Row> rows = new LinkedList<Row>();
		for (int i = 0; i < count; i++) {
			Row row = new Row();
			List<String> values = new LinkedList<String>();
			for (ColumnModel cm : cms) {
				values.add("");
			}
			row.setValues(values);
			rows.add(row);
		}
		return rows;
	}

	public static void updateRow(List<ColumnModel> cms, Row toUpdate, int i) {
		updateRow(cms, toUpdate, i, true, false);
	}

	private static final SimpleDateFormat gmtDateFormatter;
	static {
		gmtDateFormatter = new SimpleDateFormat("yy-M-d H:m:s.SSS");
		gmtDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static void updateRow(List<ColumnModel> cms, Row toUpdate, int i, boolean isUpdate, boolean useDateStrings) {
		// Add a value for each column
		List<String> values = new LinkedList<String>();
		for (ColumnModel cm : cms) {
			if (cm.getColumnType() == null)
				throw new IllegalArgumentException("ColumnType cannot be null");
			switch (cm.getColumnType()) {
			case STRING:
				values.add((isUpdate ? "updatestring" : "string") + i);
				continue;
			case LONG:
				values.add("" + (i + 3000));
				continue;
			case DATE:
				if (useDateStrings && i % 2 == 0) {
					values.add(gmtDateFormatter.format(new Date(i + 4000)));
				} else {
					values.add("" + (i + 4000));
				}
				continue;
			case FILEHANDLEID:
				values.add("" + (i + 5000));
				continue;
			case BOOLEAN:
				if (i % 2 > 0) {
					values.add(Boolean.TRUE.toString());
				} else {
					values.add(Boolean.FALSE.toString());
				}
				continue;
			case DOUBLE:
				values.add("" + (i * 3.41 + 3.12));
				continue;
			}
			throw new IllegalArgumentException("Unknown ColumnType: " + cm.getColumnType());
		}
		toUpdate.setValues(values);
	}

	public static Row createRow(Long rowId, Long rowVersion, String... values) {
		Row row = new Row();
		row.setRowId(rowId);
		row.setVersionNumber(rowVersion);
		row.setValues(Lists.newArrayList(values));
		return row;
	}

	public static RowReference createRowReference(Long rowId, Long rowVersion) {
		RowReference ref = new RowReference();
		ref.setRowId(rowId);
		ref.setVersionNumber(rowVersion);
		return ref;
	}
	
	/**
	 * Helper to create columns by name.
	 * @param names
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public static List<ColumnModel> createColumsWithNames(String...names){
		List<ColumnModel> results = new ArrayList<ColumnModel>(names.length);
		for(int i=0; i<names.length; i++){
			results.add(createColumn(i, names[i], ColumnType.STRING));
		}
		return results;
	}

	public static ColumnModel createColumn(long id) {
		return createColumn(id, "col_" + id, ColumnType.STRING);
	}

	public static ColumnModel createColumn(long id, String name, ColumnType type) {
		ColumnModel cm = new ColumnModel();
		cm.setId("" + id);
		cm.setName(name);
		cm.setColumnType(type);
		return cm;
	}

	/**
	 * Create a CSV string from the passed row data.
	 * @param  input List of rows where each row is represented by a string array.
	 * @return
	 */
	public static String createCSVString(List<String[]> input) {
		StringWriter writer = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(writer);
		csvWriter.writeAll(input);
		return writer.toString();
	}
	
	/**
	 * Create a reader that wraps the passed row data.
	 * @param input List of rows where each row is represented by a string array.
	 * @return
	 */
	public static CsvNullReader createReader(List<String[]> input) {
		String csv = createCSVString(input);
		StringReader reader = new StringReader(csv);
		return new CsvNullReader(reader);
	}
}
