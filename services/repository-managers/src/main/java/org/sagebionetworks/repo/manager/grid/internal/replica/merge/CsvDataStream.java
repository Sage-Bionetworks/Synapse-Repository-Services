package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.sagebionetworks.repo.manager.grid.row.translator.ColumnTypeToConType;
import org.sagebionetworks.repo.manager.grid.row.translator.Translator;
import org.sagebionetworks.util.ValidateArgument;

import au.com.bytecode.opencsv.CSVReader;

public class CsvDataStream implements DataStream {
	
	private CSVReader csvReader;
	private String[] currentRow;
	private ColumnMapping[] columnMapping;
	private Translator[] columnTranslators;
	private boolean[] requiredColumns;
	
	/**
	 * @param csvReader           the reader, positioned past the header row.
	 * @param columnMapping       the ordered mapping of the CSV columns to import.
	 * @param requiredColumnNames the names of the columns the grid's bound JSON
	 *                            schema requires, empty when the grid has no bound
	 *                            schema.
	 */
	public CsvDataStream(CSVReader csvReader, ColumnMapping[] columnMapping, Set<String> requiredColumnNames) {
		this.csvReader = csvReader;
		// Move the cursor to the first row
		this.currentRow = getNextRow();
		ValidateArgument.requirement(currentRow != null, "The CSV file cannot be empty.");
		ValidateArgument.required(currentRow.length >= columnMapping.length, "The CSV file does not match the given schema.");
		this.columnMapping = columnMapping;
		this.columnTranslators = Arrays.stream(columnMapping).map(mapping -> 
			ColumnTypeToConType.lookUpType(mapping.getType()).getTranslator()
		).toArray(Translator[]::new);
		this.requiredColumns = new boolean[columnMapping.length];
		for (int i = 0; i < columnMapping.length; i++) {
			this.requiredColumns[i] = requiredColumnNames.contains(columnMapping[i].getColumnName());
		}
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
				// Upsert key columns are stored in a typed, NOT NULL temp table column and
				// drive the join against the grid's temp table, so a value that cannot be
				// represented as the column's type must fail the import rather than being
				// carried through as text.
				stringValue = checkUpsertKeyValue(stringValue);
				values[i] = columnTranslators[i].translateNullable(stringValue).getValue();
			} else {
				// Non-key columns are packed untyped into a JSON array (see
				// GridCsvImportDaoImpl#mapRow), so a value that doesn't fit the column's
				// declared type — e.g. inferred from a different revision's data — is
				// carried through as raw text instead of failing the whole import.
				// The cell's compact form is packed rather than its raw value because a
				// JSON array cannot otherwise tell an undefined cell from a null one.
				values[i] = columnTranslators[i].translateLeniently(stringValue, requiredColumns[i]).toCompact();
			}
		}
		
		return values;
	}
	
	static String checkUpsertKeyValue(String value) {
		ValidateArgument.requirement(value != null && !value.isBlank(), "The upsert key cannot have null or empty values.");
		return value;
	}

}
