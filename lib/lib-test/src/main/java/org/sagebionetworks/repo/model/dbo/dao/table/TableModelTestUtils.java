package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.AbstractColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SelectColumnAndModel;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.csv.CsvNullReader;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
	 * @param hasDefaults
	 * 
	 * @return
	 */
	public static List<ColumnModel> createOneOfEachType() {
		return createOneOfEachType(false);
	}

	public static ColumnMapper createMapperForOneOfEachType() {
		return new AbstractColumnMapper() {
			@Override
			protected LinkedHashMap<String, SelectColumnAndModel> createNameToModelMap() {
				List<SelectColumnAndModel> selectColumnAndModels = createSelectColumnAndModelList();
				return Transform.toOrderedIdMap(selectColumnAndModels, new Function<SelectColumnAndModel, String>() {
					@Override
					public String apply(SelectColumnAndModel input) {
						return input.getColumnModel().getName();
					}
				});
			}

			@Override
			protected Map<Long, SelectColumnAndModel> createIdToModelMap() {
				List<SelectColumnAndModel> selectColumnAndModels = createSelectColumnAndModelList();
				return Transform.toIdMap(selectColumnAndModels, new Function<SelectColumnAndModel, Long>() {
					@Override
					public Long apply(SelectColumnAndModel input) {
						return Long.parseLong(input.getColumnModel().getId());
					}
				});
			}

			@Override
			protected List<SelectColumnAndModel> createSelectColumnAndModelList() {
				List<ColumnModel> oneOfEachType = createOneOfEachType(false);
				return Transform.toList(oneOfEachType, new Function<ColumnModel, SelectColumnAndModel>() {
					@Override
					public SelectColumnAndModel apply(final ColumnModel input) {
						return new SelectColumnAndModel() {
							@Override
							public String getName() {
								return input.getName();
							}

							@Override
							public ColumnType getColumnType() {
								return input.getColumnType();
							}

							@Override
							public SelectColumn getSelectColumn() {
								SelectColumn selectColumn = new SelectColumn();
								selectColumn.setName(input.getName());
								selectColumn.setColumnType(input.getColumnType());
								selectColumn.setId(input.getId());
								return selectColumn;
							}

							@Override
							public ColumnModel getColumnModel() {
								return input;
							}
						};
					}
				});
			}
		};
	}

	public static final Function<ColumnModel, String> convertToNameFunction = new Function<ColumnModel, String>() {
		@Override
		public String apply(ColumnModel input) {
			return input.getName();
		}
	};

	public static List<ColumnModel> createOneOfEachType(boolean hasDefaults) {
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for (int i = 0; i < ColumnType.values().length; i++) {
			ColumnType type = ColumnType.values()[i];
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(type);
			cm.setName("i" + i);
			cm.setId("" + i);
			if (type == ColumnType.STRING || type == ColumnType.LINK) {
				cm.setMaximumSize(47L);
			}
			if (hasDefaults) {
				String defaultValue;
				switch (type) {
				case BOOLEAN:
					defaultValue = "true";
					break;
				case DATE:
					defaultValue = "978307200000";
					break;
				case DOUBLE:
					defaultValue = "1.3";
					break;
				case FILEHANDLEID:
				case ENTITYID:
				case LARGETEXT:
					defaultValue = null;
					break;
				case INTEGER:
					defaultValue = "-10000000000000";
					break;
				case STRING:
					defaultValue = "defaultString";
					break;
				case LINK:
					defaultValue = "defaultLink";
					break;
				default:
					throw new IllegalStateException("huh? missing enum");
				}
				cm.setDefaultValue(defaultValue);
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
		return createRows(cms, count, true, null);
	}

	/**
	 * Create a row.
	 * @param cms
	 * @param count
	 * @param fileHandleIs All file handle columns will be populated with files from this this.
	 * @return
	 */
	public static List<Row> createRows(List<ColumnModel> cms, int count, List<String> fileHandleIs) {
		return createRows(cms, count, true, fileHandleIs);
	}
	
	public static List<PartialRow> createPartialRows(List<ColumnModel> cms, int count) {
		List<PartialRow> rows = new LinkedList<PartialRow>();
		for (int i = 0; i < count; i++) {
			PartialRow row = new PartialRow();
			// Add a value for each column
			createPartialRow(cms, row, i, false, null);
			rows.add(row);
		}
		return rows;
	}

	public static List<Row> createExpectedFullRows(List<ColumnModel> cms, int count) {
		List<Row> rows = new LinkedList<Row>();
		for (int i = 0; i < count; i++) {
			Row row = new Row();
			// Add a value for each column
			updateExpectedPartialRow(cms, row, i, false, true, null);
			rows.add(row);
		}
		return rows;
	}

	public static List<Row> createRows(List<ColumnModel> cms, int count, boolean useDateStrings) {
		return createRows(cms, count, useDateStrings, null);
	}
	
	public static List<Row> createRows(List<ColumnModel> cms, int count, boolean useDateStrings, List<String> fileHandleIs) {
		List<Row> rows = new LinkedList<Row>();
		for (int i = 0; i < count; i++) {
			Row row = new Row();
			// Add a value for each column
			updateRow(cms, row, i, false, useDateStrings, fileHandleIs);
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
		updateRow(cms, toUpdate, i, true, false, null);
	}
	
	public static void updateRow(List<ColumnModel> cms, Row toUpdate, int i, List<String> fileHandleIds) {
		updateRow(cms, toUpdate, i, true, false, fileHandleIds);
	}

	public static PartialRow updatePartialRow(List<ColumnModel> schema, Row row, int i) {
		return updatePartialRow(schema, row, i, false, null);
	}

	private static final SimpleDateFormat gmtDateFormatter;
	static {
		gmtDateFormatter = new SimpleDateFormat("yy-M-d H:m:s.SSS");
		gmtDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static void updateRow(List<ColumnModel> cms, Row toUpdate, int i, boolean isUpdate, boolean useDateStrings, List<String> fileHandleIds) {
		// Add a value for each column
		List<String> values = new LinkedList<String>();
		int cmIndex = 0;
		for (ColumnModel cm : cms) {
			values.add(getValue(cm, i, isUpdate, useDateStrings, false, cmIndex++, fileHandleIds));
		}
		toUpdate.setValues(values);
	}

	private static void createPartialRow(List<ColumnModel> cms, PartialRow toUpdate, int i, boolean useDateStrings, List<String> fileHandleIds) {
		// Add a value for each column
		Map<String, String> values = Maps.newHashMap();
		int cmIndex = 0;
		for (ColumnModel cm : cms) {
			if (cm.getColumnType() == null)
				throw new IllegalArgumentException("ColumnType cannot be null");
			if ((i + cmIndex) % 3 != 0) {
				values.put(cm.getId(), getValue(cm, i, false, useDateStrings, false, cmIndex, fileHandleIds));
			}
			cmIndex++;
		}
		toUpdate.setValues(values);
	}

	private static PartialRow updatePartialRow(List<ColumnModel> cms, Row toUpdate, int i, boolean useDateStrings, List<String> fileHandleIds) {
		// Add a value for each column
		Map<String, String> values = Maps.newHashMap();
		for (int cmIndex = 0; cmIndex < cms.size(); cmIndex++) {
			ColumnModel cm = cms.get(cmIndex);
			if (cm.getColumnType() == null)
				throw new IllegalArgumentException("ColumnType cannot be null");
			if ((i + cmIndex) % 3 != 0) {
				String value = getValue(cm, i, true, useDateStrings, false, cmIndex, fileHandleIds);
				values.put(cm.getId(), value);
				toUpdate.getValues().set(cmIndex, value);
			}
		}
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(toUpdate.getRowId());
		partialRow.setValues(values);
		return partialRow;
	}

	private static void updateExpectedPartialRow(List<ColumnModel> cms, Row toUpdate, int i, boolean isUpdate, boolean useDateStrings, List<String> fileHandleIds) {
		// Add a value for each column
		List<String> values = new LinkedList<String>();
		int cmIndex = 0;
		for (ColumnModel cm : cms) {
			if (cm.getColumnType() == null)
				throw new IllegalArgumentException("ColumnType cannot be null");
			if ((i + cmIndex) % 3 != 0) {
				values.add(getValue(cm, i, isUpdate, useDateStrings, true, cmIndex, fileHandleIds));
			} else {
				values.add(cm.getDefaultValue());
			}
			cmIndex++;
		}
		toUpdate.setValues(values);
	}

	private static String getValue(ColumnModel cm, int i, boolean isUpdate, boolean useDateStrings, boolean isExpected, int colIndex, List<String> fileHandleIds) {
		i = i + 100000 * colIndex;
		if (cm.getColumnType() == null)
			throw new IllegalArgumentException("ColumnType cannot be null");
		switch (cm.getColumnType()) {
		case STRING:
			return (isUpdate ? "updatestring" : "string") + i;
		case INTEGER:
			return "" + (i + 3000);
		case DATE:
			if (!isExpected && useDateStrings && i % 2 == 0) {
				return gmtDateFormatter.format(new Date(i + 4000 + (isUpdate ? 10000 : 0)));
			} else {
				return "" + (i + 4000 + (isUpdate ? 10000 : 0));
			}
		case FILEHANDLEID:
			if(fileHandleIds != null){
				int index = i % fileHandleIds.size();
				return fileHandleIds.get(index);
			}else{
				return "" + (i + 5000 + (isUpdate ? 10000 : 0));
			}
		case ENTITYID:
			return "syn" + (i + 6000 + (isUpdate ? 10000 : 0)) + "." + (i + 7000 + (isUpdate ? 10000 : 0));
		case BOOLEAN:
			if (i % 2 > 0 ^ isUpdate) {
				return Boolean.TRUE.toString();
			} else {
				return Boolean.FALSE.toString();
			}
		case DOUBLE:
			return "" + (i * 3.41 + 3.12 + (isUpdate ? 10000 : 0));
		case LINK:
			return (isUpdate ? "updatelink" : "link") + (8000 + i);
		case LARGETEXT:
			return (isUpdate ? "updateLargeText" : "largeText") + (4000 + i);	
		}
		throw new IllegalArgumentException("Unknown ColumnType: " + cm.getColumnType());
	}

	public static Row createRow(Long rowId, Long rowVersion, String... values) {
		Row row = new Row();
		row.setRowId(rowId);
		row.setVersionNumber(rowVersion);
		row.setValues(Lists.newArrayList(values));
		return row;
	}

	public static Row createDeletionRow(Long rowId, Long rowVersion) {
		Row row = new Row();
		row.setRowId(rowId);
		row.setVersionNumber(rowVersion);
		row.setValues(null);
		return row;
	}

	public static PartialRow createPartialRow(Long rowId, String... keysAndValues) {
		PartialRow row = new PartialRow();
		row.setRowId(rowId);
		Map<String,String> values = Maps.newHashMap();
		for(int i = 0; i < keysAndValues.length; i+=2){
			assertNull("duplicate key", values.put(keysAndValues[i], keysAndValues[i + 1]));
		}
		row.setValues(values);
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
		for (int i = 0; i < names.length; i++) {
			results.add(createColumn((long) i, names[i], ColumnType.STRING));
		}
		return results;
	}

	public static ColumnModel createColumn(long id) {
		return createColumn(id, "col_" + id, ColumnType.STRING);
	}

	public static ColumnModel createColumn(Long id, String name, ColumnType type) {
		ColumnModel cm = new ColumnModel();
		if (id != null) {
			cm.setId(id.toString());
		}
		cm.setName(name);
		cm.setColumnType(type);
		if (type == ColumnType.STRING) {
			cm.setMaximumSize(50L);
		}
		return cm;
	}

	public static SelectColumn createSelectColumn(Long id, String name, ColumnType type) {
		SelectColumn scm = new SelectColumn();
		if (id != null) {
			scm.setId(id.toString());
		}
		scm.setName(name);
		scm.setColumnType(type);
		return scm;
	}

	/**
	 * Create a CSV string from the passed row data.
	 * 
	 * @param input List of rows where each row is represented by a string array.
	 * @return
	 * @throws IOException
	 */
	public static String createCSVString(List<String[]> input) throws IOException {
		StringWriter writer = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(writer);
		csvWriter.writeAll(input);
		csvWriter.close();
		return writer.toString();
	}
	
	/**
	 * Create a reader that wraps the passed row data.
	 * 
	 * @param input List of rows where each row is represented by a string array.
	 * @return
	 * @throws IOException
	 */
	public static CsvNullReader createReader(List<String[]> input) throws IOException {
		String csv = createCSVString(input);
		StringReader reader = new StringReader(csv);
		return new CsvNullReader(reader);
	}

	/**
	 * Is the row ID null or invalid?
	 * 
	 * @param rowId
	 * @return
	 */
	private static boolean isNullOrInvalid(Long rowId) {
		if (rowId == null)
			return true;
		return rowId < 0;
	}

	/**
	 * @param set
	 */
	private static void validateRowSet(RowSet set) {
		if (set == null)
			throw new IllegalArgumentException("RowSet cannot be null");
		;
		if (set.getHeaders() == null)
			throw new IllegalArgumentException("RowSet.headers cannot be null");
		if (set.getRows() == null)
			throw new IllegalArgumentException("RowSet.rows cannot be null");
		if (set.getRows().size() < 1)
			throw new IllegalArgumentException("RowSet.rows must contain at least one row.");
	}

	/**
	 * Assign RowIDs and version numbers to each row in the set according to the passed range.
	 * 
	 * @param set
	 * @param range
	 */
	public static void assignRowIdsAndVersionNumbers(RowSet set, IdRange range) {
		validateRowSet(set);
		Long id = range.getMinimumId();
		for (Row row : set.getRows()) {
			// Set the version number for each row
			row.setVersionNumber(range.getVersionNumber());
			if (isNullOrInvalid(row.getRowId())) {
				if (range.getMinimumId() == null) {
					throw new IllegalStateException("RowSet required at least one row ID but none were allocated.");
				}
				// This row needs an id.
				row.setRowId(id);
				id++;
				// Validate we have not exceeded the rows
				if (row.getRowId() > range.getMaximumId()) {
					throw new IllegalStateException("RowSet required more row IDs than were allocated.");
				}
			} else {
				// Validate the rowId is within range
				if (row.getRowId() > range.getMaximumUpdateId()) {
					throw new IllegalArgumentException("Cannot update row: " + row.getRowId() + " because it does not exist.");
				}
			}
		}
	}

	/**
	 * Extract the headers from a list of select columns
	 * 
	 * @param models
	 * @return
	 */
	public static List<Long> getIdsFromSelectColumns(List<SelectColumn> columns) {
		if (columns == null)
			throw new IllegalArgumentException("ColumnModels cannot be null");
		List<Long> ids = Lists.newArrayList();
		for (SelectColumn column : columns) {
			ids.add(Long.parseLong(column.getId()));
		}
		return ids;
	}
}
