package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
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
		assertNull(result.getViewType());
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
		assertNull(result.getViewType());
		assertEquals(viewTypeMaks, result.getViewTypeMask());
	}
	
	@Test
	public void testCreateDatabaseObjectFromBackupNoObjectType() {
		DBOViewType type = new DBOViewType();
		// old value
		type.setViewObjectType(null);
		Long viewTypeMaks = ViewTypeMask.File.getMask() | ViewTypeMask.Folder.getMask();
		type.setViewTypeMask(viewTypeMaks);
		// call under test
		DBOViewType result = type.getTranslator().createDatabaseObjectFromBackup(type);
		assertNotNull(result);
		assertNull(result.getViewType());
		assertEquals(viewTypeMaks, result.getViewTypeMask());
		assertEquals(ObjectType.ENTITY.name(), result.getViewObjectType());
	}
}
