package org.sagebionetworks.table.cluster.avro;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.Row;

public class RowPFBUtilsTest {

	@Test
	public void testCreateEntiyIdFromRowId() {
		// call under test
		assertEquals("123", RowPFBUtils.createEntityIdFromRowId(new Row().setRowId(123L)));
		assertEquals("123_456", RowPFBUtils.createEntityIdFromRowId(new Row().setRowId(123L).setVersionNumber(456L)));
		assertNull(RowPFBUtils.createEntityIdFromRowId(new Row()));
	}

	@Test
	public void testCreateEntityIdFromRowIdWithNullRow() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			RowPFBUtils.createEntityIdFromRowId(null);
		}).getMessage();
		assertEquals("row is required.", message);
	}
	
	@Test
	public void testCreateEntityIdFromColumns() {
		// call under test
		assertEquals("a_b_c", RowPFBUtils.createEntityIdFromColumns(List.of("a","b","c","d"), new int[] {0,1,2}));
	}
	
	@Test
	public void testCreateEntityIdFromColumnsWithSingleColumn() {
		// call under test
		assertEquals("b", RowPFBUtils.createEntityIdFromColumns(List.of("a","b","c","d"), new int[] {1}));
	}
	
	@Test
	public void testCreateEntityIdFromColumnsWithOutOfOrder() {
		// call under test
		assertEquals("c_a_b", RowPFBUtils.createEntityIdFromColumns(List.of("a","b","c","d"), new int[] {2,0,1}));
	}
	
	@Test
	public void testCreateEntityIdFromColumnsWithNullValues() {
		// call under test
		assertEquals("c_null_b", RowPFBUtils.createEntityIdFromColumns(Arrays.asList(null,"b","c","d"), new int[] {2,0,1}));
	}
	
	@Test
	public void testCreateEntityIdFromColumnsWithMissingValues() {
		assertEquals("rowValues is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			RowPFBUtils.createEntityIdFromColumns(null, new int[] {0,1,2});
		}).getMessage());
	}
	
	@Test
	public void testCreateEntityIdFromColumnsWithMissingIndexRef() {
		assertEquals("idColumnIndexRef is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			RowPFBUtils.createEntityIdFromColumns(List.of("a","b","c","d"), null);
		}).getMessage());
	}
}
