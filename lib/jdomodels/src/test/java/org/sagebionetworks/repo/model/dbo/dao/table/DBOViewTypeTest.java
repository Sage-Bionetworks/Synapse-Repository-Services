package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;

public class DBOViewTypeTest {

	/**
	 * Test for PLFM_4956
	 */
	@Test
	public void testCreateDatabaseObjectFromBackupOldType() {
		DBOViewType type = new DBOViewType();
		// old value
		type.setViewType(ViewType.file_and_table.toString());
		// call under test
		DBOViewType result = type.getTranslator().createDatabaseObjectFromBackup(type);
		assertNotNull(result);
		assertEquals(null, result.getViewType());
		Long expectedMask = ViewTypeMask.getMaskForDepricatedType(ViewType.file_and_table);
		assertEquals(expectedMask, result.getViewTypeMask());
	}
	
	/**
	 * Test for PLFM_4956
	 */
	@Test
	public void testCreateDatabaseObjectFromBackupNewMask() {
		DBOViewType type = new DBOViewType();
		// old value
		type.setViewType(null);
		Long viewTypeMaks = ViewTypeMask.File.getMask() | ViewTypeMask.Folder.getMask();
		type.setViewTypeMask(viewTypeMaks);
		// call under test
		DBOViewType result = type.getTranslator().createDatabaseObjectFromBackup(type);
		assertNotNull(result);
		assertEquals(null, result.getViewType());
		assertEquals(viewTypeMaks, result.getViewTypeMask());
	}
}
