package org.sagebionetworks.table.cluster.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.AbstractColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelMapper;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SelectColumnAndModel;
import org.sagebionetworks.repo.model.table.SelectColumnMapper;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CsvNullReader;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * Utilities for working with Tables and Row data.
 * 
 * @author jmhill
 * 
 */
public class TableModelUtils {

	public static final Function<Long, String> LONG_TO_STRING = new Function<Long, String>() {
		@Override
		public String apply(Long input) {
			return input.toString();
		}
	};

	private static final String INVALID_VALUE_TEMPLATE = "Value at [%1$s,%2$s] was not a valid %3$s. %4$s";
	private static final String TABLE_SEMAPHORE_KEY_TEMPLATE = "TALBE-LOCK-%1$d";

	/**
	 * The maximum allowed value for the number characters for a string.
	 */
	public static final Long MAX_ALLOWED_STRING_SIZE = 1000L;
	
	/**
	 * Delimiter used to list column model IDs as a string.
	 */
	public static final String COLUMN_MODEL_ID_STRING_DELIMITER = ",";
	
	public static final Pattern ENTITYID_PATTERN = Pattern.compile("syn(\\d+)(\\.(\\d+))?");

	public static class NodeIdAndVersion {
		public final Long id;
		public final Long version;

		public NodeIdAndVersion(Long id, Long version) {
			this.id = id;
			this.version = version;
		}
	}

	public static final Function<ColumnModel, Long> COLUMN_MODEL_TO_ID = new Function<ColumnModel, Long>() {
		@Override
		public Long apply(ColumnModel cm) {
			return Long.parseLong(cm.getId());
		}
	};

	public static final Function<SelectColumn, Long> SELECT_COLUMN_TO_ID = new Function<SelectColumn, Long>() {
		@Override
		public Long apply(SelectColumn sc) {
			return Long.parseLong(sc.getId());
		}
	};

	public static final Function<SelectColumnAndModel, String> SELECT_COLUMN_AND_MODEL_TO_NAME = new Function<SelectColumnAndModel, String>() {
		@Override
		public String apply(SelectColumnAndModel sc) {
			return sc.getSelectColumn().getName();
		}
	};

	public static final Function<SelectColumnAndModel, Long> SELECT_COLUMN_AND_MODEL_TO_ID = new Function<SelectColumnAndModel, Long>() {
		@Override
		public Long apply(SelectColumnAndModel sc) {
			return Long.parseLong(sc.getSelectColumn().getId());
		}
	};

	/**
	 * This utility will validate and convert the passed RowSet to an output CSV written to a GZip stream.
	 * 
	 * @param models
	 * @param set
	 * @param out
	 * @param isDeletion
	 * @throws IOException
	 */
	public static void validateAnWriteToCSVgz(List<ColumnModel> models, RawRowSet set, OutputStream out) throws IOException {
		GZIPOutputStream zipOut = null;
		OutputStreamWriter osw = null;
		CSVWriter csvWriter = null;
		try{
			zipOut = new GZIPOutputStream(out);
			osw = new OutputStreamWriter(zipOut);
			csvWriter = new CSVWriter(osw);
			// Write the data to the the CSV.
			TableModelUtils.validateAndWriteToCSV(models, set, csvWriter);
		}finally{
			if(csvWriter != null){
				csvWriter.flush();
				csvWriter.close();
			}
			if(out != null){
				out.close();
			}
		}
	}
	

