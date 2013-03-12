package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Unit test for the GenericBackupDriverImpl.
 * 
 * @author John
 *
 */
public class GenericBackupDriverImplTest {
	
	MigratableManagerStub sourceManagerStub;
	GenericBackupDriverImpl sourceDriver;
	MigratableManagerStub destManagerStub;
	GenericBackupDriverImpl destDriver;
	
	@Before
	public void before(){
		sourceManagerStub = new MigratableManagerStub();
		sourceDriver = new GenericBackupDriverImpl(sourceManagerStub);
		destManagerStub = new MigratableManagerStub();
		destDriver = new GenericBackupDriverImpl(destManagerStub);
	}
	
	
	@Test
	public void testMigration() throws IOException, NotFoundException, InterruptedException{
		File tempFile = File.createTempFile("GenericBackupDriverImplTest", ".tmp");
		try{
			// this test will attempt to move all of the following ID from the source to destination
			Set<String> idsToBackup = new HashSet<String>();
			idsToBackup.add("12345");
			idsToBackup.add("43456");
			idsToBackup.add("6645345");
			idsToBackup.add("575667567");
			// Add all of the data to the stub
			sourceManagerStub.setIdSet(idsToBackup);
			// Start
			Progress backupProgresss = new Progress();
			// Write all of the data from the source into the passed file
			sourceDriver.writeBackup(tempFile, backupProgresss, idsToBackup);
			assertEquals(idsToBackup.size(), backupProgresss.getTotalCount());
			assertEquals(idsToBackup.size(), backupProgresss.getCurrentIndex());
			// Use the file to restore the data
			Progress restoreProgress = new Progress();
			destDriver.restoreFromBackup(tempFile, restoreProgress);
			assertEquals(tempFile.length(), restoreProgress.getTotalCount());
			// Validate that all of the data is not in the destination stub
			assertEquals(idsToBackup, destManagerStub.getIdSet());
		}finally{
			tempFile.delete();
		}
	}

}
