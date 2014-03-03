package org.sagebionetworks.repo.manager.backup.daemon;

import static org.junit.Assert.assertEquals;

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
}
