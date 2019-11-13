package org.sagebionetworks.table.cluster.utils;

import java.io.Reader;

import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.Constants;

public class CSVUtils {
	
	public static final String ERROR_CELLS_EXCEED_MAX = "One or more cell value exceeds the maxiumn number of characters: "+ ColumnConstants.MAX_LARGE_TEXT_CHARACTERS;
	/**
	 * When searching for a type this setups the order we check for.  Not all types are included.
	 */
	private static final ColumnType[] typesToCheck = new ColumnType[]{ColumnType.BOOLEAN, ColumnType.INTEGER, ColumnType.DOUBLE, ColumnType.DATE, ColumnType.ENTITYID, ColumnType.STRING, ColumnType.LARGETEXT};

	/**
	 * Create CSVReader with the correct parameters using the provided parameters or default values.
	 * @param reader
	 * @param body
	 * @param contentType
	 * @return
	 */
	public static CSVReader createCSVReader(Reader reader, CsvTableDescriptor descriptor, Long linesToSkip) {
		char separator = Constants.DEFAULT_SEPARATOR;
		char quotechar = Constants.DEFAULT_QUOTE_CHARACTER;
		char escape = Constants.DEFAULT_ESCAPE_CHARACTER;
		int skipLines = 0;
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
		return new CSVReader(reader, separator, quotechar, escape, skipLines);
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
	 * Check if the given value is compatible with the given columnType.
	 * If not, a ColumnModel that is compatible will be found and returned.
	 * 
	 * @param value If null, then the currentType will be returned.
	 * @param currentType If null, then a compatible type will be returned.
	 * @return
	 */
	public static ColumnModel checkType(String value, ColumnModel currentType) {
		// We can tell nothing from null or empty cells.
		if(value == null || "".equals(value.trim())){
			return currentType;
		}
		long currentMaxSize = 0;
		if(currentType != null){
			currentMaxSize = currentType.getMaximumSize();
		}
		// The current type determines where lookup starts.
		int startIndex = findIndexOf(currentType);
		// Try each type in order
		for(int i=startIndex; i<typesToCheck.length; i++){
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(typesToCheck[i]);
			long maxSize = Math.max(value.length(), currentMaxSize);
			cm.setMaximumSize(maxSize);
			try {
				TableModelUtils.validateValue(value, cm);
				// We have a match.
				return cm;
			} catch (IllegalArgumentException e) {
				// This type will not work so try the next.
				continue;
			}
		}
		// We failed to match a type
		throw new IllegalArgumentException(ERROR_CELLS_EXCEED_MAX);
	}
	
	/**
	 * Find the index of the given ColumnModel from the typesToCheck.
	 * @param currentType
	 * @return
	 */
	static int findIndexOf(ColumnModel currentType){
		if(currentType == null){
			return 0;
		}
		for(int i=0; i<typesToCheck.length; i++){
			if(typesToCheck[i].equals(currentType.getColumnType())){
				return i;
			}
		}
		throw new IllegalArgumentException("Unkown ColumnType: "+currentType.getColumnType());
	}

	/**
	 * make a rough guess as to what the extension for the file should be based on the separator.
	 * 
	 * @param separator
	 * @return
	 */
	public static String guessExtension(String separator) {
		String extension = "csv"; // by default, just use csv
		if ("\t".equals(separator)) {
			extension = "tsv";
		}
		return extension;
	}

	/**
	 * make a rough guess as to what the extension for the file should be based on the separator.
	 * 
	 * @param separator
	 * @return
	 */
	public static String guessContentType(String separator) {
		String contentType = "text/csv";
		if ("\t".equals(separator)) {
			contentType = "text/tsv";
		}
		return contentType;
	}
}
