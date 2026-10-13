package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnType;

import au.com.bytecode.opencsv.CSVReader;

public class CsvDataStreamTest {

	private CSVReader csvReader;
	private ColumnMapping[] columnMapping;
	private Set<String> requiredColumnNames;
	
	private CsvDataStream dataStream;
	
	@BeforeEach
	public void beforeEach() {
		
		csvReader = new CSVReader(new StringReader(
			"0,1,data0,extra0"	+ System.lineSeparator() +
			"1,2,data1,etxra1" 	+ System.lineSeparator() +
			"2,3,data2,extra2" 	+ System.lineSeparator() +
			"3,4,data3,extra3" 	+ System.lineSeparator() +
			"4,5,data4,extra4" 	+ System.lineSeparator()
		));
		
		columnMapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 0, 0, true),
			new ColumnMapping("b", ColumnType.INTEGER, 1, 1, false),
			new ColumnMapping("c", ColumnType.STRING, 2, 2, false)
		};

		requiredColumnNames = Set.of();
	}
	
	@Test
	public void testStream() {
		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);
		
		long rowId = 0;
		
		while(dataStream.hasNext()) {
			Object[] row = dataStream.next();
			
			// An upsert key cell is mapped to its raw value, while every other cell is
			// mapped to the compact form of its ConValue.
			assertEquals("[" + rowId						// a
				+ ", [" + (rowId + 1) + "]"					// b
				+ ", [\"data" + rowId + "\"]]", 			// c
				Arrays.toString(row));
			
			rowId++;
		}
	}
	
	@Test
	public void testStreamWithOutOfOrderUpsertKey() {
		columnMapping = new ColumnMapping[] {
			new ColumnMapping("b", ColumnType.INTEGER, 1, 1, true),
			new ColumnMapping("a", ColumnType.INTEGER, 0, 0, true),
			new ColumnMapping("c", ColumnType.STRING, 2, 2, false)
		};
		
		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);
		
		long rowId = 0;
		
		while(dataStream.hasNext()) {
			Object[] row = dataStream.next();
			
			assertEquals("[" + (rowId + 1)					// b
				+ ", " + rowId								// a
				+ ", [\"data" + rowId + "\"]]", 			// c
				Arrays.toString(row));
			
			rowId++;
		}
	}
	
	@Test
	public void testStreamWithOutOfOrderCsvColumns() {
		
		csvReader = new CSVReader(new StringReader(
			"data0,1,0,extra0"	+ System.lineSeparator() +
			"data1,2,1,etxra1" 	+ System.lineSeparator() +
			"data2,3,2,extra2" 	+ System.lineSeparator() +
			"data3,4,3,extra3" 	+ System.lineSeparator() +
			"data4,5,4,extra4" 	+ System.lineSeparator()
		));
		
		columnMapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 2, 0, true),
			new ColumnMapping("c", ColumnType.STRING, 0, 2, false),
			new ColumnMapping("b", ColumnType.INTEGER, 1, 1, false)
		};
		
		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);
		
		long rowId = 0;
		
		while(dataStream.hasNext()) {
			Object[] row = dataStream.next();
			
			assertEquals("[" + rowId						// a
				+ ", [\"data" + rowId + "\"]"				// c
				+ ", [" + (rowId + 1) + "]]", 				// b
				Arrays.toString(row));
			
			rowId++;
		}
	}
	
	@Test
	public void testStreamWithMissingColumn() {
		csvReader = new CSVReader(new StringReader(
			"0,1,extra0"	+ System.lineSeparator() +
			"1,2,etxra1" 	+ System.lineSeparator() +
			"2,3,extra2" 	+ System.lineSeparator() +
			"3,4,extra3" 	+ System.lineSeparator() +
			"4,5,extra4" 	+ System.lineSeparator()
		));
		
		columnMapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 0, 0, true),
			new ColumnMapping("b", ColumnType.INTEGER, 1, 1, false)
		};
		
		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);
		
		long rowId = 0;
		
		while(dataStream.hasNext()) {
			Object[] row = dataStream.next();
			
			assertEquals("[" + rowId						// a
				+ ", [" + (rowId + 1) + "]]", 				// b
				Arrays.toString(row));
			
			rowId++;
		}
	}
	
	@Test
	public void testStreamWithEmptyUpsertKeyColumn() {
		csvReader = new CSVReader(new StringReader(
			"0,1,extra0"	+ System.lineSeparator() +
			"1,2,etxra1" 	+ System.lineSeparator() +
			",3,extra2" 	+ System.lineSeparator() +
			"3,4,extra3" 	+ System.lineSeparator() +
			"4,5,extra4" 	+ System.lineSeparator()
		));	
		
		columnMapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 0, 0, true),
			new ColumnMapping("b", ColumnType.INTEGER, 1, 1, false)
		};
		
		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);
		
		assertEquals("The upsert key cannot have null or empty values.", assertThrows(IllegalArgumentException.class, () -> {
			while(dataStream.hasNext()) {
				dataStream.next();
			}
		}).getMessage());
		
	}
	
	@Test
	public void testStreamWithEmptyCsv() {		
		csvReader = new CSVReader(new StringReader(""));
		
		assertEquals("The CSV file cannot be empty.", assertThrows(IllegalArgumentException.class, () -> {
			new CsvDataStream(csvReader, columnMapping, requiredColumnNames);
		}).getMessage());
		
	}

	/**
	 * A blank cell in a non-key INTEGER column must not fail the import: blank
	 * cells carry no type information during schema inference, so a column can
	 * legitimately be typed INTEGER despite having blanks.
	 */
	@Test
	public void testStreamWithBlankNonKeyIntegerCell() {
		csvReader = new CSVReader(new StringReader(
			"0,,data0" + System.lineSeparator()
		));

		requiredColumnNames = Set.of("b");

		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);

		// call under test
		Object[] row = dataStream.next();

		// The schema requires "b", so its blank cell carries an explicit null value that
		// the schema can then flag as invalid.
		assertEquals("[0, [null], [\"data0\"]]", Arrays.toString(row));
	}

	/**
	 * A blank cell in a column the schema does not require becomes an undefined
	 * value, so that the row simply has no value for that column and the schema
	 * does not flag an optional value the user left empty as invalid.
	 */
	@Test
	public void testStreamWithBlankOptionalNonKeyIntegerCell() {
		csvReader = new CSVReader(new StringReader(
			"0,,data0" + System.lineSeparator()
		));

		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);

		// call under test
		Object[] row = dataStream.next();

		assertEquals("[0, [0,0], [\"data0\"]]", Arrays.toString(row));
	}

	/**
	 * A non-key column may hold a value inferred from a different revision's data
	 * that doesn't fit its declared type (e.g. an INTEGER column upserted from a
	 * grid that has since gained non-numeric values). It must be carried through
	 * as raw text rather than failing the whole import.
	 */
	@Test
	public void testStreamWithUnparseableNonKeyIntegerCell() {
		csvReader = new CSVReader(new StringReader(
			"0,not-a-number,data0" + System.lineSeparator()
		));

		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);

		// call under test
		Object[] row = dataStream.next();

		assertEquals("[0, [\"not-a-number\"], [\"data0\"]]", Arrays.toString(row));
	}

	/**
	 * The upsert key columns are stored in a typed, NOT NULL temp table column and
	 * drive the join against the grid's temp table, so an unparseable key value
	 * must still fail the import rather than being carried through as text.
	 */
	@Test
	public void testStreamWithUnparseableUpsertKeyValue() {
		csvReader = new CSVReader(new StringReader(
			"not-a-number,1,data0" + System.lineSeparator()
		));

		dataStream = new CsvDataStream(csvReader, columnMapping, requiredColumnNames);

		// call under test
		assertThrows(NumberFormatException.class, dataStream::next);
	}
}
