package org.sagebionetworks.repo.model.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


public class TableConstantsTest {
	
	private enum TestEnum {
		A, B, C;
		
		// Makes sure we are not using the toString
		@Override
		public String toString() {
			return this.name().toLowerCase();
		}
	}
	
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
		assertTrue(TableConstants.isReservedColumnName("row_id"), "The isReservedColumnName() method should be case insensitive.");
	}

	@Test
	public void testIsReservedColumnNameRowVersion(){
		assertTrue(TableConstants.isReservedColumnName("ROW_VERSION"));
	}


	@Test
	public void testIsReservedColumnNameRowBenefactor(){
		assertTrue(TableConstants.isReservedColumnName("row_benefactor"));
	}
	
	@Test
	public void testJoinEnumForSQL() {
		assertEquals("'A','B','C'", TableConstants.joinEnumForSQL(TestEnum.values()));
	}
}
