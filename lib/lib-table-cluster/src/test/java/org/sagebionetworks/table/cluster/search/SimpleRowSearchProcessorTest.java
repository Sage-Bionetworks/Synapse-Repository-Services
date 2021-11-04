package org.sagebionetworks.table.cluster.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnType;

@ExtendWith(MockitoExtension.class)
public class SimpleRowSearchProcessorTest {

	@InjectMocks
	private SimpleRowSearchProcessor processor;
	
	@Test
	public void testProcess() {
		
		List<TypedCellValue> data = Arrays.asList(
			new TypedCellValue(ColumnType.STRING, "value"),
			new TypedCellValue(ColumnType.INTEGER, "1"),
			new TypedCellValue(ColumnType.LARGETEXT, "big value")
		);
		
		// Call under test
		String result = processor.process(data);
		
		assertEquals("value 1 big value", result);
	}
	
	@Test
	public void testProcessWithNullValues() {
		
		List<TypedCellValue> data = Arrays.asList(
			new TypedCellValue(ColumnType.STRING, "value"),
			new TypedCellValue(ColumnType.INTEGER, null),
			new TypedCellValue(ColumnType.LARGETEXT, "big value")
		);
		
		// Call under test
		String result = processor.process(data);
		
		assertEquals("value big value", result);
	}
	
	@Test
	public void testProcessWithEmptyValues() {
		
		List<TypedCellValue> data = Arrays.asList(
			new TypedCellValue(ColumnType.STRING, "value"),
			new TypedCellValue(ColumnType.INTEGER, "    "),
			new TypedCellValue(ColumnType.LARGETEXT, "big value")
		);
		
		// Call under test
		String result = processor.process(data);
		
		assertEquals("value big value", result);
	}
	
	@Test
	public void testProcessWithNothingToIndex() {
		
		List<TypedCellValue> data = Arrays.asList(
			new TypedCellValue(ColumnType.INTEGER, null),
			new TypedCellValue(ColumnType.LARGETEXT, "   "),
			new TypedCellValue(ColumnType.STRING_LIST, "[]")
		);
		
		// Call under test
		String result = processor.process(data);

		assertNull(result);
	}
	
	@Test
	public void testProcessWithMultiValues() {
		
		List<TypedCellValue> data = Arrays.asList(
			new TypedCellValue(ColumnType.STRING, "value"),
			new TypedCellValue(ColumnType.STRING_LIST, "[\"a\",\"b\", \" c\"]"),
			new TypedCellValue(ColumnType.LARGETEXT, "big value")
		);
		
		// Call under test
		String result = processor.process(data);
		
		assertEquals("value a b c big value", result);
	}
	
	@Test
	public void testProcessWithMultiNullValues() {
				
		List<TypedCellValue> data = Arrays.asList(
			new TypedCellValue(ColumnType.STRING, "value"),
			new TypedCellValue(ColumnType.STRING_LIST, "[\"a\",\"b\", null]"),
			new TypedCellValue(ColumnType.LARGETEXT, "big value")
		);
		
		// Call under test
		String result = processor.process(data);
		
		assertEquals("value a b big value", result);
	}
	
	@Test
	public void testProcessWithMultiEmptyValues() {
		
		List<TypedCellValue> data = Arrays.asList(
			new TypedCellValue(ColumnType.STRING, "value"),
			new TypedCellValue(ColumnType.STRING_LIST, ""),
			new TypedCellValue(ColumnType.LARGETEXT, "big value")
		);
		
		// Call under test
		String result = processor.process(data);
		
		assertEquals("value big value", result);
	}
	
	@Test
	public void testProcessWithNullRowData() {
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.process(null);
		});
		
		assertEquals("data is required.", ex.getMessage());
	}

}
