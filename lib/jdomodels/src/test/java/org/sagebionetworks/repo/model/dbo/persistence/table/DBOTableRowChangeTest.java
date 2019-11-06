package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.TableChangeType;

public class DBOTableRowChangeTest {

	/**
	 * Column changes were saved to the old key field and need to be moved to keyNew.
	 * 
	 */
	@Test
	public void testTranslateColumnToNew(){
		DBOTableRowChange dbo = new DBOTableRowChange();
		dbo.setChangeType(TableChangeType.COLUMN.name());
		dbo.setKey("move-me");
		dbo.setKeyNew(null);
		// call under test
		DBOTableRowChange back = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals("move-me", back.getKeyNew());
	}
	
	@Test
	public void testTranslateColumnHasKeyNew(){
		DBOTableRowChange dbo = new DBOTableRowChange();
		dbo.setChangeType(TableChangeType.COLUMN.name());
		dbo.setKey(null);
		dbo.setKeyNew("leave-me-alone");
		// call under test
		DBOTableRowChange back = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals("leave-me-alone", back.getKeyNew());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateColumnNullKeys(){
		DBOTableRowChange dbo = new DBOTableRowChange();
		dbo.setChangeType(TableChangeType.COLUMN.name());
		dbo.setKey(null);
		dbo.setKeyNew(null);
		// call under test
		dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
	}
	
	@Test
	public void testTranslateRow(){
		DBOTableRowChange dbo = new DBOTableRowChange();
		dbo.setChangeType(TableChangeType.ROW.name());
		dbo.setKey("ignore-me");
		dbo.setKeyNew(null);
		// call under test
		DBOTableRowChange back = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(null, back.getKeyNew());
	}
}
