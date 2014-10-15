package org.sagebionetworks.table.worker;

import java.io.Reader;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.csv.CsvNullReader;

public class CSVUtils {
	
	/**
	 * When searching for a type this setups the order we check for.  Not all types are included.
	 */
	public static final ColumnType[] typesToCheck = new ColumnType[]{ColumnType.BOOLEAN, ColumnType.INTEGER, ColumnType.DOUBLE, ColumnType.DATE, ColumnType.ENTITYID, ColumnType.STRING};

	/**
	 * Create CsvNullReader with the correct parameters using the provided parameters or default values.
	 * @param reader
	 * @param body
	 * @param contentType
	 * @return
	 */
	public static CsvNullReader createCSVReader(Reader reader, CsvTableDescriptor descriptor, Long linesToSkip) {
		char separator = CsvNullReader.DEFAULT_SEPARATOR;
		char quotechar = CsvNullReader.DEFAULT_QUOTE_CHARACTER;
		char escape = CsvNullReader.DEFAULT_ESCAPE_CHARACTER;
		int skipLines = CsvNullReader.DEFAULT_SKIP_LINES;
		if(descriptor != null){
			if (descriptor.getSeparator() != null) {
				if (descriptor.getSeparator().length() != 1) {
					throw new IllegalArgumentException(
							"CsvTableDescriptor.separator must be exactly one character.");
				}
				separator = descriptor.getSeparator().charAt(0);
			}
			if (descriptor.getQuoteCharacter() != null) {
				if (descriptor.getQuoteCharacter().length() != 1) {
					throw new IllegalArgumentException(
							"CsvTableDescriptor.quoteCharacter must be exactly one character.");
				}
				quotechar = descriptor.getQuoteCharacter()
						.charAt(0);
			}
			if (descriptor.getEscapeCharacter() != null) {
				if (descriptor.getEscapeCharacter().length() != 1) {
					throw new IllegalArgumentException(
							"CsvTableDescriptor.escapeCharacter must be exactly one character.");
				}
				escape = descriptor.getEscapeCharacter()
						.charAt(0);
			}			
		}
		if (linesToSkip != null) {
			skipLines = linesToSkip.intValue();
		}
		// Create the reader.
		return new CsvNullReader(reader, separator, quotechar, escape, skipLines);
	}
	
	/**
	 * Is the first line a header.  If null then true.
	 * @param descriptor
	 * @return
	 */
	public static boolean isFirstRowHeader(CsvTableDescriptor descriptor){
		if(descriptor != null){
			if(descriptor.getIsFirstLineHeader() != null){
				return descriptor.getIsFirstLineHeader();
			}
		}
		// default to true
		return true;
	}
	
	/**
	 * Do a full scan?  If null then false.
	 * 
	 * @param request
	 * @return
	 */
	public static boolean doFullScan(UploadToTablePreviewRequest request){
		if(request != null){
			if(request.getDoFullFileScan() != null){
				return request.getDoFullFileScan();
			}
		}
		return false;
	}
	
	/**
	 * Check the types for each column.
	 * @param cells
	 * @param currentTypes
	 */
	public static void checkTypes(String[] cells, ColumnModel[] currentTypes){
		// Check the type of each column
		for(int i=0; i<cells.length; i++){
			currentTypes[i] = checkType(cells[i], currentTypes[i]);
		}
	}
	
	/**
	 * Check the type of the given value.  If the current type is null then determine the type.
	 * If the type does not match then return a type that does match.
	 */
	public static ColumnModel checkType(String value, ColumnModel currentType){
		// We can tell nothing from null or empty cells.
		if(value == null || "".equals(value.trim())){
			return currentType;
		}
		if(currentType != null){
			try {
				TableModelUtils.validateValue(value, currentType);
				// type is the same
				return currentType;
			} catch (IllegalArgumentException e) {
				// That type will not work so go fish
				return checkType(value, null);
			}
		}else{
			// Try each type in order
			for(ColumnType testType: typesToCheck){
				ColumnModel cm = new ColumnModel();
				cm.setColumnType(testType);
				if(testType.equals(ColumnType.STRING)){
					cm.setMaximumSize(new Long(value.length()));
				}
				try {
					TableModelUtils.validateValue(value, cm);
					// We have a match.
					return cm;
				} catch (IllegalArgumentException e) {
					// That type will not work so go fish
					// try another
					continue;
				}
			}
		}
		// We failed to match a type
		throw new IllegalArgumentException("Failed to match a cell value to a ColumnType. Value: "+value);
	}
}
