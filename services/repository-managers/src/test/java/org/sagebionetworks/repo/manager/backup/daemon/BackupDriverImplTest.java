package org.sagebionetworks.repo.manager.backup.daemon;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class BackupDriverImplTest {

	@Test
	public void testFileNameRoundTrip(){
		String name = BackupDriverImpl.getFileNameForType(MigrationType.FILE_HANDLE);
		assertEquals("FILE_HANDLE.xml", name);
		MigrationType type = BackupDriverImpl.getTypeFromFileName(name);
		assertEquals(MigrationType.FILE_HANDLE, type);
	}
	
	@Test
	public void testInvalidTypeFromFileName() {
		String name = "SOME_INVALID_TYPE.xml";
		MigrationType type = BackupDriverImpl.getTypeFromFileName(name);
		assertNull(type);
	}
}
