package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.TableChangeType;

public class DBOTableRowChangeTest {

	@Test
	public void testTranslatePLFM_4016Null(){
		DBOTableRowChange dbo = new DBOTableRowChange();
		dbo.setChangeType(null);
		DBOTableRowChange back = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(TableChangeType.ROW.name(), back.getChangeType());
	}
	
	@Test
	public void testTranslatePLFM_4016NotNull(){
		DBOTableRowChange dbo = new DBOTableRowChange();
		dbo.setChangeType(TableChangeType.COLUMN.name());
		DBOTableRowChange back = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(TableChangeType.COLUMN.name(), back.getChangeType());
	}

}
