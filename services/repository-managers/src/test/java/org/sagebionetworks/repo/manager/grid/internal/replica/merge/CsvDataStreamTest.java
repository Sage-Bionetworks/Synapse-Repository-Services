package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnType;

import au.com.bytecode.opencsv.CSVReader;

public class CsvDataStreamTest {

	private CSVReader csvReader;
	private ColumnMapping[] columnMapping;
	
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
	}
	
	@Test
	public void testStream() {
		dataStream = new CsvDataStream(csvReader, columnMapping);
		
		long rowId = 0;
		
		while(dataStream.hasNext()) {
			Object[] row = dataStream.next();
			
			assertArrayEquals(new Object[] {
				rowId,				// a
				rowId + 1L,			// b
				"data"+ rowId		// c
			}, row);
			
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
		
		dataStream = new CsvDataStream(csvReader, columnMapping);
		
		long rowId = 0;
		
		while(dataStream.hasNext()) {
			Object[] row = dataStream.next();
			
			assertArrayEquals(new Object[] {
				rowId + 1,		// b
				rowId,			// a
				"data"+ rowId	// c
			}, row);
			
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
		
		dataStream = new CsvDataStream(csvReader, columnMapping);
		
		long rowId = 0;
		
		while(dataStream.hasNext()) {
			Object[] row = dataStream.next();
			
			assertArrayEquals(new Object[] {
				rowId,			// a
				"data"+ rowId, 	// c
				rowId + 1L		// b
			}, row);
			
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
		
		dataStream = new CsvDataStream(csvReader, columnMapping);
		
		long rowId = 0;
		
		while(dataStream.hasNext()) {
			Object[] row = dataStream.next();
			
			assertArrayEquals(new Object[] {
				rowId,		// a
				rowId + 1L 	// b
			}, row);
			
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
		
		dataStream = new CsvDataStream(csvReader, columnMapping);
		
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
			new CsvDataStream(csvReader, columnMapping);
		}).getMessage());
		
	}
}
