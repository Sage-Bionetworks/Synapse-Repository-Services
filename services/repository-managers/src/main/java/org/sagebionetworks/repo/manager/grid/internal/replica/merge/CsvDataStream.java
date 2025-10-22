package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.io.IOException;
import java.util.Arrays;

import org.sagebionetworks.repo.manager.grid.row.translator.ColumnTypeToConType;
import org.sagebionetworks.repo.manager.grid.row.translator.Translator;
import org.sagebionetworks.util.ValidateArgument;

import au.com.bytecode.opencsv.CSVReader;

public class CsvDataStream implements DataStream {
	
	private CSVReader csvReader;
	private String[] currentRow;
	private ColumnMapping[] columnMapping;
	private Translator[] columnTranslators;
	
	public CsvDataStream(CSVReader csvReader, ColumnMapping[] columnMapping) {
		this.csvReader = csvReader;
		// Move the cursor to the first row
		this.currentRow = getNextRow();
		ValidateArgument.requirement(currentRow != null, "The CSV file cannot be empty.");
		ValidateArgument.required(currentRow.length >= columnMapping.length, "The CSV file does not match the given schema.");
		this.columnMapping = columnMapping;
		this.columnTranslators = Arrays.stream(columnMapping).map(mapping -> 
			ColumnTypeToConType.lookUpType(mapping.getType()).getTranslator()
		).toArray(Translator[]::new);
	}

	@Override
	public boolean hasNext() {
		return currentRow != null;
	}

	@Override
	public Object[] next() {
		Object[] mappedRow = mapCurrentRow();

		this.currentRow = getNextRow();
		
		return mappedRow;
	}
	
	String[] getNextRow() {
		try {
			return csvReader.readNext();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	Object[] mapCurrentRow() {
		Object[] values = new Object[columnMapping.length];
		
		// First we map the upsert key columns
		for (int i = 0; i < columnMapping.length; i++) {
			ColumnMapping mapping = columnMapping[i];
			
			String stringValue = currentRow[mapping.getCsvIndex()];
			
			if (mapping.isUpsertColumn()) {
				stringValue = checkUpsertKeyValue(stringValue);
			}
			
			values[i] = columnTranslators[i].translateNullable(stringValue).getValue();
		}
		
		return values;
	}
	
	static String checkUpsertKeyValue(String value) {
		ValidateArgument.requirement(value != null && !value.isBlank(), "The upsert key cannot have null or empty values.");
		return value;
	}

}
