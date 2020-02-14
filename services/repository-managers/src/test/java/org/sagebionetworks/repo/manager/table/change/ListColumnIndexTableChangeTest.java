package org.sagebionetworks.repo.manager.table.change;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

class ListColumnIndexTableChangeTest {

	Long oldColumnId;
	ColumnModel newColumn;

	@BeforeEach
	public void setUp(){
		oldColumnId = 55L;
		newColumn = new ColumnModel();
		newColumn.setId("434");
		newColumn.setColumnType(ColumnType.STRING_LIST);
		newColumn.setMaximumSize(50L);
	}

	@Test
	public void testConstructor_AddOrUpdate_nullNewColumnId(){
		ColumnModel nullColumnModel = null;

		String expectedError = "newColumnChange is required.";

		String errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newAddition(nullColumnModel)
		).getMessage();

		assertEquals(expectedError, errMessage);

		errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newUpdate(oldColumnId, nullColumnModel)
		).getMessage();

		assertEquals(expectedError, errMessage);
	}


	@Test
	public void testConstructor_AddOrUpdate_newColumnNullId(){
		newColumn.setId(null);

		String expectedError = "newColumnChange.id is required.";

		String errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newAddition(newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);

		errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newUpdate(oldColumnId, newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);
	}

	@Test
	public void testConstructor_AddOrUpdate_newColumnNullColumnType(){
		newColumn.setColumnType(null);

		String expectedError = "newColumnChange must be a LIST type";

		String errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newAddition(newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);

		errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newUpdate(oldColumnId, newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);
	}

	@Test
	public void testConstructor_AddOrUpdate_newColumnNonListColumnType(){
		newColumn.setColumnType(ColumnType.STRING);

		String expectedError = "newColumnChange must be a LIST type";

		String errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newAddition(newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);

		errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newUpdate(oldColumnId, newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);
	}

	@Test
	public void testConstructor_AddOrUpdate_newColumnStringListColumnTypeNoSize(){
		newColumn.setMaximumSize(null);

		String expectedError = "newColumnChange.maximumSize is required.";

		String errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newAddition(newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);

		errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newUpdate(oldColumnId, newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);
	}


	@Test
	public void testConstructor_RemoveOrUpdate_nullOldColumnId(){
		Long nullOldColumnId = null;

		String expectedError = "oldColumnId is required.";

		String errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newRemoval(nullOldColumnId)
		).getMessage();

		assertEquals(expectedError, errMessage);

		errMessage = assertThrows(IllegalArgumentException.class, ()->
				ListColumnIndexTableChange.newUpdate(nullOldColumnId, newColumn)
		).getMessage();

		assertEquals(expectedError, errMessage);
	}

}