package org.sagebionetworks.table.query.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;

class ColumnTypeListMappingsTest {

	@Test
	public void testForListType(){
		assertEquals(ColumnTypeListMappings.STRING, ColumnTypeListMappings.forListType(ColumnType.STRING_LIST));
		assertEquals(ColumnTypeListMappings.INTEGER, ColumnTypeListMappings.forListType(ColumnType.INTEGER_LIST));
		assertEquals(ColumnTypeListMappings.DATE, ColumnTypeListMappings.forListType(ColumnType.DATE_LIST));
		assertEquals(ColumnTypeListMappings.BOOLEAN, ColumnTypeListMappings.forListType(ColumnType.BOOLEAN_LIST));
		assertEquals(ColumnTypeListMappings.ENTITYID, ColumnTypeListMappings.forListType(ColumnType.ENTITYID_LIST));
		assertEquals(ColumnTypeListMappings.USERID, ColumnTypeListMappings.forListType(ColumnType.USERID_LIST));
	}

	@Test
	public void testForListType_notFound() {
		assertThrows(IllegalArgumentException.class, () -> ColumnTypeListMappings.forListType(ColumnType.STRING));
	}


	@Test
	public void testForNonListType(){
		assertEquals(ColumnTypeListMappings.STRING, ColumnTypeListMappings.forNonListType(ColumnType.STRING));
		assertEquals(ColumnTypeListMappings.INTEGER, ColumnTypeListMappings.forNonListType(ColumnType.INTEGER));
		assertEquals(ColumnTypeListMappings.DATE, ColumnTypeListMappings.forNonListType(ColumnType.DATE));
		assertEquals(ColumnTypeListMappings.BOOLEAN, ColumnTypeListMappings.forNonListType(ColumnType.BOOLEAN));
		assertEquals(ColumnTypeListMappings.ENTITYID, ColumnTypeListMappings.forNonListType(ColumnType.ENTITYID));
		assertEquals(ColumnTypeListMappings.USERID, ColumnTypeListMappings.forNonListType(ColumnType.USERID));
	}

	@Test
	public void testForNonListType_notFound() {
		assertThrows(IllegalArgumentException.class, () -> ColumnTypeListMappings.forNonListType(ColumnType.STRING_LIST));
	}
}