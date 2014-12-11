package org.sagebionetworks.client;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.fileuploader.FileUploader;
import org.sagebionetworks.client.fileuploader.FileUploaderView;
import org.sagebionetworks.client.fileuploader.StatusCallback;
import org.sagebionetworks.client.fileuploader.UploadFuturesFactory;
import org.sagebionetworks.client.fileuploader.UploadStatus;
import org.sagebionetworks.client.fileuploader.UploaderUtils;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class UploaderUtilsTest {	
	
	UploadFuturesFactory mockUploadFuturesFactory;
	SynapseClient mockSynapseClient;
	Path tmpdir = null;	
	Path tmpfile;
	File tmpFileFile;		
	String mimeType = "text/plain";
	StatusCallback mockStatusCallback;
	String targetEntityId = "syn123";
	
	@Before
	public void before() throws Exception {	
		mockSynapseClient = mock(SynapseClient.class);
		tmpdir = Files.createTempDirectory("FileUploaderTest");
		mockStatusCallback = mock(StatusCallback.class);
		tmpfile = Files.createTempFile(tmpdir, "tmpFile", "txt");
		tmpFileFile = tmpfile.toFile();		
	}	

	@After
	public void after() throws Exception {
		if(tmpdir != null) TestUtils.removeRecursive(tmpdir);		
	}

	
	@Test 
	public void testCreateChildFileEntity() throws Exception {
		S3FileHandle s3fileHandle = new S3FileHandle();
		s3fileHandle.setId("id");
		FileEntity childEntity = new FileEntity();
		childEntity.setName(tmpFileFile.getName());
		childEntity.setDataFileHandleId(s3fileHandle.getId());
		childEntity.setParentId(targetEntityId);
		
		when(mockSynapseClient.createFileHandle(any(File.class), anyString(), eq(targetEntityId))).thenReturn(s3fileHandle);
		when(mockSynapseClient.createEntity(any(Entity.class))).thenReturn(childEntity);
		
		Entity returned = UploaderUtils.createChildFileEntity(tmpFileFile, mimeType, mockSynapseClient, targetEntityId, mockStatusCallback);
		
		verify(mockStatusCallback).setStatus(UploadStatus.UPLOADING);
		verify(mockSynapseClient).createEntity(childEntity);
		assertEquals(childEntity, returned);
	}

	@Test
	public void testCreateNewVersionFileEntity() throws Exception {
		S3FileHandle s3fileHandle = new S3FileHandle();
		s3fileHandle.setId("id");
		FileEntity oldVersion = new FileEntity();
		oldVersion.setId(targetEntityId);
		FileEntity expectedNewVersion = new FileEntity();
		expectedNewVersion.setId(targetEntityId);
		expectedNewVersion.setName(tmpFileFile.getName());
		expectedNewVersion.setDataFileHandleId(s3fileHandle.getId());		
		EntityBundle ebcUpdated = new EntityBundle();
		ebcUpdated.setEntity(expectedNewVersion);
		
		when(mockSynapseClient.createFileHandle(any(File.class), anyString(), eq(oldVersion.getParentId()))).thenReturn(s3fileHandle);
		when(mockSynapseClient.updateEntityBundle(anyString(), any(EntityBundleCreate.class))).thenReturn(ebcUpdated);
		
		Entity returned = UploaderUtils.createNewVersionFileEntity(tmpFileFile, mimeType, mockSynapseClient, oldVersion, mockStatusCallback);

		EntityBundleCreate expectedEbc = new EntityBundleCreate();
		expectedEbc.setEntity(expectedNewVersion);
		
		verify(mockStatusCallback).setStatus(UploadStatus.UPLOADING);
		verify(mockSynapseClient).updateEntityBundle(targetEntityId, expectedEbc);
		assertEquals(expectedNewVersion, returned);

	}	

}
