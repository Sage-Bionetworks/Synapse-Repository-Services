package org.sagebionetworks.table.cluster.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.ColumnTypeInfo;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Utilities for working with Tables and Row data.
 * 
 * @author jmhill
 * 
 */
public class TableModelUtils {

	private static final String TABLE_LOCK_PREFIX = "TABLE-LOCK-";

	public static final String UTF_8 = "UTF-8";

	public static final Function<Long, String> LONG_TO_STRING = new Function<Long, String>() {
		@Override
		public String apply(Long input) {
			return input.toString();
		}
	};
	
	private static final String PARTIAL_ROW_KEY_NOT_A_VALID = "PartialRow.value.key: '%s' is not a valid column ID for row ID: %s";
	
	public static final String EXCEEDS_MAX_SIZE_TEMPLATE = "Request exceeds the maximum number of bytes per request.  Maximum : %1$s bytes";

	private static final String INVALID_VALUE_TEMPLATE = "Value at [%1$s,%2$s] was not a valid %3$s. %4$s";
	
	/**
	 * Delimiter used to list column model IDs as a string.
	 */
	public static final String COLUMN_MODEL_ID_STRING_DELIMITER = ",";

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
	
	/**
	 * Write a SparseChangeSetDto to the given output stream as GZIP compressed JSON.
	 * @param set
	 * @param out
	 * @throws IOException
	 */
	public static void writeSparesChangeSetToGz(SparseChangeSetDto set, OutputStream out) throws IOException {
		GZIPOutputStream zipOut = null;
		try{
			zipOut = new GZIPOutputStream(out);
			String jsonString = EntityFactory.createJSONStringForEntity(set);
			IOUtils.write(jsonString, zipOut, UTF_8);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}finally{
			if(zipOut != null){
				zipOut.flush();
				zipOut.close();
			}
			if(out != null){
				out.close();
			}
		}
	}

	
	/**
	 * Validate the given SparseChangeSetDto.
	 * @param set
	 */
	public static void validateRowSet(SparseChangeSet set) {
		ValidateArgument.required(set, "SparseChangeSet");
		ValidateArgument.required(set.getSchema(), "SparseChangeSet.schema");
		ValidateArgument.requirement(set.getRowCount() > 0, "SparseChangeSet must contain at least one row.");
		ValidateArgument.required(set.getTableId(), "SparseChangeSet.tableId");
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


	/**
	 * Validate that the given value conforms to the given ColumnModel.
	 * 
	 * @param value
	 * @param cm
	 * @return
	 */
	public static String validateValue(String value, ColumnModel cm) {
		switch (cm.getColumnType()) {
		case STRING:
			if (cm.getMaximumSize() == null)
				throw new IllegalArgumentException("String columns must have a maximum size");
			if (cm.getMaximumSize() > ColumnConstants.MAX_ALLOWED_STRING_SIZE){
				throw new IllegalArgumentException("Exceeds the maximum number of character: "+ColumnConstants.MAX_ALLOWED_STRING_SIZE);
			}
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
		default:
			// All other types are handled by the type specific parser.
			ColumnTypeInfo info = ColumnTypeInfo.getInfoForType(cm.getColumnType());
			Object objectValue = info.parseValueForDatabaseWrite(value);
			return objectValue.toString();
		}
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
	public static String translateRowValueFromQuery(String value, ColumnTypeInfo columnType) {
		if(value == null){
			return null;
		}
		if(columnType == null){
			return null;
		}
		return columnType.parseValueForDatabaseRead(value);
	}

	/**
	 * Count all of the empty or invalid rows in the set
	 * @param set
	 */
	public static int countEmptyOrInvalidRowIds(SparseChangeSet set) {
		validateRowSet(set);
		int count = 0;
		for (SparseRow row : set.rowIterator()) {
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
	public static void assignRowIdsAndVersionNumbers(SparseChangeSet set, IdRange range) {
		validateRowSet(set);
		Long id = range.getMinimumId();
		for (SparseRow row : set.rowIterator()) {
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
	public static List<Row> readFromCSV(CSVReader reader) throws IOException {
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
	public static void scanFromCSV(CSVReader reader, RowHandler handler) throws IOException {
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
	 * Read GZIP compressed JSON from the passed stream.
	 * 
	 * @param zippedStream
	 * @return
	 * @throws IOException
	 */
	public static SparseChangeSetDto readSparseChangeSetDtoFromGzStream(InputStream zippedStream) throws IOException {
		GZIPInputStream zipIn = null;
		try{
			zipIn = new GZIPInputStream(zippedStream);
			String json = IOUtils.toString(zipIn, UTF_8);
			return EntityFactory.createEntityFromJSONString(json, SparseChangeSetDto.class);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}finally{
			if(zipIn != null){
				zipIn.close();
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
		CSVReader csvReader = null;
		try{
			zipIn = new GZIPInputStream(zippedStream);
			isr = new InputStreamReader(zipIn);
			csvReader = new CSVReader(isr);
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
	public static List<String> getIds(List<ColumnModel> models) {
		if(models == null) throw new IllegalArgumentException("ColumnModels cannot be null");
		List<String> ids = Lists.newArrayListWithCapacity(models.size());
		for(ColumnModel model: models){
			if(model.getId() == null) throw new IllegalArgumentException("ColumnModel ID cannot be null");
			ids.add(model.getId());
		}
		return ids;
	}
	
	/**
	 * Given a list of SelectColumns get the list of Column Ids.
	 * @param models
	 * @return
	 */
	public static List<String> getColumnIdsFromSelectColumns(List<SelectColumn> models) {
		ValidateArgument.required(models, "SelectColumns");
		List<String> ids = Lists.newArrayListWithCapacity(models.size());
		for(SelectColumn model: models){
			if(model == null){
				ids.add(null);
			}else{
				ValidateArgument.required(model.getId(), "SelectColumn.id");
				ids.add(model.getId());
			}
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
	public static List<SelectColumn> getSelectColumns(Collection<ColumnModel> models) {
		ValidateArgument.required(models, "models");
		List<SelectColumn> result = Lists.newArrayListWithCapacity(models.size());
		for (ColumnModel model : models) {
			result.add(createSelectColumn(model));
		}
		return result;
	}

	/**
	 * Create a delimited string of column model IDs.
	 * 
	 * @param models
	 * @return
	 */
	public static String createDelimitedColumnModelIdString(List<String> ids) {
		ValidateArgument.required(ids, "headers");
		return StringUtils.join(ids, COLUMN_MODEL_ID_STRING_DELIMITER);
	}
	
	/**
	 * Read the list of column model ids from the passed delimited string.
	 * @param in
	 * @return
	 */
	public static List<String> readColumnModelIdsFromDelimitedString(String in) {
		ValidateArgument.required(in, "String");
		String[] split = in.split(COLUMN_MODEL_ID_STRING_DELIMITER);
		List<String> ids = Lists.newArrayListWithCapacity(split.length);
		for (String idString : split) {
			ids.add(idString);
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
	public static Map<Long, Long> getDistictValidRowIds(Iterable<SparseRow> rows) {
		ValidateArgument.required(rows, "rows");
		Map<Long, Long> distictRowIds = Maps.newHashMap();
		for (SparseRow ref : rows) {
			if (!isNullOrInvalid(ref.getRowId())) {
				if(!ref.isDelete()){
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
	public static RowSet convertToSchemaAndMerge(List<RawRowSet> sets, List<ColumnModel> columns, String tableId,
			String etag) {
		// Prepare the final set
		RowSet out = new RowSet();
		out.setTableId(tableId);
		out.setRows(new LinkedList<Row>());
		out.setHeaders(TableModelUtils.getSelectColumns(columns));
		out.setEtag(etag);
		// Transform each
		for (RawRowSet set : sets) {
			// Transform each and merge the results
			convertToSchemaAndMerge(set, columns, out);
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
	public static void convertToSchemaAndMerge(RawRowSet in, List<ColumnModel> columns, RowSet out) {
		Map<String, Integer> columnIndexMap = createColumnIdToIndexMap(in);
		// Now convert each row into the requested format.
		// Process each row
		for (Row row : in.getRows()) {
			// First convert the values to
			if (row.getValues() == null) {
				continue;
			}
			Row newRow = convertToSchemaAndMerge(row, columnIndexMap, columns);
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
	public static Row convertToSchemaAndMerge(Row row, Map<String, Integer> columnIndexMap, List<ColumnModel> columns) {
		// Create the new row
		Row newRow = new Row();
		newRow.setRowId(row.getRowId());
		newRow.setVersionNumber(row.getVersionNumber());
		List<String> newValues = Lists.newArrayListWithCapacity(columns.size());
		newRow.setValues(newValues);

		// Now process all of the columns as defined by the schema
		for (ColumnModel model : columns) {
			String value = null;
			Integer valueIndex = columnIndexMap.get(model.getId());
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
	public static int calculateMaxRowSizeForSelectColumn(List<SelectColumn> columns) {
		int size = 0;
		for (SelectColumn scm : columns) {
			if (scm.getColumnType() == null) {
				// we don't know the type, now what?
				size += 64;
			}else {
				// we don't know the max size, now what?
				size += calculateMaxSizeForType(scm.getColumnType(), ColumnConstants.MAX_ALLOWED_STRING_SIZE);
			}
		}
		return size;
	}
	
	/**
	 * Given a list of SelectColumns and a name to schema map, calculate the maximum size of each
	 * row in bytes.
	 * @param columns
	 * @param nameToSchemaMap
	 * @return
	 */
	public static int calculateMaxRowSize(List<SelectColumn> columns, Map<String, ColumnModel> nameToSchemaMap) {
		int size = 0;
		for (SelectColumn scm : columns) {
			// Lookup the column by name
			ColumnModel column = nameToSchemaMap.get(scm.getName());
			if(column != null){
				size += calculateMaxSizeForType(column.getColumnType(), column.getMaximumSize());
			}else{
				// Since the size is unknown, the max allowed size is used.
				size += calculateMaxSizeForType(scm.getColumnType(), ColumnConstants.MAX_ALLOWED_STRING_SIZE);
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
			return ColumnConstants.SIZE_OF_LARGE_TEXT_FOR_COLUMN_SIZE_ESTIMATE_BYTES;	
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
		case USERID:
			return ColumnConstants.MAX_USER_ID_BYTES_AS_STRING;
		}
		throw new IllegalArgumentException("Unknown ColumnType: " + type);
	}
	
	
	/**
	 * Calculate the amount of memory needed load the given row.
	 * 
	 * @param row
	 * @return
	 */
	public static int calculateActualRowSize(SparseRowDto row){
		// row ID + row version.
		int bytes = ColumnConstants.MINIMUM_ROW_SIZE;
		if(row.getValues() != null){
			for(String key: row.getValues().keySet()){
				// Include references to both the key and value and arrays
				bytes += ColumnConstants.MINUMUM_ROW_VALUE_SIZE;
				// Include the size of the key
				bytes += key.length()*(ColumnConstants.MAX_BYTES_PER_CHAR_MEMORY);
				String value = row.getValues().get(key);
				if (value != null) {
					bytes += value.length()
							* (ColumnConstants.MAX_BYTES_PER_CHAR_MEMORY);
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
	public static boolean isRequestWithinMaxBytePerRequest(List<ColumnModel> columns, int rowCount, int maxBytesPerRequest){
		// What is the size per row
		int maxBytesPerRow = calculateMaxRowSize(columns);
		int neededBytes = rowCount*maxBytesPerRow;
		return neededBytes <= maxBytesPerRequest;
	}
	
	/**
	 * Validate the given RowSet is within the maximum number of bytes.
	 * 
	 * @param columns
	 * @param rowSet
	 * @param maxBytesPerRequest
	 */
	public static void validateRequestSize(List<ColumnModel> columns, RowSet rowSet, int maxBytesPerRequest){
		// Validate the request is under the max bytes per requested
		if (!TableModelUtils.isRequestWithinMaxBytePerRequest(columns, rowSet.getRows().size(), maxBytesPerRequest)) {
			throw new IllegalArgumentException(String.format(EXCEEDS_MAX_SIZE_TEMPLATE, maxBytesPerRequest));
		}
	}
	
	/**
	 * Validate the given PartialRowSet is within the maximum number of bytes.
	 * @param rowSet
	 * @param maxBytesPerRequest
	 */
	public static void validateRequestSize(PartialRowSet rowSet, int maxBytesPerRequest){
		// count the characters in the request
		int totalSizeBytes = calculatePartialRowSetBytes(rowSet);
		if(totalSizeBytes >  maxBytesPerRequest){
			throw new IllegalArgumentException(String.format(EXCEEDS_MAX_SIZE_TEMPLATE, maxBytesPerRequest));
		}
	}
	
	/**
	 * Calculate the number of bytes used by the given PartialRowSet.
	 * 
	 * @param rowSet
	 * @return
	 */
	public static int calculatePartialRowSetBytes(PartialRowSet rowSet){
		int totalSizeBytes = 0;
		for(PartialRow row: rowSet.getRows()){
			totalSizeBytes += calculatetPartialRowBytes(row);
		}
		return totalSizeBytes;
	}
	
	/**
	 * Calculate the number of bytes used for the given PartialRow.
	 * @param row
	 * @return
	 */
	public static int calculatetPartialRowBytes(PartialRow row){
		int charCount = 0;
		if(row.getValues() == null){
			return charCount;
		}
		for(String key: row.getValues().keySet()){
			charCount += key.length();
			String value = row.getValues().get(key);
			if(value != null){
				charCount += value.length();
			}
		}
		return ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8*charCount;
	}
	
	/**
	 * This is the key used to gate access to a single table.
	 * This lock is used both to update the table index and also when a query is run.
	 * @param tableId
	 * @return
	 */
	public static String getTableSemaphoreKey(IdAndVersion tableId){
		if(tableId == null) {
			throw new IllegalArgumentException("TableId cannot be null");
		}
		StringBuilder builder = new StringBuilder(TABLE_LOCK_PREFIX);
		builder.append(tableId.getId());
		if(tableId.getVersion().isPresent()) {
			builder.append("-").append(tableId.getVersion().get());
		}
		return builder.toString();
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
	 * Map the ID of each column to its ColumnModel.
	 * @param columns
	 * @return
	 */
	public static LinkedHashMap<String, ColumnModel> createIdToColumnModelMap(List<ColumnModel> columns) {
		LinkedHashMap<String, ColumnModel> map = Maps.newLinkedHashMap();
		for (ColumnModel cm : columns) {
			map.put(cm.getId(), cm);
		}
		return map;
	}

	/**
	 * Map the column id to the column index.
	 * 
	 * @param rowset
	 * @return
	 */
	public static Map<String, Integer> createColumnIdToIndexMap(RawRowSet rowset) {
		return createColumnIdToIndexMap(rowset.getIds());
	}

	/**
	 * Map the input list of headers to their position in the list
	 * 
	 * @param headers
	 * @return
	 */
	public static Map<String, Integer> createColumnIdToIndexMap(List<String> columnIds) {
		Map<String, Integer> columnIndexMap = Maps.newHashMap();
		int index = 0;
		for (String columnId : columnIds) {
			if(columnId != null){
				columnIndexMap.put(columnId, index);
			}
			index++;
		}
		return columnIndexMap;
	}
	
	/**
	 * Create a list of all column models included in the provided details.
	 * 
	 * @param details
	 * @return
	 */
	public static List<ColumnModel> createListOfAllColumnModels(Iterable<ColumnChangeDetails> details) {
		List<ColumnModel> fullList = new LinkedList<>();
		for(ColumnChangeDetails detail: details) {
			if(detail.getNewColumn() != null) {
				fullList.add(detail.getNewColumn());
			}
			if(detail.getOldColumn() != null) {
				fullList.add(detail.getOldColumn());
			}
		}
		return fullList;
	}
	
	/**
	 * Map column Id to column Models for all columns in the details.
	 * 
	 * @param columns
	 * @return
	 */
	public static Map<Long, ColumnModel> createIDtoColumnModeMapDetails(Iterable<ColumnChangeDetails> details) {
		List<ColumnModel> fullList = createListOfAllColumnModels(details);
		return createIDtoColumnModelMap(fullList);
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
	public static String[] createColumnNameHeader(List<SelectColumn> selectColumns, boolean includeRowIdAndVersion, boolean includeRowEtag) {
		List<String> outHeaderList = Lists.newArrayListWithCapacity(selectColumns.size() + 2);
		if (includeRowIdAndVersion) {
			outHeaderList.add(TableConstants.ROW_ID);
			outHeaderList.add(TableConstants.ROW_VERSION);
			if(includeRowEtag){
				outHeaderList.add(TableConstants.ROW_ETAG);
			}
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
	public static String[] writeRowToStringArray(Row row, boolean includeRowIdAndVersion, boolean includeRowEtag){
		String[] array = null;
		// Write this row
		if(!includeRowIdAndVersion){
			// For aggregates just write the values to the array.
			array = row.getValues().toArray(new String[row.getValues().size()]);
		}else{
			// For non-aggregates the rowId and rowVersion must also be written
			int size = 2;
			if(includeRowEtag){
				size++;
			}
			int index = 0;
			array = new String[row.getValues().size()+size];
			array[index++] = row.getRowId().toString();
			array[index++] = row.getVersionNumber().toString();
			if(includeRowEtag){
				array[index++] = row.getEtag();
			}
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
										+ name + " is not a valid column name or id). Header row: " + StringUtils.join(rowValues, ','));
					}
				}
				columnIdToColumnIndexMap.put(id, i);
			}
		}
		return columnIdToColumnIndexMap;
	}

	public static SelectColumn createSelectColumn(String name, ColumnType columnType, String id) {
		SelectColumn newSelectColumn = new SelectColumn();
		newSelectColumn.setName(name);
		newSelectColumn.setColumnType(columnType);
		newSelectColumn.setId(id);
		return newSelectColumn;
	}

	public static SelectColumn createSelectColumn(ColumnModel model) {
		return createSelectColumn(model.getName(), model.getColumnType(), model.getId());
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
	 * Create the MD5 hex string of the given column model IDs.
	 * Note: The resulting MD5 is independent of order.
	 * @param currentSchema
	 * @return
	 */
	public static String createSchemaMD5Hex(List<String> columnIds){
		// Sort the IDs to yield the same MD5 regardless of order.
		columnIds = new LinkedList<String>(columnIds);
		Collections.sort(columnIds);
		StringBuilder builder = new StringBuilder();
		builder.append("DEFAULT");
		for(String id : columnIds){
			builder.append("+");
			builder.append(id);
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(builder.toString().getBytes(UTF_8));
			return new String(Hex.encodeHex(digest.digest()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Create a List of SelectColumn in the same order as the passed list of column model IDs.
	 * Each SelectColumn is derived from the provided schema according to ID.  A null value
	 * will be added to the result list for any column model ID that does not match the given schema.
	 * 
	 * @param columnIds
	 * @param schema
	 * @return
	 */
	public static List<SelectColumn> getSelectColumnsFromColumnIds(List<String> columnIds, final List<ColumnModel> schema) {
		Map<String, ColumnModel> schemaMap = new HashMap<String, ColumnModel>(schema.size());
		for(ColumnModel cm: schema){
			schemaMap.put(cm.getId(), cm);
		}
		List<SelectColumn> results = new LinkedList<SelectColumn>();
		for(String id: columnIds){
			SelectColumn select = null;
			ColumnModel cm = schemaMap.get(id);
			if(cm != null){
				select = createSelectColumn(cm);
			}
			results.add(select);
		}
		return results;
	}
	
	/**
	 * Create a new RowChangeSet from a full RowSet.
	 * 
	 * @param rowSet
	 * @param schema
	 * @param versionNumber
	 */
	public static SparseChangeSet createSparseChangeSet(RowSet rowSet, List<ColumnModel> schema) {
		SparseChangeSet changeSet = new SparseChangeSet(rowSet.getTableId(), schema);
		changeSet.setEtag(rowSet.getEtag());
		List<String> rowSetHeaderIds = getColumnIdsFromSelectColumns(rowSet.getHeaders());
		Map<String, Integer> headerIdToIndex = createColumnIdToIndexMap(rowSetHeaderIds);
		// Add all rows
		for (Row row : rowSet.getRows()) {
			SparseRow sparse = changeSet.addEmptyRow();
			sparse.setRowId(row.getRowId());
			sparse.setVersionNumber(row.getVersionNumber());
			sparse.setRowEtag(row.getEtag());
			if(row.getValues() != null && !row.getValues().isEmpty()){
				// add each value that matches the schema
				for(ColumnModel column: schema){
					Integer index = headerIdToIndex.get(column.getId());
					if(index != null){
						if(index < row.getValues().size()){
							String value = row.getValues().get(index);
							sparse.setCellValue(column.getId(), value);
						}
					}
				}
			}
		}
		return changeSet;
	}
	
	/**
	 * Create a RowSet from a RawRowSet.
	 * @param rawRowSet
	 * @param schema
	 * @return
	 */
	public static RowSet createRowSet(RawRowSet rawRowSet, List<ColumnModel> schema){
		RowSet rowSet = new RowSet();
		rowSet.setEtag(rawRowSet.getEtag());
		rowSet.setHeaders(getSelectColumns(schema));
		rowSet.setTableId(rawRowSet.getTableId());
		rowSet.setRows(new LinkedList<>(rawRowSet.getRows()));
		return rowSet;
	}
	
	/**
	 * Create a SparseChangeSet from a RawRowSet
	 * @param rawRowSet
	 * @param schema
	 * @return
	 */
	public static SparseChangeSet createSparseChangeSet(RawRowSet rawRowSet, List<ColumnModel> schema) {
		RowSet rowSet = createRowSet(rawRowSet, schema);
		return createSparseChangeSet(rowSet, schema);
	}
	
	/**
	 * Validate the given PartialRowSet against the given schema.
	 * @param partial
	 * @param schema
	 */
	public static void validatePartialRowSet(PartialRowSet partial, List<ColumnModel> schema){
		Set<Long> columnIds = new HashSet<Long>(schema.size());
		for(ColumnModel cm: schema){
			columnIds.add(Long.parseLong(cm.getId()));
		}
		for(PartialRow row: partial.getRows()){
			validatePartialRow(row, columnIds);
		}
	}
	
	/**
	 * Validate the PartialRow matches the headers.
	 * 
	 * @param row
	 * @param headers
	 */
	public static void validatePartialRow(PartialRow row, Set<Long> columnIds){
		ValidateArgument.required(row, "PartialRow");
		ValidateArgument.required(columnIds, "columnIds");
		if(row != null){
			if(row.getValues() != null){
				for(String key: row.getValues().keySet()){
					try {
						Long columnId = Long.parseLong(key);
						if(!columnIds.contains(columnId)){
							throw new IllegalArgumentException(String.format(PARTIAL_ROW_KEY_NOT_A_VALID, key, row.getRowId()));
						}
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(String.format(PARTIAL_ROW_KEY_NOT_A_VALID, key, row.getRowId()));
					}
				}
			}
		}
	}
	
	/**
	 * Translate from a PartialRowSet to a SparseChangeSetDto.
	 * @param versionNumber
	 * @param partialSet
	 * @return
	 */
	public static SparseChangeSetDto createSparseChangeSetFromPartialRowSet(TableRowChange lastRowChange, PartialRowSet partialSet){
		Long versionNumber = 0L;
		String lastEtag = null;
		if(lastRowChange != null){
			versionNumber = lastRowChange.getRowVersion();
			lastEtag = lastRowChange.getEtag();
		}
		List<SparseRowDto> sparseRows = new LinkedList<SparseRowDto>();
		for(PartialRow partialRow: partialSet.getRows()){
			// ignore partial rows with empty values
			if(partialRow.getValues() != null && partialRow.getValues().isEmpty()){
				continue;
			}
			SparseRowDto sparseRow = new SparseRowDto();
			sparseRow.setRowId(partialRow.getRowId());
			sparseRow.setVersionNumber(versionNumber);
			sparseRow.setEtag(partialRow.getEtag());
			sparseRow.setValues(partialRow.getValues());
			sparseRows.add(sparseRow);
		}
		SparseChangeSetDto results = new SparseChangeSetDto();
		results.setEtag(lastEtag);
		results.setRows(sparseRows);
		results.setTableId(partialSet.getTableId());;
		return results;
	}
	
	/**
	 * Wrap a TableUpdateRequest in a transaction.
	 * 
	 * @param toWrap
	 * @return
	 */
	public static TableUpdateTransactionRequest wrapInTransactionRequest(TableUpdateRequest toWrap){
		ValidateArgument.required(toWrap, "TableUpdateRequest");
		ValidateArgument.required(toWrap.getEntityId(), "TableUpdateRequest.entityId");
		TableUpdateTransactionRequest result = new TableUpdateTransactionRequest();
		result.setEntityId(toWrap.getEntityId());
		result.setChanges(new LinkedList<TableUpdateRequest>());
		result.getChanges().add(toWrap);
		return result;
	}
	
	/**
	 * Extract a single TableUpdateResponse from a TableUpdateTransactionResponse;
	 * @param transactionResponse
	 * @param clazz
	 * @return
	 */
	public static <T extends TableUpdateResponse> T extractResponseFromTransaction(AsynchronousResponseBody responseBody, Class<? extends T> clazz){
		ValidateArgument.required(responseBody, "AsynchronousResponseBody");
		ValidateArgument.required(clazz, "clazz");
		ValidateArgument.requirement(responseBody instanceof TableUpdateTransactionResponse, "TableUpdateTransactionResponse");
		TableUpdateTransactionResponse transactionResponse = (TableUpdateTransactionResponse) responseBody;
		ValidateArgument.required(transactionResponse.getResults(), "TableUpdateTransactionResponse.results");
		ValidateArgument.requirement(transactionResponse.getResults().size() == 1, "Expected one response in TableUpdateTransactionResponse.results");
		TableUpdateResponse singleResponse = transactionResponse.getResults().get(0);
		ValidateArgument.required(singleResponse, "TableUpdateTransactionResponse.results.get(0)");
		ValidateArgument.requirement(clazz.isInstance(singleResponse), "Expected response to be of type "+clazz.getName());
		return (T)singleResponse;
	}
	
	/**
	 * Given a new schema and an old schema, create the column changes needed
	 * to convert the old schema to the new schema.
	 * @param oldSchema
	 * @param newSchema
	 * @return
	 */
	public static List<ColumnChange> createChangesFromOldSchemaToNew(List<String> oldSchema, List<String> newSchema){
		if(oldSchema == null) {
			oldSchema = Collections.emptyList();
		}
		if(newSchema == null) {
			newSchema = Collections.emptyList();
		}
		Set<String> oldIdSet = new HashSet<>(oldSchema);
		Set<String> newIdSet = new HashSet<>(newSchema);
		int maxSize = Math.max(oldSchema.size(), newSchema.size());
		List<ColumnChange> changes = new ArrayList<>(maxSize);
		// remove any column in the old that is not in the new
		for(String oldColumnId: oldSchema) {
			if(!newIdSet.contains(oldColumnId)) {
				// remove this column
				ColumnChange remove = new ColumnChange();
				remove.setNewColumnId(null);
				remove.setOldColumnId(oldColumnId);
				changes.add(remove);
			}
		}
		// add any column in the new that is not in the old
		for(String newColumnId: newSchema) {
			if(!oldIdSet.contains(newColumnId)) {
				// add this column
				ColumnChange add = new ColumnChange();
				add.setNewColumnId(newColumnId);
				add.setOldColumnId(null);
				changes.add(add);
			}
		}
		return changes;
	}

}
