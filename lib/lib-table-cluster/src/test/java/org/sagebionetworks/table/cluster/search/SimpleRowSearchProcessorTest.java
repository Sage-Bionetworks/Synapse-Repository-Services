package org.sagebionetworks.table.cluster.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

@ExtendWith(MockitoExtension.class)
public class SimpleRowSearchProcessorTest {

	@InjectMocks
	private SimpleRowSearchProcessor processor;
	
	@Test
	public void testProcess() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT)
		);
		
		List<String> rowValues = Arrays.asList("value", "1", "big value");
		
		// Call under test
		Optional<String> result = processor.process(columns, rowValues);
		
		assertEquals("value 1 big value", result.get());
	}
	
	@Test
	public void testProcessWithNullValues() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT)
		);
		
		List<String> rowValues = Arrays.asList("value", null, "big value");
		
		// Call under test
		Optional<String> result = processor.process(columns, rowValues);
		
		assertEquals("value big value", result.get());
	}
	
	@Test
	public void testProcessWithEmptyValues() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT)
		);
		
		List<String> rowValues = Arrays.asList("value", "  ", "big value");
		
		// Call under test
		Optional<String> result = processor.process(columns, rowValues);
		
		assertEquals("value big value", result.get());
	}
	
	@Test
	public void testProcessWithNothingToIndex() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT),
				TableModelTestUtils.createColumn(4L, "four", ColumnType.STRING_LIST)
		);
		
		List<String> rowValues = Arrays.asList(null, null, " ", "[]");
		
		// Call under test
		Optional<String> result = processor.process(columns, rowValues);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void testProcessWithMultiValues() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT)
		);
		
		List<String> rowValues = Arrays.asList("value", "[\"a\",\"b\", \" c\"]", "big value");
		
		// Call under test
		Optional<String> result = processor.process(columns, rowValues);
		
		assertEquals("value a b c big value", result.get());
	}
	
	@Test
	public void testProcessWithMultiNullValues() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT)
		);
		
		List<String> rowValues = Arrays.asList("value", "[\"a\",\"b\", null]", "big value");
		
		// Call under test
		Optional<String> result = processor.process(columns, rowValues);
		
		assertEquals("value a b big value", result.get());
	}
	
	@Test
	public void testProcessWithMultiEmptyValues() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT)
		);
		
		List<String> rowValues = Arrays.asList("value", "", "big value");
		
		// Call under test
		Optional<String> result = processor.process(columns, rowValues);
		
		assertEquals("value big value", result.get());
	}
	
	@Test
	public void testProcessWithNullColumns() {
		
		List<ColumnModel> columns = null;
		List<String> rowValues = Arrays.asList("a", "b");
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.process(columns, rowValues);
		});
		
		assertEquals("columns is required.", ex.getMessage());
	}
	
	@Test
	public void testProcessWithNullRowValues() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT)
		);
		List<String> rowValues = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.process(columns, rowValues);
		});
		
		assertEquals("rowValues is required.", ex.getMessage());
	}
	
	@Test
	public void testProcessWithSizeMismatch() {
		
		List<ColumnModel> columns = Arrays.asList(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.LARGETEXT)
		);
		List<String> rowValues = Arrays.asList("a", "b");
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.process(columns, rowValues);
		});
		
		assertEquals("The number of columns and row values must match.", ex.getMessage());
	}

}
