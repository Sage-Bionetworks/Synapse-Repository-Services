package org.sagebionetworks.table.cluster.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnType;

@ExtendWith(MockitoExtension.class)
public class SimpleRowSearchProcessorTest {

	@InjectMocks
	private SimpleRowSearchProcessor processor;
	
	@Test
	public void testProcess() {
		
		Long tableId = 1L;
		
		TableRowData rowData = new TableRowData(tableId, Arrays.asList(
				new TableCellData(TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING), "value"),
				new TableCellData(TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER), "1"),
				new TableCellData(TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT), "big value")
		));
		
		// Call under test
		Optional<RowSearchContent> result = processor.process(rowData);
		
		assertEquals(new RowSearchContent(tableId, "value 1 big value"), result.get());
	}
	
	@Test
	public void testProcessWithNullValues() {
		
		Long tableId = 1L;
		
		TableRowData rowData = new TableRowData(tableId, Arrays.asList(
				new TableCellData(TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING), "value"),
				new TableCellData(TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER), null),
				new TableCellData(TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT), "big value")
		));
		
		// Call under test
		Optional<RowSearchContent> result = processor.process(rowData);
		
		assertEquals(new RowSearchContent(tableId, "value big value"), result.get());
	}
	
	@Test
	public void testProcessWithEmptyValues() {
		
		Long tableId = 1L;
		
		TableRowData rowData = new TableRowData(tableId, Arrays.asList(
				new TableCellData(TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING), "value"),
				new TableCellData(TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER), "   "),
				new TableCellData(TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT), "big value")
		));
		
		// Call under test
		Optional<RowSearchContent> result = processor.process(rowData);
		
		assertEquals(new RowSearchContent(tableId, "value big value"), result.get());
	}
	
	@Test
	public void testProcessWithNothingToIndex() {
		
		Long tableId = 1L;
		
		TableRowData rowData = new TableRowData(tableId, Arrays.asList(
				new TableCellData(TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING), null),
				new TableCellData(TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER), null),
				new TableCellData(TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT), " "),
				new TableCellData(TableModelTestUtils.createColumn(4L, "four", ColumnType.STRING_LIST), "[]")
		));
		
		// Call under test
		Optional<RowSearchContent> result = processor.process(rowData);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void testProcessWithMultiValues() {
		
		Long tableId = 1L;
		
		TableRowData rowData = new TableRowData(tableId, Arrays.asList(
				new TableCellData(TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING), "value"),
				new TableCellData(TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST), "[\"a\",\"b\", \" c\"]"),
				new TableCellData(TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT), "big value")
		));
		
		// Call under test
		Optional<RowSearchContent> result = processor.process(rowData);
		
		assertEquals(new RowSearchContent(tableId, "value a b c big value"), result.get());
	}
	
	@Test
	public void testProcessWithMultiNullValues() {
		
		Long tableId = 1L;
		
		TableRowData rowData = new TableRowData(tableId, Arrays.asList(
				new TableCellData(TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING), "value"),
				new TableCellData(TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST), "[\"a\",\"b\", null]"),
				new TableCellData(TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT), "big value")
		));
		
		// Call under test
		Optional<RowSearchContent> result = processor.process(rowData);
		
		assertEquals(new RowSearchContent(tableId, "value a b big value"), result.get());
	}
	
	@Test
	public void testProcessWithMultiEmptyValues() {
		
		Long tableId = 1L;
		
		TableRowData rowData = new TableRowData(tableId, Arrays.asList(
				new TableCellData(TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING), "value"),
				new TableCellData(TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST), ""),
				new TableCellData(TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT), "big value")
		));
		
		// Call under test
		Optional<RowSearchContent> result = processor.process(rowData);
		
		assertEquals(new RowSearchContent(tableId, "value big value"), result.get());
	}
	
	@Test
	public void testProcessWithNullRowData() {
		
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.process(null);
		});
		
		assertEquals("rowData is required.", ex.getMessage());
	}

}
