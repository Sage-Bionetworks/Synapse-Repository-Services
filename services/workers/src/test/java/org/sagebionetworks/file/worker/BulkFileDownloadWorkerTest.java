package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationAuthorizationStatus;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.springframework.test.util.ReflectionTestUtils;

import static org.sagebionetworks.repo.manager.AuthorizationManagerUtil.ACCESS_DENIED;
import static org.sagebionetworks.repo.manager.AuthorizationManagerUtil.AUTHORIZED;

import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.Sets;

public class BulkFileDownloadWorkerTest {
	
	UserManager mockUserManger;
	FileHandleAuthorizationManager mockFileHandleAuthorizationManager;
	FileHandleDao mockFileHandleDao;
	AmazonS3Client mockS3client;
	FileResourceProvider mockFileResourceProvider;
	
	BulkFileDownloadWorker worker;
	
	ZipOutputStream mockZipOut;
	FileHandleAssociation fha1;
	
	@Before
	public void before(){
		//dependencies
		mockUserManger = Mockito.mock(UserManager.class);
		mockFileHandleAuthorizationManager = Mockito.mock(FileHandleAuthorizationManager.class);
		mockFileHandleDao = Mockito.mock(FileHandleDao.class);
		mockS3client = Mockito.mock(AmazonS3Client.class);
		mockFileResourceProvider = Mockito.mock(FileResourceProvider.class);
		
		worker = new BulkFileDownloadWorker();
		ReflectionTestUtils.setField(worker, "userManger", mockUserManger);
		ReflectionTestUtils.setField(worker, "fileHandleAuthorizationManager", mockFileHandleAuthorizationManager);
		ReflectionTestUtils.setField(worker, "fileHandleDao", mockFileHandleDao);
		ReflectionTestUtils.setField(worker, "s3client", mockS3client);
		ReflectionTestUtils.setField(worker, "fileResourceProvider", mockFileResourceProvider);
		
		fha1 = new FileHandleAssociation();
		fha1.setFileHandleId("1");
		fha1.setAssociateObjectId("123");
		fha1.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		
		//mock test objects
		mockZipOut = Mockito.mock(ZipOutputStream.class);
	}
	
	@Test
	public void testCreateZipEntryName(){
		assertEquals("321/321321/foo.txt", BulkFileDownloadWorker.createZipEntryName("foo.txt", 321321));
	}
	
	@Test
	public void testWriteOneFileToZipHappy() throws IOException{
		FileHandleAssociationAuthorizationStatus status = new FileHandleAssociationAuthorizationStatus(fha1, AUTHORIZED);
		long zipFileSize = 0L;
		Set<String> fileHandleIdsInZip = Sets.newHashSet();
		worker.writeOneFileToZip(mockZipOut, zipFileSize, status, fileHandleIdsInZip);
	}

}
