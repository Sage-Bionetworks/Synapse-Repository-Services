package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;

import au.com.bytecode.opencsv.CSVReader;
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
	 * Delimiter used to list column model IDs as a string.
	 */
	public static final String COLUMN_MODEL_ID_STRING_DELIMITER = ",";
	

	/**
	 * This utility will validate and convert the passed RowSet to an output
	 * CSV written to a GZip stream.
	 * @param models
	 * @param set
	 * @param out
	 * @throws IOException
	 */
	public static void validateAnWriteToCSVgz(List<ColumnModel> models,	RowSet set, OutputStream out) throws IOException{
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
	public static void validateAndWriteToCSV(List<ColumnModel> models,
			RowSet set, CSVWriter out) {
		if (models == null)
			throw new IllegalArgumentException("Models cannot be null");
		validateRowSet(set);
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
	 * @param set
	 */
	public static void validateRowSet(RowSet set) {
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
	
	/**
	 * Count all of the empty or invalid rows in the set
	 * @param set
	 */
	public static int countEmptyOrInvalidRowIds(RowSet set){
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
	private static boolean isNullOrInvalid(Long rowId){
		if(rowId == null) return true;
		return rowId < 0;
	}
	
	/**
	 * Assign RowIDs and version numbers to each row in the set according to the passed range.
	 * @param set
	 * @param range
	 */
	public static void assignRowIdsAndVersionNumbers(RowSet set, IdRange range){
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
			}
		}
	}
	
	/**
	 * Read the passed CSV into a RowSet.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static RowSet readFromCSV(CSVReader reader, String tableId, List<String> headers) throws IOException{
		if(reader == null) throw new IllegalArgumentException("CSVReader cannot be null");
		if(headers == null) throw new IllegalArgumentException("headers cannot be null");
		if(tableId == null) throw new IllegalArgumentException("tableId cannot be null");
		String[] rowArray = null;
		List<Row> rows = new LinkedList<Row>();
		while((rowArray = reader.readNext()) != null){
			Row row = new Row();
			if(rowArray.length < 3) throw new IllegalArgumentException("Row does not contain at least three columns");
			row.setRowId(Long.parseLong(rowArray[0]));
			row.setVersionNumber(Long.parseLong(rowArray[1]));
			List<String> values = new LinkedList<String>();
			// Add the rest of the values
			for(int i=2; i<rowArray.length; i++){
				values.add(rowArray[i]);
			}
			row.setValues(values);
			rows.add(row);
		}
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(headers);
		set.setTableId(tableId);
		return set;
	}
	
	/**
	 * Read the passed Gzip CSV into a RowSet.
	 * @param zippedStream
	 * @return
	 * @throws IOException 
	 */
	public static RowSet readFromCSVgzStream(InputStream zippedStream, String tableId, List<String> headers) throws IOException{
		GZIPInputStream zipIn = null;
		InputStreamReader isr = null;
		CSVReader csvReader = null;
		try{
			zipIn = new GZIPInputStream(zippedStream);
			isr = new InputStreamReader(zipIn);
			csvReader = new CSVReader(isr);
			return readFromCSV(csvReader, tableId, headers);
		}finally{
			if(csvReader != null){
				csvReader.close();
			}
		}
	}
	
	/**
	 * Extract the headers from a list of column Models.s
	 * @param models
	 * @return
	 */
	public static List<String> getHeaders(List<ColumnModel> models){
		if(models == null) throw new IllegalArgumentException("ColumnModels cannot be null");
		List<String> headers = new LinkedList<String>();
		for(ColumnModel model: models){
			if(model.getId() == null) throw new IllegalArgumentException("ColumnModel ID cannot be null");
			headers.add(model.getId());
		}
		return headers;
	}
	
	/**
	 * Create a delimited string of column model IDs.
	 * @param models
	 * @return
	 */
	public static String createDelimitedColumnModelIdString(List<String> headers){
		if(headers == null) throw new IllegalArgumentException("headers cannot be null");
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for(String id: headers){
			if(!first){
				builder.append(COLUMN_MODEL_ID_STRING_DELIMITER);
			}
			builder.append(id);
			first=false;
		}
		return builder.toString();
	}
	
	/**
	 * Read the list of column model ids from the passed delimited string.
	 * @param in
	 * @return
	 */
	public static List<String> readColumnModelIdsFromDelimitedString(String in){
		if(in == null) throw new IllegalArgumentException("String cannot be null");
		String[] split = in.split(COLUMN_MODEL_ID_STRING_DELIMITER);
		List<String> result = new LinkedList<String>();
		for(String id: split){
			result.add(id);
		}
		return result;
	}
	
	/**
	 * Create one column of each type.
	 * @return
	 */
	public static List<ColumnModel> createOneOfEachType(){
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for(int i=0; i<ColumnType.values().length; i++){
			ColumnType type = ColumnType.values()[i];
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(type);
			cm.setName("i"+i);
			cm.setId(""+i);
			results.add(cm);
		}
		return results;
	}
	
	/**
	 * Create the given number of rows.
	 * @param cms
	 * @param count
	 * @return
	 */
	public static List<Row> createRows(List<ColumnModel> cms, int count){
		List<Row> rows = new LinkedList<Row>();
		for(int i=0; i<count; i++){
			Row row = new Row();
			// Add a value for each column
			List<String> values = new LinkedList<String>();
			for(ColumnModel cm: cms){
				if(ColumnType.STRING.equals(cm.getColumnType())){
					values.add("string"+i);
				}else if(ColumnType.LONG.equals(cm.getColumnType())){
					values.add(""+i);
				}else if(ColumnType.BOOLEAN.equals(cm.getColumnType())){
					if(i % 2 > 0){
						values.add(Boolean.TRUE.toString());
					}else{
						values.add(Boolean.FALSE.toString());
					}
				}else if(ColumnType.FILEHANDLEID.equals(cm.getColumnType())){
					values.add(""+i);
				}else if(ColumnType.DOUBLE.equals(cm.getColumnType())){
					values.add(""+(i*3.41));
				}else{
					throw new IllegalArgumentException("Unknown ColumnType: "+cm.getColumnType());
				}
			}
			row.setValues(values);
			rows.add(row);
		}
		return rows;
	}
}
