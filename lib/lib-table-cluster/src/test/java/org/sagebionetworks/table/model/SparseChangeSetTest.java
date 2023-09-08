package org.sagebionetworks.table.model;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class SparseChangeSetTest {
	
	ColumnModel booleanColumn;
	ColumnModel stringColumn;
	ColumnModel doubleColumn;
	List<ColumnModel> schema;
	long versionNumber;
	SparseChangeSet changeSet;
	
	@BeforeEach
	public void before(){
		MockitoAnnotations.initMocks(this);
		booleanColumn = TableModelTestUtils.createColumn(1L, "aBoolean", ColumnType.BOOLEAN);
		stringColumn = TableModelTestUtils.createColumn(2L, "aString", ColumnType.STRING);
		doubleColumn = TableModelTestUtils.createColumn(3L, "aDouble", ColumnType.DOUBLE);
		schema = Arrays.asList(booleanColumn, stringColumn, doubleColumn);
		versionNumber = 101;
		changeSet = new SparseChangeSet("syn123",schema);
	}

	
	@Test
	public void testGetColumnModel(){
		assertEquals(booleanColumn, changeSet.getColumnModel(booleanColumn.getId()));
		assertEquals(stringColumn, changeSet.getColumnModel(stringColumn.getId()));
		assertEquals(doubleColumn, changeSet.getColumnModel(doubleColumn.getId()));
	}

	@Test
	public void testGetColumnModelNotFound(){
		String columnIdDoesNotExist = "-99";
		
		assertThrows(NotFoundException.class, () -> {
			changeSet.getColumnModel(columnIdDoesNotExist);
		});
	}
	
	@Test
	public void testGetColumnIndex(){
		assertEquals(new Integer(0), changeSet.getColumnIndex(booleanColumn.getId()));
		assertEquals(new Integer(1), changeSet.getColumnIndex(stringColumn.getId()));
		assertEquals(new Integer(2), changeSet.getColumnIndex(doubleColumn.getId()));
	}
	
	@Test
	public void testGetColumnIndexNotFound(){
		String columnIdDoesNotExist = "-99";
		assertThrows(NotFoundException.class, () -> {
			changeSet.getColumnIndex(columnIdDoesNotExist);
		});
	}
	
	@Test
	public void testNullSchema(){
		schema = null;
		assertThrows(IllegalArgumentException.class, () -> {
			changeSet = new SparseChangeSet("syn123", schema);
		});
	}
	
	@Test
	public void testSparseRowSetValueValidate(){
		// first row valid.
		SparseRow row = changeSet.addEmptyRow();
		row.setCellValue(booleanColumn.getId(), "true");
		row.setCellValue(stringColumn.getId(), "aString");
		row.setCellValue(doubleColumn.getId(), "2.21");
		// add invalid values to the next row
		SparseRow nextRow = changeSet.addEmptyRow();
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			nextRow.setCellValue(booleanColumn.getId(), "notABoolean");
		});
		
		assertTrue(e.getMessage().startsWith("Value at [1,0] was not a valid BOOLEAN"));
		assertFalse(nextRow.hasCellValue(booleanColumn.getId()));
		
		e = assertThrows(IllegalArgumentException.class, () -> {
			nextRow.setCellValue(stringColumn.getId(), "way too long................................................................................");
		});
		
		assertTrue(e.getMessage().startsWith("Value at [1,1] was not a valid STRING"));
		assertFalse(nextRow.hasCellValue(stringColumn.getId()));
		
		 e = assertThrows(IllegalArgumentException.class, () -> {
			nextRow.setCellValue(doubleColumn.getId(), "not a double");
		 });
		 
		assertTrue(e.getMessage().startsWith("Value at [1,2] was not a valid DOUBLE"));
		assertFalse(nextRow.hasCellValue(doubleColumn.getId()));
		
	}
	
	/**
	 * Only strings can have empty values
	 */
	@Test
	public void testSparseRowSetValuePLFM_2657(){
		// first row valid.
		SparseRow row = changeSet.addEmptyRow();
		row.setCellValue(booleanColumn.getId(), "");
		row.setCellValue(stringColumn.getId(), "");
		row.setCellValue(doubleColumn.getId(), "");
		// empty boolean should be treated as a null
		assertNull(row.getCellValue(booleanColumn.getId()));
		// strings can have empty values.
		assertEquals("", row.getCellValue(stringColumn.getId()));
		// empty values for doubles default to null
		assertNull(row.getCellValue(booleanColumn.getId()));
	}
	
	@Test
	public void testSparseRowHasCellValue(){
		SparseRow row = changeSet.addEmptyRow();
		row.setRowId(123L);
		String columnId = booleanColumn.getId();
		assertFalse(row.hasCellValue(columnId), "Cell should start no value");
		row.setCellValue(columnId, "true");
		assertTrue(row.hasCellValue(columnId));
	}
	
	@Test
	public void testSparseRowHasCellValueWithNull(){
		SparseRow row = changeSet.addEmptyRow();
		String columnId = stringColumn.getId();
		assertFalse(row.hasCellValue(columnId));
		// set a null value.
		row.setCellValue(columnId, null);
		// even though the value is null it should still have a value.
		assertTrue(row.hasCellValue(columnId));
	}
	
	@Test
	public void testSparseRowHasCellValueRemove(){
		SparseRow row = changeSet.addEmptyRow();
		String columnId = stringColumn.getId();
		assertFalse(row.hasCellValue(columnId));
		row.setCellValue(columnId, "aString");
		assertTrue(row.hasCellValue(columnId));
		row.removeValue(columnId);
		// should no longer have a value after a remove.
		assertFalse(row.hasCellValue(columnId));
	}
	
	@Test
	public void testGetRowValue(){
		SparseRow row = changeSet.addEmptyRow();
		String columnId = stringColumn.getId();
		row.setCellValue(columnId, "foo");
		String value = row.getCellValue(columnId);
		assertEquals("foo", value);
	}
	
	@Test
	public void testGetRowValueNotFound(){
		SparseRow row = changeSet.addEmptyRow();
		String columnId = stringColumn.getId();
		
		assertThrows(NotFoundException.class, () -> {			
			// should throw not found.
			row.getCellValue(columnId);
		});
	}
	
	@Test
	public void testRowIterator(){
		// Add three rows
		for(int i=0; i<3; i++){
			SparseRow row = changeSet.addEmptyRow();
			row.setRowId(new Long(i));
		}
		// iterate over the rows
		int i =0;
		for(SparseRow row: changeSet.rowIterator()){
			assertEquals(new Long(i), row.getRowId());
			assertEquals(i, row.getRowIndex());
			i++;
		}
		assertEquals(3, i);
	}
	
	@Test
	public void testSparseRowIsDelete(){
		SparseRow row = changeSet.addEmptyRow();
		// new rows start off as a delete since they have not values
		assertTrue(row.isDelete());
		// add a value
		row.setCellValue(booleanColumn.getId(), "false");
		// should no longer be a delete
		assertFalse(row.isDelete());
	}
	
	@Test
	public void testGroupByValidValues(){
		ColumnModel c1 = TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING);
		ColumnModel c3 = TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING);
		List<ColumnModel> schema = Arrays.asList(c1, c2, c3);
		changeSet = new SparseChangeSet("syn123",schema);
		// add combinations of rows
		SparseRow r0 = changeSet.addEmptyRow();
		r0.setRowId(0L);
		r0.setCellValue(c1.getId(), "0,0");
		SparseRow r1 = changeSet.addEmptyRow();
		r1.setRowId(1L);
		r1.setCellValue(c2.getId(), "1,1");
		SparseRow r2 = changeSet.addEmptyRow();
		r2.setRowId(2L);
		r2.setCellValue(c3.getId(), "2,2");
		SparseRow r3 = changeSet.addEmptyRow();
		r3.setRowId(3L);
		r3.setCellValue(c1.getId(), "3,1");
		r3.setCellValue(c2.getId(), "3,2");
		r3.setCellValue(c3.getId(), "3,3");
		// add an empty row
		SparseRow r4 = changeSet.addEmptyRow();
		r4.setRowId(4L);
		// add more rows with the same
		SparseRow r5 = changeSet.addEmptyRow();
		r5.setRowId(5L);
		r5.setCellValue(c1.getId(), "5,0");
		SparseRow r6 = changeSet.addEmptyRow();
		r6.setRowId(6L);
		r6.setCellValue(c2.getId(), "6,1");
		SparseRow r7 = changeSet.addEmptyRow();
		r7.setRowId(7L);
		r7.setCellValue(c3.getId(), "7,2");
		SparseRow r8 = changeSet.addEmptyRow();
		r8.setRowId(8L);
		r8.setCellValue(c1.getId(), "8,1");
		r8.setCellValue(c2.getId(), "8,2");
		r8.setCellValue(c3.getId(), "8,3");
		// add another empty row.
		SparseRow r9 = changeSet.addEmptyRow();
		r9.setRowId(9L);
		
		// get the grouping.
		Iterator<Grouping> it = changeSet.groupByValidValues().iterator();
		// group one.
		assertTrue(it.hasNext());
		Grouping g1 = it.next();
		assertEquals(Arrays.asList(c1), g1.getColumnsWithValues());
		assertEquals(Arrays.asList(r0,r5), g1.getRows());
		// group two.
		assertTrue(it.hasNext());
		Grouping g2 = it.next();
		assertEquals(Arrays.asList(c2), g2.getColumnsWithValues());
		assertEquals(Arrays.asList(r1,r6), g2.getRows());
		// group three.
		assertTrue(it.hasNext());
		Grouping g3 = it.next();
		assertEquals(Arrays.asList(c3), g3.getColumnsWithValues());
		assertEquals(Arrays.asList(r2,r7), g3.getRows());
		// group four.
		assertTrue(it.hasNext());
		Grouping g4 = it.next();
		assertEquals(Arrays.asList(c1,c2,c3), g4.getColumnsWithValues());
		assertEquals(Arrays.asList(r3,r8), g4.getRows());
		// group four.
		assertTrue(it.hasNext());
		Grouping g5 = it.next();
		assertEquals(Arrays.asList(), g5.getColumnsWithValues());
		assertEquals(Arrays.asList(r4,r9), g5.getRows());
	}
	
	/**
	 * Write a SparseChangeSet to a DTO and then use
	 * the DTO to create an exact copy of the original SparseChangeSet.
	 * @throws JSONObjectAdapterException 
	 * 
	 */
	@Test
	public void testToDtoAndBack() throws JSONObjectAdapterException{
		changeSet.setEtag("etag1");
		// add some rows
		SparseRow row = changeSet.addEmptyRow();
		row.setRowId(0L);
		row.setVersionNumber(111L);
		row.setRowEtag("etag1");
		row.setCellValue(booleanColumn.getId(), "true");
		row.setCellValue(stringColumn.getId(), "aString");
		row.setCellValue(doubleColumn.getId(), "2.21");
		
		row = changeSet.addEmptyRow();
		row.setRowId(1L);
		row.setVersionNumber(222L);
		row.setCellValue(booleanColumn.getId(), "false");
		row.setCellValue(doubleColumn.getId(), "2.22");
		// Add a delete
		row = changeSet.addEmptyRow();
		row.setRowId(2L);
		row.setVersionNumber(333L);
		
		row = changeSet.addEmptyRow();
		row.setRowId(3L);
		row.setCellValue(doubleColumn.getId(), "29.92");
		// Write to a DTO
		SparseChangeSetDto dto = changeSet.writeToDto();
		assertNotNull(dto);
		//System.out.println(EntityFactory.createJSONStringForEntity(dto));
		// Create a copy from the DTO
		SparseChangeSet copy = new SparseChangeSet(dto, schema);
		// the should be the same.
		assertEquals(changeSet, copy);
	}
	
	@Test
	public void testGetCreatedOrUpdatedRowIds() {
		ColumnModel c1 = TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER);
		ColumnModel c3 = TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING_LIST);
		
		List<ColumnModel> schema = Arrays.asList(c1, c2, c3);
		
		changeSet = new SparseChangeSet("syn123",schema);
		// Add a delete
		SparseRow r0 = changeSet.addEmptyRow();
		r0.setRowId(0L);
		
		// Add a row with the first column only
		SparseRow r1 = changeSet.addEmptyRow();
		r1.setRowId(1L);
		r1.setCellValue(c1.getId(), "value");
		
		// Add a row with the second column only
		SparseRow r2 = changeSet.addEmptyRow();
		r2.setRowId(2L);
		r2.setCellValue(c2.getId(), "1");
		
		// Add all the column
		SparseRow r3 = changeSet.addEmptyRow();
		r3.setRowId(3L);
		r3.setCellValue(c1.getId(), "value");
		r3.setCellValue(c2.getId(), "1");
		r3.setCellValue(c3.getId(), "[\"value\"]");
		
		List<ColumnModel> columnFilter = Arrays.asList(c1, c3);
		
		Set<Long> expected = ImmutableSet.of(1L, 3L);
		
		// Call under test
		Set<Long> result = changeSet.getCreatedOrUpdatedRowIds(columnFilter);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetCreatedOrUpdatedRowIdsWithNoMatch() {
		ColumnModel c1 = TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER);
		ColumnModel c3 = TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING_LIST);
		
		List<ColumnModel> schema = Arrays.asList(c1, c2, c3);
		
		changeSet = new SparseChangeSet("syn123", schema);
		
		// Add a delete
		SparseRow r0 = changeSet.addEmptyRow();
		r0.setRowId(0L);
		
		// Add a row with the first column only
		SparseRow r1 = changeSet.addEmptyRow();
		r1.setRowId(1L);
		r1.setCellValue(c1.getId(), "value");
		
		// Add a row with the second column only
		SparseRow r2 = changeSet.addEmptyRow();
		r2.setRowId(2L);
		r2.setCellValue(c2.getId(), "1");
		
		// Add all the column
		SparseRow r3 = changeSet.addEmptyRow();
		r3.setRowId(3L);
		r3.setCellValue(c1.getId(), "value");
		r3.setCellValue(c2.getId(), "1");
		r3.setCellValue(c3.getId(), "[\"value\"]");
		
		List<ColumnModel> columnFilter = Arrays.asList(new ColumnModel().setId("4"));

		Set<Long> expected = Collections.emptySet();
				
		// Call under test
		Set<Long> result = changeSet.getCreatedOrUpdatedRowIds(columnFilter);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetCreatedOrUpdatedRowIdsWithEmptyChangeSet() {
		ColumnModel c1 = TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER);
		ColumnModel c3 = TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING_LIST);
		
		List<ColumnModel> schema = Arrays.asList(c1, c2, c3);
		
		changeSet = new SparseChangeSet("syn123", schema);
		
		List<ColumnModel> columnFilter = Arrays.asList(c1, c2, c3);

		Set<Long> expected = Collections.emptySet();
				
		// Call under test
		Set<Long> result = changeSet.getCreatedOrUpdatedRowIds(columnFilter);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetCreatedOrUpdatedRowIdsWithNullFilter() {
		
		List<ColumnModel> columnFilter = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			changeSet.getCreatedOrUpdatedRowIds(columnFilter);
		});

		assertEquals("The columnModels is required and must not be empty.", ex.getMessage());
	}
	
	@Test
	public void testGetCreatedOrUpdatedRowIdsWithEmptyFilter() {
		
		List<ColumnModel> columnFilter = Collections.emptyList();
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			changeSet.getCreatedOrUpdatedRowIds(columnFilter);
		});

		assertEquals("The columnModels is required and must not be empty.", ex.getMessage());
	}
	
}