	/**
	 * This utility will validate and convert the passed RowSet to an output
	 * CSV.
	 * 
	 * @param models
	 * @param set
	 */
	public static void validateAndWriteToCSV(List<ColumnModel> models, RawRowSet set, CSVWriter out) {
		if (models == null)
			throw new IllegalArgumentException("Models cannot be null");
		validateRowSet(set);
		if (out == null)
			throw new IllegalArgumentException("CSVWriter cannot be null");
		if (models.size() != set.getIds().size())
			throw new IllegalArgumentException(
					"RowSet.headers size must be equal to the number of columns in the table.  The table has :"
							+ models.size()
							+ " columns and the passed RowSet.headers has: "
 + set.getIds().size());
		// Now map the index of each column
		Map<Long, Integer> columnIndexMap = createColumnIdToIndexMap(set);
		// Process each row
		int count = 0;
		for (Row row : set.getRows()) {
			if (row.getRowId() == null)
				throw new IllegalArgumentException(
						"Row.rowId cannot be null for row number: " + count);
			if (row.getVersionNumber() == null)
				throw new IllegalArgumentException(
						"Row.versionNumber cannot be null for row number: "
								+ count);
			String[] finalRow;
			if (row.getValues() == null) {
				// only output rowId and rowVersion
				finalRow = new String[2];
			} else {
				if (row.getValues().size() == 0)
					throw new IllegalArgumentException("Row " + count + " has empty list of values");
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
				finalRow = new String[values.length + 2];
				// Now process all of the columns as defined by the schema
				for (int i = 0; i < models.size(); i++) {
					ColumnModel cm = models.get(i);
					Integer valueIndex = columnIndexMap.get(Long.parseLong(cm.getId()));
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
			}
			finalRow[0] = row.getRowId().toString();
			finalRow[1] = row.getVersionNumber().toString();
			out.writeNext(finalRow);
			count++;
		}
	}


	/**
	 * @param set
	 */
	public static void validateRowSet(RawRowSet set) {
		if (set == null)
			throw new IllegalArgumentException("RowSet cannot be null");
		;
		if (set.getIds() == null)
			throw new IllegalArgumentException("RowSet.ids cannot be null");
		if (set.getRows() == null)
			throw new IllegalArgumentException("RowSet.rows cannot be null");
		if (set.getRows().size() < 1)
			throw new IllegalArgumentException(
					"RowSet.rows must contain at least one row.");
	}
	
	/**
	 * Validate that all rows have the expected version number
	 * @param rows
	 * @param versionNumber
	 */
	public static void validateRowVersions(final List<Row> rows, final Long versionNumber){
		if(rows == null){
			throw new IllegalArgumentException("Rows cannot be null");
		}
		if(rows.isEmpty()){
			throw new IllegalArgumentException("Rows cannot be empty");
		}
		for(Row row: rows){
			if(row.getVersionNumber() == null){
				throw new IllegalArgumentException("Row.versionNumber cannot be null");
			}
			if(!row.getVersionNumber().equals(versionNumber)){
				throw new IllegalArgumentException("Row.versionNumber does not match expected version: "+versionNumber);
			}
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
		
		// Only strings can have a value that is an empty string. See PLFM-2657
		if ("".equals(value) && !(cm.getColumnType() == ColumnType.STRING || cm.getColumnType() == ColumnType.LINK || cm.getColumnType() == ColumnType.LARGETEXT)) {
			value = null;
		}
		
		// Validate non-null values
		if (value != null) {	
			try {
				return validateValue(value, cm);
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


	public static String validateValue(String value, ColumnModel cm) {
		switch (cm.getColumnType()) {
		case BOOLEAN:
			boolean boolValue;
			if (value.equalsIgnoreCase("true")) {
				boolValue = true;
			} else if (value.equalsIgnoreCase("false")) {
				boolValue = false;
			} else {
				throw new IllegalArgumentException("A value in a boolean column must be null, 'true' or 'false', but was '" + value
						+ "'");
			}
			return Boolean.toString(boolValue);
		case INTEGER:
		case FILEHANDLEID:
			long lv = Long.parseLong(value);
			return Long.toString(lv);
		case ENTITYID:
			if (!ENTITYID_PATTERN.matcher(value).matches()) {
				throw new IllegalArgumentException("Malformed entity ID (should be syn123 or syn 123.4): " + value);
			}
			return value;
		case DATE:
			// value can be either a number (in which case it is milliseconds since blah) or not a number (in
			// which case it is date string)
			long time;
			try {
				time = Long.parseLong(value);
			} catch (NumberFormatException e) {
				time = TimeUtils.parseSqlDate(value);
			}
			return Long.toString(time);
		case DOUBLE:
			double dv;
			try {
				dv = Double.parseDouble(value);
			} catch (NumberFormatException e) {
				value = value.toLowerCase();
				if (value.equals("nan")) {
					dv = Double.NaN;
				} else if (value.equals("-inf") || value.equals("-infinity") || value.equals("-\u221E")) {
					dv = Double.NEGATIVE_INFINITY;
				} else if (value.equals("+inf") || value.equals("+infinity") || value.equals("+\u221E") || value.equals("inf")
						|| value.equals("infinity") || value.equals("\u221E")) {
					dv = Double.POSITIVE_INFINITY;
				} else {
					throw e;
				}
			}
			return Double.toString(dv);
		case STRING:
			if (cm.getMaximumSize() == null)
				throw new IllegalArgumentException("String columns must have a maximum size");
			if (value.length() > cm.getMaximumSize()) {
				throw new IllegalArgumentException("String '" + value + "' exceeds the maximum length of " + cm.getMaximumSize()
						+ " characters. Consider using a FileHandle to store large strings.");
			}
			checkStringEnum(value, cm);
			return value;
		case LINK:
			if (cm.getMaximumSize() == null)
				throw new IllegalArgumentException("Link columns must have a maximum size");
			if (value.length() > cm.getMaximumSize()) {
				throw new IllegalArgumentException("Link '" + value + "' exceeds the maximum length of " + cm.getMaximumSize()
						+ " characters.");
			}
			checkStringEnum(value, cm);
			return value;
		case LARGETEXT:
			if (value.length() > ColumnConstants.MAX_LARGE_TEXT_CHARACTERS) {
				throw new IllegalArgumentException("Exceeds the maximum number of characters: "+ColumnConstants.MAX_LARGE_TEXT_CHARACTERS);
			}
			checkStringEnum(value, cm);
			return value;	
		}
		
		throw new IllegalArgumentException("Unknown ColumModel type: " + cm.getColumnType());
	}


	private static void checkStringEnum(String value, ColumnModel cm) {
		if (cm.getEnumValues() != null) {
			// doing a contains directly on the list. With 100 values or less, making this a set is not making much
			// of a difference and isn't east to do. When/if we allow many more values, we might have to revisit
			if (!cm.getEnumValues().contains(value)) {
				if (cm.getEnumValues().size() > 10) {
					throw new IllegalArgumentException("'" + value
							+ "' is not a valid value for this column. See column definition for valid values.");
				} else {
					throw new IllegalArgumentException("'" + value + "' is not a valid value for this column. Valid values are: "
							+ StringUtils.join(cm.getEnumValues(), ", ") + ".");
				}
			}
		}
	}

	/**
	 * Translate the value as returned from a query according to the column model
	 * 
	 * @param value
	 * @param columnModel
	 * @return
	 */
	public static String translateRowValueFromQuery(String value, ColumnType columnType) {
		if (columnType == ColumnType.BOOLEAN) {
			if ("0".equals(value)) {
				value = "false";
			} else if ("1".equals(value)) {
				value = "true";
			}
		}
		return value;
	}

	/**
	 * Count all of the empty or invalid rows in the set
	 * @param set
	 */
	public static int countEmptyOrInvalidRowIds(RawRowSet set) {
		validateRowSet(set);
		int count = 0;
		for (Row row : set.getRows()) {
			if(isNullOrInvalid(row.getRowId())){
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Is the row ID null or invalid?
	 * 
	 * @param rowId
	 * @return
	 */
	public static boolean isNullOrInvalid(Long rowId) {
		if(rowId == null) return true;
		return rowId < 0;
	}
	
	/**
	 * Is this a deleted row?
	 * 
	 * @param row
	 * @return
	 */
	public static boolean isDeletedRow(Row row) {
		return row.getValues() == null || row.getValues().size() == 0;
	}

	/**
	 * Assign RowIDs and version numbers to each row in the set according to the passed range.
	 * @param set
	 * @param range
	 */
	public static void assignRowIdsAndVersionNumbers(RawRowSet set, IdRange range) {
		validateRowSet(set);
		Long id = range.getMinimumId();
		for (Row row : set.getRows()) {
			// Set the version number for each row
			row.setVersionNumber(range.getVersionNumber());
			if(isNullOrInvalid(row.getRowId())){
				if(range.getMinimumId() == null){
					throw new IllegalStateException("RowSet required at least one row ID but none were allocated.");
				}
				// This row needs an id.
				row.setRowId(id);
				id++;
				// Validate we have not exceeded the rows
				if(row.getRowId() > range.getMaximumId()){
					throw new IllegalStateException("RowSet required more row IDs than were allocated.");
				}
			}else{
				// Validate the rowId is within range
				if(row.getRowId() > range.getMaximumUpdateId()){
					throw new IllegalArgumentException("Cannot update row: "+row.getRowId()+" because it does not exist.");
				}
			}
		}
	}

	/**
	 * Read the passed CSV into a RowSet.
	 * 
	 * @param reader
	 * @param rowsToGet
	 * @return
	 * @throws IOException
	 */
	public static List<Row> readFromCSV(CsvNullReader reader) throws IOException {
		if (reader == null)
			throw new IllegalArgumentException("CsvNullReader cannot be null");
		final List<Row> rows = new LinkedList<Row>();
		// Scan the data.
		scanFromCSV(reader, new RowHandler() {

			@Override
			public void nextRow(Row row) {
				// Add this to the rows
				rows.add(row);
			}
		});
		return rows;
	}
	
	/**
	 * Read the passed CSV into a RowSet.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static void scanFromCSV(CsvNullReader reader, RowHandler handler) throws IOException {
		if (reader == null)
			throw new IllegalArgumentException("CsvNullReader cannot be null");
		String[] rowArray = null;
		while((rowArray = reader.readNext()) != null){
			Row row = new Row();
			if (rowArray.length < 2)
				throw new IllegalArgumentException("Row does not contain at least three columns");
			row.setRowId(Long.parseLong(rowArray[0]));
			row.setVersionNumber(Long.parseLong(rowArray[1]));
			List<String> values = new LinkedList<String>();
			// Add the rest of the values
			for(int i=2; i<rowArray.length; i++){
				values.add(rowArray[i]);
			}
			row.setValues(values);
			// Pass to the handler
			handler.nextRow(row);
		}
	}
	
	/**
	 * Read the passed Gzip CSV into a RowSet.
	 * 
	 * @param zippedStream
	 * @param rowsToGet
	 * @return
	 * @throws IOException
	 */
	public static List<Row> readFromCSVgzStream(InputStream zippedStream) throws IOException {
		GZIPInputStream zipIn = null;
		InputStreamReader isr = null;
		CsvNullReader csvReader = null;
		try{
			zipIn = new GZIPInputStream(zippedStream);
			isr = new InputStreamReader(zipIn);
			csvReader = new CsvNullReader(isr);
			return readFromCSV(csvReader);
		}finally{
			if(csvReader != null){
				csvReader.close();
			}
		}
	}
	
	/**
	 * Scan over the passed stream without loading it into memory
	 * @param zippedStream
	 * @param tableId
	 * @param headers
	 * @param handler
	 * @throws IOException
	 */
	public static void scanFromCSVgzStream(InputStream zippedStream, RowHandler handler) throws IOException{
		GZIPInputStream zipIn = null;
		InputStreamReader isr = null;
		CsvNullReader csvReader = null;
		try{
			zipIn = new GZIPInputStream(zippedStream);
			isr = new InputStreamReader(zipIn);
			csvReader = new CsvNullReader(isr);
			scanFromCSV(csvReader, handler);
		}finally{
			if(csvReader != null){
				csvReader.close();
			}
		}
	}
	
	/**
	 * Extract the ids from a list of column Models.s
	 * 
	 * @param models
	 * @return
	 */
	public static List<Long> getIds(List<ColumnModel> models) {
		if(models == null) throw new IllegalArgumentException("ColumnModels cannot be null");
		List<Long> ids = Lists.newArrayListWithCapacity(models.size());
		for(ColumnModel model: models){
			if(model.getId() == null) throw new IllegalArgumentException("ColumnModel ID cannot be null");
			ids.add(Long.parseLong(model.getId()));
		}
		return ids;
	}

	/**
	 * Extract the headers from a list of column Models.s
	 * 
	 * @param models
	 * @return
	 */
	public static List<String> getNames(List<ColumnModel> models) {
		if (models == null)
			throw new IllegalArgumentException("ColumnModels cannot be null");
		List<String> names = Lists.newArrayListWithCapacity(models.size());
		for (ColumnModel model : models) {
			if (model.getId() == null)
				throw new IllegalArgumentException("ColumnModel ID cannot be null");
			names.add(model.getName());
		}
		return names;
	}

	/**
	 * Extract the headers from a list of column Models.s
	 * 
	 * @param models
	 * @return
	 */
	public static List<SelectColumn> getSelectColumns(Collection<ColumnModel> models, boolean isAggregate) {
		ValidateArgument.required(models, "models");
		List<SelectColumn> result = Lists.newArrayListWithCapacity(models.size());
		for (ColumnModel model : models) {
			result.add(createSelectColumn(model, isAggregate));
		}
		return result;
	}

	/**
	 * Create a delimited string of column model IDs.
	 * 
	 * @param models
	 * @return
	 */
	public static String createDelimitedColumnModelIdString(List<Long> ids) {
		ValidateArgument.required(ids, "headers");
		return StringUtils.join(ids, COLUMN_MODEL_ID_STRING_DELIMITER);
	}
	
	/**
	 * Read the list of column model ids from the passed delimited string.
	 * @param in
	 * @return
	 */
	public static List<Long> readColumnModelIdsFromDelimitedString(String in) {
		ValidateArgument.required(in, "String");
		String[] split = in.split(COLUMN_MODEL_ID_STRING_DELIMITER);
		List<Long> ids = Lists.newArrayListWithCapacity(split.length);
		for (String idString : split) {
			ids.add(Long.parseLong(idString));
		}
		return ids;
	}

	/**
	 * Get the distinct version from the the rows ordered by version
	 * 
	 * @param rows
	 * @return
	 */
	public static Set<Long> getDistictVersions(List<RowReference> rows) {
		if(rows == null) throw new IllegalArgumentException("rows cannot be null");
		Set<Long> distictVersions = Sets.newTreeSet();
		for(RowReference ref: rows){
			if (ref.getVersionNumber() == null) {
				throw new IllegalArgumentException("version cannot be null (row " + ref.getRowId() + ")");
			}
			distictVersions.add(ref.getVersionNumber());
		}
		return distictVersions;
	}
	
	
	/**
	 * Get the distinct and valid rowIds from the passed rows
	 * @param rows
	 * @return
	 */
	public static Map<Long, Long> getDistictValidRowIds(Iterable<Row> rows) {
		ValidateArgument.required(rows, "rows");
		Map<Long, Long> distictRowIds = Maps.newHashMap();
		for (Row ref : rows) {
			if (!isNullOrInvalid(ref.getRowId())) {
				if(ref.getValues() != null){
					if (distictRowIds.put(ref.getRowId(), ref.getVersionNumber()) != null) {
						// the row id is found twice int the same rowset
						throw new IllegalArgumentException("The row id " + ref.getRowId() + " is included more than once in the rowset");
					}
				}
			}
		}
		return distictRowIds;
	}
	
	/**
	 * Convert each passed RowSet into the passed schema and merge all results into a single output set.
	 * 
	 * @param sets
	 * @param restultForm
	 * @return
	 */
	public static RowSet convertToSchemaAndMerge(List<RawRowSet> sets, ColumnModelMapper resultSchema, String tableId,
			String etag) {
		// Prepare the final set
		RowSet out = new RowSet();
		out.setTableId(tableId);
		out.setRows(new LinkedList<Row>());
		out.setHeaders(resultSchema.getSelectColumns());
		out.setEtag(etag);
		// Transform each
		for (RawRowSet set : sets) {
			// Transform each and merge the results
			convertToSchemaAndMerge(set, resultSchema, out);
		}
		return out;
	}
	
	/**
	 * Convert the passed RowSet into the passed schema and append the rows to the passed output set.
	 * 
	 * @param sets
	 * @param restultForm
	 * @param sets
	 */
	public static void convertToSchemaAndMerge(RawRowSet in, ColumnModelMapper resultSchema, RowSet out) {
		Map<Long, Integer> columnIndexMap = createColumnIdToIndexMap(in);
		// Now convert each row into the requested format.
		// Process each row
		for (Row row : in.getRows()) {
			// First convert the values to
			if (row.getValues() == null) {
				continue;
			}
			Row newRow = convertToSchemaAndMerge(row, columnIndexMap, resultSchema);
			// add the new row to the out set
			out.getRows().add(newRow);
		}
	}
	
	/**
	 * Convert the passed RowSet into the passed schema and append the rows to the passed output set.
	 * @param sets
	 * @param resultSchema
	 * @param sets
	 */
	public static Row convertToSchemaAndMerge(Row row, Map<Long, Integer> columnIndexMap, ColumnModelMapper resultSchema) {
		// Create the new row
		Row newRow = new Row();
		newRow.setRowId(row.getRowId());
		newRow.setVersionNumber(row.getVersionNumber());
		List<String> newValues = Lists.newArrayListWithCapacity(resultSchema.columnModelCount());
		newRow.setValues(newValues);

		// Now process all of the columns as defined by the schema
		for (ColumnModel model : resultSchema.getColumnModels()) {
			String value = null;
			Integer valueIndex = columnIndexMap.get(Long.parseLong(model.getId()));
			if (valueIndex == null) {
				// this means this column did not exist when this row as created, so set the value to the default
				// value
				value = model.getDefaultValue();
			} else {
				// Get the value
				value = row.getValues().get(valueIndex);
			}
			newValues.add(value);
		}
		return newRow;
	}
	
	/**
	 * Calculate the maximum row size for a given schema.
	 * 
	 * @param collection
	 * @return
	 */
	public static int calculateMaxRowSize(Iterable<ColumnModel> models) {
		ValidateArgument.required(models, "models");
		int size = 0;
		for (ColumnModel cm : models) {
			size += calculateMaxSizeForType(cm.getColumnType(), cm.getMaximumSize());
		}
		return size;
	}
	
	/**
	 * Calculate the maximum row size for a given schema.
	 * 
	 * @param collection
	 * @return
	 */
	public static int calculateMaxRowSizeForColumnModels(ColumnMapper columnMapper) {
		int size = 0;
		for (SelectColumnAndModel scm : columnMapper.getSelectColumnAndModels()) {
			if (scm.getColumnType() == null) {
				// we don't know the type, now what?
				size += 64;
			} else if (scm.getColumnModel() != null) {
				size += calculateMaxSizeForType(scm.getColumnType(), scm.getColumnModel().getMaximumSize());
			} else {
				// we don't know the max size, now what?
				size += calculateMaxSizeForType(scm.getColumnType(), MAX_ALLOWED_STRING_SIZE);
			}
		}
		return size;
	}

	/**
	 * Calculate the maximum size in bytes that a column of this type can be when represented as a string.
	 * 
	 * @param cm
	 * @return
	 */
	public static int calculateMaxSizeForType(ColumnType type, Long maxSize){
		if(type == null) throw new IllegalArgumentException("ColumnType cannot be null");
		switch (type) {
		case STRING:
		case LINK:
			if (maxSize == null) {
				throw new IllegalArgumentException("maxSize cannot be null for String types");
			}
			return (int) (ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8 * maxSize);
		case LARGETEXT:
			return ColumnConstants.DEFAULT_LARGE_TEXT_BYTES;	
		case BOOLEAN:
			return ColumnConstants.MAX_BOOLEAN_BYTES_AS_STRING;
		case INTEGER:
		case DATE:
			return ColumnConstants.MAX_INTEGER_BYTES_AS_STRING;
		case DOUBLE:
			return ColumnConstants.MAX_DOUBLE_BYTES_AS_STRING;
		case FILEHANDLEID:
			return ColumnConstants.MAX_FILE_HANDLE_ID_BYTES_AS_STRING;
		case ENTITYID:
			return ColumnConstants.MAX_ENTITY_ID_BYTES_AS_STRING;
		}
		throw new IllegalArgumentException("Unknown ColumnType: " + type);
	}
	
	
	/**
	 * Calculate the actual size of a row.
	 * @param row
	 * @return
	 */
	public static int calculateActualRowSize(Row row){
		// row ID + row version.
		int bytes = ColumnConstants.MAX_INTEGER_BYTES_AS_STRING*2;
		if(row.getValues() != null){
			for(String value: row.getValues()){
				if(value != null){
					bytes += value.length() * ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8;
				}
			}
		}
		return bytes;
	}
	
	/**
	 * Is a request within the maximum number of bytes per request?
	 * 
	 * @param columnMapper - The schema of the request.  This determines the maximum number of bytes per row.
	 * @param rowCount - The number of rows requested.
	 * @param maxBytesPerRequest - The limit of the maximum number of bytes per request.
	 */
	public static boolean isRequestWithinMaxBytePerRequest(ColumnMapper columnMapper, int rowCount, int maxBytesPerRequest){
		// What is the size per row
		int maxBytesPerRow = calculateMaxRowSizeForColumnModels(columnMapper);
		int neededBytes = rowCount*maxBytesPerRow;
		return neededBytes <= maxBytesPerRequest;
	}
	
	/**
	 * This is the key used to gate access to a single table.
	 * This lock is used both to update the table index and also when a query is run.
	 * @param tableId
	 * @return
	 */
	public static String getTableSemaphoreKey(String tableId){
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		return String.format(TABLE_SEMAPHORE_KEY_TEMPLATE, KeyFactory.stringToKey(tableId));
	}
	
	/**
	 * Map the column names to the column IDs.
	 * @param columns
	 * @return
	 */
	public static Map<String, Long> createColumnNameToIdMap(List<ColumnModel> columns){
		HashMap<String, Long> map = new HashMap<String, Long>();
		for(ColumnModel cm: columns){
			map.put(cm.getName(), Long.parseLong(cm.getId()));
		}
		return map;
	}

	/**
	 * Map the column names to the column models.
	 * 
	 * @param columns
	 * @return
	 */
	public static LinkedHashMap<String, ColumnModel> createColumnNameToModelMap(List<ColumnModel> columns) {
		LinkedHashMap<String, ColumnModel> map = Maps.newLinkedHashMap();
		for (ColumnModel cm : columns) {
			map.put(cm.getName(), cm);
		}
		return map;
	}

	/**
	 * Map the column id to the column index.
	 * 
	 * @param rowset
	 * @return
	 */
	public static Map<Long, Integer> createColumnIdToIndexMap(TableRowChange rowChange) {
		return createColumnIdToIndexMap(rowChange.getIds());
	}

	/**
	 * Map the column id to the column index.
	 * 
	 * @param rowset
	 * @return
	 */
	public static Map<Long, Integer> createColumnIdToIndexMap(RawRowSet rowset) {
		return createColumnIdToIndexMap(rowset.getIds());
	}

	/**
	 * Map the input list of headers to their position in the list
	 * 
	 * @param headers
	 * @return
	 */
	public static Map<Long, Integer> createColumnIdToIndexMap(List<Long> columnIds) {
		Map<Long, Integer> columnIndexMap = Maps.newHashMap();
		int index = 0;
		for (Long columnId : columnIds) {
			columnIndexMap.put(columnId, index);
			index++;
		}
		return columnIndexMap;
	}

	/**
	 * Map column Id to column Models.
	 * 
	 * @param columns
	 * @return
	 */
	public static Map<Long, ColumnModel> createIDtoColumnModelMap(Iterable<ColumnModel> columns) {
		HashMap<Long, ColumnModel>  map = new HashMap<Long, ColumnModel> ();
		for(ColumnModel cm: columns){
			map.put(Long.parseLong(cm.getId()), cm);
		}
		return map;
	}
	/**
	 * Map String column id to column model
	 * @param columns
	 * @return
	 */
	public static Map<String, ColumnModel> createStringIDtoColumnModelMap(Collection<ColumnModel> columns){
		HashMap<String, ColumnModel>  map = new HashMap<String, ColumnModel> ();
		for(ColumnModel cm: columns){
			map.put(cm.getId(), cm);
		}
		return map;
	}
	
	/**
	 * Give a list of column Ids, create a column name header array.
	 * @param columnIds
	 * @param columns
	 * @param isAggregate
	 * @return
	 */
	public static String[] createColumnNameHeader(List<SelectColumn> selectColumns, boolean includeRowIdAndVersion) {
		List<String> outHeaderList = Lists.newArrayListWithCapacity(selectColumns.size() + 2);
		if (includeRowIdAndVersion) {
			outHeaderList.add(TableConstants.ROW_ID);
			outHeaderList.add(TableConstants.ROW_VERSION);
		}
		for (SelectColumn selectColumn : selectColumns) {
			outHeaderList.add(selectColumn.getName());
		}
		return outHeaderList.toArray(new String[outHeaderList.size()]);
	}
	
	/**
	 * Write a row to a string array.
	 * 
	 * @param row
	 * @param isAggregate
	 * @return
	 */
	public static String[] writeRowToStringArray(Row row, boolean includeRowIdAndVersion){
		String[] array = null;
		// Write this row
		if(!includeRowIdAndVersion){
			// For aggregates just write the values to the array.
			array = row.getValues().toArray(new String[row.getValues().size()]);
		}else{
			// For non-aggregates the rowId and rowVersion must also be written
			array = new String[row.getValues().size()+2];
			array[0] = row.getRowId().toString();
			array[1] = row.getVersionNumber().toString();
			int index = 2;
			for(String value: row.getValues()){
				array[index] = value;
				index++;
			}
		}
		return array;
	}
	
	/**
	 * Map column names to column IDs
	 * @param columns
	 * @return
	 */
	public static Map<String, Long> createNameToIDMap(List<ColumnModel> columns) {
		HashMap<String, Long> map = Maps.newHashMap();
		for(ColumnModel cm: columns){
			map.put(cm.getName(), Long.parseLong(cm.getId()));
		}
		return map;
	}

	/**
	 * Given the first row of a CSV create a columnId to Index map.
	 * If the row does not contain the names of the columns then null.
	 * @param rowValues
	 * @param schema
	 * @return
	 */
	public static Map<Long, Integer> createColumnIdToColumnIndexMapFromFirstRow(String[] rowValues, List<ColumnModel> schema) {
		Map<String, Long> nameMap = createNameToIDMap(schema);
		// Build the map from the names
		Map<Long, Integer> columnIdToColumnIndexMap = Maps.newHashMap();
		for (int i = 0; i < rowValues.length; i++) {
			String name = rowValues[i];
			if (name != null) {
				Long id = TableConstants.getReservedColumnId(name);
				if (id == null) {
					id = nameMap.get(name);
					if (id == null) {
						// The values are not column names so this was not a header row.
						throw new IllegalArgumentException(
								"The first line is expected to be a header but the values do not match the names of of the columns of the table ("
										+ name + " is not a vaild column name or id). Header row: " + StringUtils.join(rowValues, ','));
					}
				}
				columnIdToColumnIndexMap.put(id, i);
			}
		}
		return columnIdToColumnIndexMap;
	}

	public static Map<Long, Integer> createColumnIdToColumnIndexMapFromColumnIds(List<String> columnIds, List<ColumnModel> resultSchema) {
		Set<Long> existingColumnIds = Sets.newHashSet(Lists.transform(resultSchema, COLUMN_MODEL_TO_ID));
		// Build the map from the ids
		Map<Long, Integer> columnIdToColumnIndexMap = Maps.newHashMap();
		for (int i = 0; i < columnIds.size(); i++) {
			String columnIdString = columnIds.get(i);
			Long id = null;
			if (columnIdString != null) {
				id = TableConstants.getReservedColumnId(columnIdString);
				if (id == null) {
					id = Long.parseLong(columnIdString);
					// make sure the column ID is a valid one for this schema
					if (!existingColumnIds.contains(id)) {
						throw new IllegalArgumentException("The column ID " + columnIdString + " is not a valid column ID for this table");
					}
				}
				columnIdToColumnIndexMap.put(id, i);
			}
		}
		return columnIdToColumnIndexMap;
	}

	public static Map<Long, Integer> createColumnIdToSchemaIndexMap(List<ColumnModel> resultSchema) {
		Map<Long, Integer> columnIdToSchemaIndexMap = Maps.newHashMap();
		for (int i = 0; i < resultSchema.size(); i++) {
			columnIdToSchemaIndexMap.put(Long.parseLong(resultSchema.get(i).getId()), i);
		}
		return columnIdToSchemaIndexMap;
	}


	public static SetMultimap<Long, Long> createVersionToRowIdsMap(Map<Long, Long> currentVersionNumbers) {
		// create a map from version to set of row ids map
		SetMultimap<Long, Long> versions = HashMultimap.create();
		for (Entry<Long, Long> rowVersion : currentVersionNumbers.entrySet()) {
			versions.put(rowVersion.getValue(), rowVersion.getKey());
		}
		return versions;
	}

	public static SetMultimap<Long, RowAccessor> createVersionToRowsMap(Iterable<RowAccessor> currentRows) {
		// create a map from version to set of row ids map
		SetMultimap<Long, RowAccessor> versions = HashMultimap.create();
		for (RowAccessor row : currentRows) {
			versions.put(row.getVersionNumber(), row);
		}
		return versions;
	}

	public static SetMultimap<Long, Long> createVersionToRowIdsMap(Iterable<RowReference> refs) {
		// create a map from version to set of row ids map
		SetMultimap<Long, Long> versions = HashMultimap.create();
		for (RowReference ref : refs) {
			if (ref.getVersionNumber() == null) {
				throw new IllegalArgumentException("version cannot be null (row " + ref.getRowId() + ")");
			}
			versions.put(ref.getVersionNumber(), ref.getRowId());
		}
		return versions;
	}

	public static NodeIdAndVersion parseEntityIdValue(String entityId) {
		Matcher m = ENTITYID_PATTERN.matcher(entityId);
		if (!m.matches()) {
			return null;
		}
		Long id = Long.parseLong(m.group(1));
		String versionString = null;
		if (m.groupCount() == 3) {
			versionString = m.group(3);
		}
		Long version = null;
		if (versionString != null) {
			version = Long.parseLong(versionString);
		}
		return new NodeIdAndVersion(id, version);
	}

	public static List<SelectColumn> getSelectColumnsFromColumnIds(List<Long> columnIds, final SelectColumnMapper schema) {
		return Transform.toList(columnIds, new Function<Long, SelectColumn>() {
			@Override
			public SelectColumn apply(Long columnId) {
				return schema.getSelectColumnById(columnId);
			}
		});
	}

	public static SelectColumn createSelectColumn(String name, ColumnType columnType, String id) {
		SelectColumn newSelectColumn = new SelectColumn();
		newSelectColumn.setName(name);
		newSelectColumn.setColumnType(columnType);
		newSelectColumn.setId(id);
		return newSelectColumn;
	}

	public static SelectColumn createSelectColumn(ColumnModel model, boolean isAggregate) {
		return createSelectColumn(model.getName(), model.getColumnType(), isAggregate ? null : model.getId());
	}

	public static SelectColumnAndModel createSelectColumnAndModel(final SelectColumn selectColumn, final ColumnModel columnModel) {
		ValidateArgument.requirement(selectColumn != null || columnModel != null, "At least one of selectColumn or columnModel is required");
		return new SelectColumnAndModel() {

			@Override
			public SelectColumn getSelectColumn() {
				return selectColumn;
			}

			@Override
			public ColumnModel getColumnModel() {
				return columnModel;
			}

			@Override
			public String getName() {
				return selectColumn != null ? selectColumn.getName() : columnModel.getName();
			}

			@Override
			public ColumnType getColumnType() {
				return selectColumn != null ? selectColumn.getColumnType() : columnModel.getColumnType();
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((selectColumn == null) ? 0 : selectColumn.hashCode());
				result = prime * result + ((columnModel == null) ? 0 : columnModel.hashCode());
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
				SelectColumnAndModel other = (SelectColumnAndModel) obj;
				if (selectColumn == null) {
					if (other.getSelectColumn() != null)
						return false;
				} else if (!selectColumn.equals(other.getSelectColumn()))
					return false;
				if (columnModel == null) {
					if (other.getColumnModel() != null)
						return false;
				} else if (!columnModel.equals(other.getColumnModel()))
					return false;
				return true;
			}
		};
	}

	public static SelectColumnAndModel createSelectColumnAndModel(final ColumnModel columnModel, final boolean isAggregate) {
		ValidateArgument.required(columnModel, "columnModel");
		return new SelectColumnAndModel() {
			SelectColumn selectColumn = null;

			@Override
			public SelectColumn getSelectColumn() {
				if (selectColumn == null) {
					selectColumn = createSelectColumn(columnModel, isAggregate);
				}
				return selectColumn;
			}

			@Override
			public ColumnModel getColumnModel() {
				return columnModel;
			}

			@Override
			public String getName() {
				return columnModel.getName();
			}

			@Override
			public ColumnType getColumnType() {
				return columnModel.getColumnType();
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((columnModel == null) ? 0 : columnModel.hashCode());
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
				SelectColumnAndModel other = (SelectColumnAndModel) obj;
				if (getSelectColumn() == null) {
					if (other.getSelectColumn() != null)
						return false;
				} else if (!getSelectColumn().equals(other.getSelectColumn()))
					return false;
				if (columnModel == null) {
					if (other.getColumnModel() != null)
						return false;
				} else if (!columnModel.equals(other.getColumnModel()))
					return false;
				return true;
			}
		};
	}

	public static ColumnModelMapper createSingleColumnColumnMapper(ColumnModel column, boolean isAggregate) {
		return createColumnModelColumnMapper(Collections.<ColumnModel> singletonList(column), isAggregate);
	}

	public static ColumnMapper createColumnModelColumnMapper(final List<ColumnModel> columnModels, boolean isAggregate) {
		LinkedHashMap<String, SelectColumnAndModel> columnNameMap = Maps.newLinkedHashMap();
		Map<Long, SelectColumnAndModel> columnIdMap = Maps.newHashMap();
		for (ColumnModel columnModel : columnModels) {
			SelectColumnAndModel selectColumnAndModel = createSelectColumnAndModel(columnModel, isAggregate);
			columnNameMap.put(columnModel.getName(), selectColumnAndModel);
			columnIdMap.put(Long.parseLong(columnModel.getId()), selectColumnAndModel);
		}
		return createColumnMapper(columnNameMap, columnIdMap);
	}

	public static ColumnMapper createColumnMapper(final LinkedHashMap<String, SelectColumnAndModel> columnNameMap,
			final Map<Long, SelectColumnAndModel> columnIdMap) {
		AbstractColumnMapper columnMapper = new AbstractColumnMapper() {

			@Override
			protected LinkedHashMap<String, SelectColumnAndModel> createNameToModelMap() {
				return columnNameMap;
			}

			@Override
			protected Map<Long, SelectColumnAndModel> createIdToModelMap() {
				return columnIdMap;
			}

			@Override
			protected List<SelectColumnAndModel> createSelectColumnAndModelList() {
				return Lists.newArrayList(columnNameMap.values());
			}
		};
		return columnMapper;
	}

	public static ColumnMapper createColumnMapper(final List<SelectColumnAndModel> columnList) {
		ColumnMapper columnMapper = new AbstractColumnMapper() {
			@Override
			protected LinkedHashMap<String, SelectColumnAndModel> createNameToModelMap() {
				return Transform.toOrderedIdMap(columnList, SELECT_COLUMN_AND_MODEL_TO_NAME);
			}

			@Override
			protected Map<Long, SelectColumnAndModel> createIdToModelMap() {
				return Transform.toIdMap(columnList, SELECT_COLUMN_AND_MODEL_TO_ID);
			}

			@Override
			protected List<SelectColumnAndModel> createSelectColumnAndModelList() {
				return columnList;
			}
		};
		return columnMapper;
	}
	
	/**
	 * Is the passed string null or empty?
	 * @param string
	 * @return
	 */
	public static boolean isNullOrEmpty(String string){
		if(string == null){
			return true;
		}
		return "".equals(string.trim());
	}
	
	/**
	 * Extract all of the FileHandleIds from the passed list of rows.
	 * @param mapper
	 * @param rows
	 * @return
	 */
	public static Set<Long> getFileHandleIdsInRowSet(RowSet rowSet){
		SelectColumn[] columns = rowSet.getHeaders().toArray(new SelectColumn[rowSet.getHeaders().size()]);
		Set<Long> fileHandleIds = new HashSet<Long>();
		for(Row row: rowSet.getRows()){
			int columnIndex = 0;
			if(row.getValues() != null){
				for(String cellValue: row.getValues()){
					if(!isNullOrEmpty(cellValue)){
						if(columns[columnIndex] != null){
							if(ColumnType.FILEHANDLEID.equals(columns[columnIndex].getColumnType())){
								try {
									fileHandleIds.add(Long.parseLong(cellValue));
								} catch (NumberFormatException e) {
									throw new IllegalArgumentException("Passed a non-integer file handle id: "+cellValue);
								}						
							}
						}
					}
					columnIndex++;
				}
			}
		}
		return fileHandleIds;
	}
	
	/**
	 * Convert a collection of longs to a collection of strings.
	 * @param ids
	 * @return
	 */
	public static void convertLongToString(Collection<Long> in, Collection<String> out) {
		if(in == null){
			throw new IllegalArgumentException("Input cannot be null");
		}
		if(out == null){
			throw new IllegalArgumentException("Output cannot be null");
		}
		for(Long id: in){
			if(id != null){
				out.add(id.toString());
			}
		}
	}

	/**
	 * Convert a collection of strings to a collection of longs.
	 * @param ids
	 * @return
	 */
	public static void convertStringToLong(Collection<String> in, Collection<Long> out){
		if(in == null){
			throw new IllegalArgumentException("Input cannot be null");
		}
		if(out == null){
			throw new IllegalArgumentException("Output cannot be null");
		}
		for(String id: in){
			if(id != null){
				try {
					out.add(Long.parseLong(id));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
	}
	
	/**
	 * Create the MD5 Hex string of the given column models.
	 * @param schema
	 * @return
	 */
	public static String createSchemaMD5HexCM(List<ColumnModel> schema){
		List<Long> ids = TableModelUtils.getIds(schema);
		return createSchemaMD5Hex(ids);
	}
	
	/**
	 * Create the MD5 hex string of the given column model IDs.
	 * @param currentSchema
	 * @return
	 */
	public static String createSchemaMD5Hex(List<Long> columnIds){
		StringBuilder builder = new StringBuilder();
		builder.append("DEFAULT");
		for(Long id : columnIds){
			builder.append("+");
			builder.append(id);
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(builder.toString().getBytes("UTF-8"));
			return new String(Hex.encodeHex(digest.digest()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
