package org.sagebionetworks.table.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

class ListColumnRowChangesTest {

	ColumnModel columnModel;
	Set<Long> rowIds;

	@BeforeEach
	public void setup(){
		columnModel = new ColumnModel();
		columnModel.setId("123");
		columnModel.setName("myColumn");
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setMaximumSize(50L);

		rowIds = Sets.newHashSet(1L,3L,5L);
	}

	@Test
	public void testConsturctor_nullColumnModel(){
		String errMessage = assertThrows(IllegalArgumentException.class, () ->{
			new ListColumnRowChanges(null, rowIds);
		}).getMessage();

		assertEquals("columnModel is required.", errMessage);
	}

	@Test
	public void testConsturctor_ColumnIsNotList(){
		columnModel.setColumnType(ColumnType.STRING);
		String errMessage = assertThrows(IllegalArgumentException.class, () ->{
			new ListColumnRowChanges(columnModel, rowIds);
		}).getMessage();

		assertEquals("columnModel must have a LIST columnType", errMessage);
	}

	@Test
	public void testConsturctor_nullRowIds(){
		String errMessage = assertThrows(IllegalArgumentException.class, () ->{
			new ListColumnRowChanges(columnModel, null);
		}).getMessage();

		assertEquals("rowIds is required and must not be empty.", errMessage);
	}

	@Test
	public void testConsturctor_emptyRowIds(){
		String errMessage = assertThrows(IllegalArgumentException.class, () ->{
			new ListColumnRowChanges(columnModel, Collections.emptySet());
		}).getMessage();

		assertEquals("rowIds is required and must not be empty.", errMessage);
	}


	@Test
	public void testConsturctor_happy(){
		ListColumnRowChanges listColumnRowChanges = new ListColumnRowChanges(columnModel, rowIds);
		assertEquals(columnModel, listColumnRowChanges.getColumnModel());
		assertEquals(rowIds, listColumnRowChanges.getRowIds());
	}
}