package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.file.preview.StubFileMetadataDao;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle.MetadataType;

/**
 * Unit test for FileHandleMigratableManager
 * @author John
 *
 */
public class FileHandleMigratableManagerTest {
	
	FileHandleDao sourceStubDao;
	FileHandleDao destStubDao;
	FileHandleMigratableManager sourceManager;
	FileHandleMigratableManager destManager;
	
	@Before
	public void before(){
		sourceStubDao = new StubFileMetadataDao();
		sourceManager = new FileHandleMigratableManager(sourceStubDao);
		destStubDao = new StubFileMetadataDao();
		destManager = new FileHandleMigratableManager(destStubDao);
	}
	
	@Test
	public void testRoundTrip() throws Exception{
		// Add a backup the source
		FileHandleBackup backup = createBackup();
		sourceStubDao.createOrUpdateFromBackup(backup);
		
		// backup source
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Write the data to the stream.
		sourceManager.writeBackupToOutputStream(backup.getId().toString(), out);
		String data = new String(out.toByteArray(), "UTF-8");
		System.out.println(data);
		// Read the file into the destination.
		ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes( "UTF-8"));
		destManager.createOrUpdateFromBackupStream(in);
		
		// Make sure the data moved to the destination
		FileHandleBackup clone = destStubDao.getFileHandleBackup(backup.getId().toString());
		assertNotNull(clone);
		assertEquals(backup, clone);
	}
	
	public FileHandleBackup createBackup(){
		FileHandleBackup b = new FileHandleBackup();
		b.setBucketName("bucket");
		b.setContentMD5("md5");
		b.setContentSize(1l);
		b.setContentType("contentType");
		b.setCreatedBy(123l);
		b.setCreatedOn(456l);
		b.setEtag("etag");
		b.setId(999l);
		b.setMetadataType(MetadataType.S3.name());
		b.setName("name");
		b.setPreviewId(555l);
		return b;
	}
}
