package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TableConstantsTest {
	@Test
	public void testIsReservedColumnNameNegative(){
		assertFalse(TableConstants.isReservedColumnName("notReserved"));
	}
	
	@Test
	public void testIsReservedColumnNameNull(){
		assertFalse(TableConstants.isReservedColumnName(null));
	}
	
	@Test
	public void testIsReservedColumnNameRowIdCaseInsensitive(){
		assertTrue("The isReservedColumnName() method should be case insensitive.", TableConstants.isReservedColumnName("row_id"));
	}
	
	@Test
	public void testIsReservedColumnNameRowVersion(){
		assertTrue(TableConstants.isReservedColumnName("ROW_VERSION"));
	}
}
