package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.fileuploader.FileUploader;
import org.sagebionetworks.client.fileuploader.FileUploaderView;
import org.sagebionetworks.client.fileuploader.StatusCallback;
import org.sagebionetworks.client.fileuploader.UploadFuturesFactory;
import org.sagebionetworks.client.fileuploader.UploadStatus;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;

public class FileUploaderTest {

	FileUploader fileUploader;
	FileUploaderView mockView;
	SynapseClient mockSynapseClient;
	UploadFuturesFactory mockUploadFuturesFactory;
	Project parentTarget;
	FileEntity updateTarget;
	final String targetId = "syn1234";
	String displayname = "name";
	UserSessionData usd;
	Path tmpdir = null;
	File tmpFile1;
	Path tmpSubdir;
	File tmpFile2;
	File tmpFile3;

	
	@Before
	public void before() throws Exception {
		mockView = mock(FileUploaderView.class);
		mockUploadFuturesFactory = mock(UploadFuturesFactory.class);
		fileUploader = new FileUploader(mockView, mockUploadFuturesFactory);		
		verify(mockView).setPresenter(fileUploader);
		
		mockSynapseClient = mock(SynapseClient.class);

		usd = new UserSessionData();		
		UserProfile profile = new UserProfile();
		usd.setProfile(profile);
		
		parentTarget = new Project();
		parentTarget.setId(targetId);
		parentTarget.setName(targetId);
		
		updateTarget = new FileEntity();
		updateTarget.setId(targetId);
		updateTarget.setName("updateTarget");

		tmpdir = Files.createTempDirectory("FileUploaderTest");
		tmpFile1 = Files.createTempFile(tmpdir, "tmpFile1", "txt").toFile();
		tmpSubdir = Files.createTempDirectory(tmpdir, "tmpDir");
		tmpFile2 = Files.createTempFile(tmpSubdir, "tmpSubFile2", "txt").toFile();
		tmpFile3 = Files.createTempFile(tmpSubdir, "tmpSubFile3", "txt").toFile();
		
		// for configure
		when(mockSynapseClient.getUserSessionData()).thenReturn(usd);
		when(mockSynapseClient.getEntityById(targetId)).thenReturn(parentTarget);
		
	}	
	
	@After
	public void after() throws Exception {
		if(tmpdir != null) TestUtils.removeRecursive(tmpdir);		
	}
	
	@Test
	public void testConfigureSessionSuccess() throws Exception {
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView, never()).alert(anyString());
		verify(mockView).setSingleFileMode(false);
	}
	
	@Test
	public void testConfigureSessionSuccessSFM() throws Exception {
		when(mockSynapseClient.getEntityById(targetId)).thenReturn(updateTarget);

		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView, never()).alert(anyString());
		verify(mockView).setSingleFileMode(true);
	}

	@Test
	public void testConfigureSessionUnathorized() throws Exception {
		when(mockSynapseClient.getUserSessionData()).thenThrow(new SynapseUnauthorizedException());
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView).alert(matches(".*session.*expired.*"));
	}

	@Test
	public void testConfigureSessionOther() throws Exception {
		when(mockSynapseClient.getUserSessionData()).thenThrow(new SynapseClientException());
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView).alert(matches(".*Error.*"));
	}

	@Test
	public void testConfigureSessionGetParentSuccess() throws Exception {
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockSynapseClient).getEntityById(targetId);
		verify(mockView).setUploadingIntoMessage(anyString());
	}

	@Test
	public void testConfigureSessionGetParentNotFound() throws Exception {
		when(mockSynapseClient.getEntityById(anyString())).thenThrow(new SynapseNotFoundException());
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView).alert(matches(".*Not Found.*"));
	}

	@Test
	public void testConfigureSessionGetParentForbidden() throws Exception {
		when(mockSynapseClient.getEntityById(anyString())).thenThrow(new SynapseForbiddenException());
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView).alert(matches(".*Access Denied.*"));
	}

	@Test
	public void testAddAndRemoveFilesForUpload() throws Exception {
		List<File> files = new ArrayList<File>(Arrays.asList(new File[] {tmpFile1, tmpSubdir.toFile()}));
	
		// test Add
		fileUploader.addFilesForUpload(files);
				
		Set<File> actualFiles = fileUploader.getStagedFilesForUpload();
		assertTrue(actualFiles.contains(tmpFile1));
		assertTrue(actualFiles.contains(tmpFile2));
		
		// test Remove
		fileUploader.removeFilesFromUpload(Arrays.asList(new File[] { tmpFile2, tmpFile1 }));
		assertEquals(1, actualFiles.size());
	}	
		
	@Test
	@SuppressWarnings("unchecked")
	public void testUploadFilesParentMode() throws Exception {
		// muilti file mode
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockView).setSingleFileMode(false);
		
		// add files
		List<File> files = new ArrayList<File>(Arrays.asList(new File[] {tmpFile1, tmpSubdir.toFile()}));	
		fileUploader.addFilesForUpload(files);
		
		// future mock whens
		Future<Entity> mockFuture1 = mock(Future.class);
		when(mockFuture1.isDone()).thenReturn(true);
		when(mockFuture1.get()).thenReturn(null);
		Future<Entity> mockFuture2 = mock(Future.class);
		when(mockFuture2.isDone()).thenReturn(true);
		when(mockFuture2.get()).thenThrow(new ExecutionException("(409)", new SynapseServerException(409, "(409)")));
		Future<Entity> mockFuture3 = mock(Future.class);
		when(mockFuture3.isDone()).thenReturn(true);
		when(mockFuture3.get()).thenThrow(new ExecutionException("some reason", new SynapseClientException("Some other exception")));		
		when(mockUploadFuturesFactory.createChildFileEntityFuture(eq(tmpFile1), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  anyString(), any(StatusCallback.class)))
				  .thenReturn(mockFuture1);
		when(mockUploadFuturesFactory.createChildFileEntityFuture(eq(tmpFile2), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  anyString(), any(StatusCallback.class)))
				  .thenReturn(mockFuture2);		
		when(mockUploadFuturesFactory.createChildFileEntityFuture(eq(tmpFile3), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  anyString(), any(StatusCallback.class)))
				  .thenReturn(mockFuture3);		
		
		// upload and verify
		fileUploader.uploadFiles();
		Thread.sleep(100);
				
		verify(mockUploadFuturesFactory).createChildFileEntityFuture(eq(tmpFile1), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  anyString(), any(StatusCallback.class));
		verify(mockUploadFuturesFactory).createChildFileEntityFuture(eq(tmpFile2), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  anyString(), any(StatusCallback.class));
		verify(mockUploadFuturesFactory).createChildFileEntityFuture(eq(tmpFile3), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  anyString(), any(StatusCallback.class));
		
		// check status
		Map<File, UploadStatus> fileStatus = fileUploader.getFileStatus();
		assertEquals(UploadStatus.UPLOADED, fileStatus.get(tmpFile1));
		assertEquals(UploadStatus.ALREADY_EXISTS, fileStatus.get(tmpFile2));		
		assertEquals(UploadStatus.FAILED, fileStatus.get(tmpFile3));
	}

	@Test
	public void testUploadFilesSingleFileMode() throws Exception {
		// single file mode
		when(mockSynapseClient.getEntityById(targetId)).thenReturn(updateTarget);
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockView).setSingleFileMode(true);
				
		// add files	
		fileUploader.addFilesForUpload(Arrays.asList(new File[] {tmpFile1}));
		
		// future mock whens
		FileEntity updatedEntity = new FileEntity();
		updatedEntity.setId(updateTarget.getId());
		updatedEntity.setName("updatedEntity");
		updatedEntity.setDataFileHandleId("dataFileHandleId");		
		Future<Entity> mockFuture1 = mock(Future.class);
		when(mockFuture1.isDone()).thenReturn(true);
		when(mockFuture1.get()).thenReturn(updatedEntity);		
		when(mockUploadFuturesFactory.createNewVersionFileEntityFuture(eq(tmpFile1), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  any(FileEntity.class), any(StatusCallback.class)))
				  .thenReturn(mockFuture1);

		// upload and verify
		fileUploader.uploadFiles();
		Thread.sleep(100);
		verify(mockUploadFuturesFactory).createNewVersionFileEntityFuture(eq(tmpFile1), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  eq(updateTarget), any(StatusCallback.class));
		// check status
		Map<File, UploadStatus> fileStatus = fileUploader.getFileStatus();
		assertEquals(UploadStatus.UPLOADED, fileStatus.get(tmpFile1));
		
		
		
		// now verify that updatedEntity is what is passed in round 2 up updating
		fileUploader.removeFilesFromUpload(Arrays.asList(new File[]{ tmpFile1 }));
		fileUploader.addFilesForUpload(Arrays.asList(new File[] { tmpFile2 }));
		Future<Entity> mockFuture2 = mock(Future.class);
		when(mockFuture2.isDone()).thenReturn(true);
		when(mockFuture2.get()).thenReturn(new FileEntity());
		when(mockUploadFuturesFactory.createChildFileEntityFuture(eq(tmpFile2), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  anyString(), any(StatusCallback.class)))
				  .thenReturn(mockFuture2);
		fileUploader.uploadFiles();
		verify(mockUploadFuturesFactory).createNewVersionFileEntityFuture(eq(tmpFile2), anyString(),
				  any(ExecutorService.class), any(SynapseClient.class),
				  eq(updatedEntity), any(StatusCallback.class));
		
	}

	@Test
	public void testUploadFilesSingleFileModeTooManyFiles() throws Exception {
		// single file mode
		when(mockSynapseClient.getEntityById(targetId)).thenReturn(updateTarget);
		fileUploader.configure(mockSynapseClient, targetId);
		verify(mockView).setSingleFileMode(true);
		
		// add files
		List<File> files = new ArrayList<File>(Arrays.asList(new File[] {tmpFile1, tmpSubdir.toFile()}));	
		fileUploader.addFilesForUpload(files);
		
		fileUploader.uploadFiles();		
		verify(mockView).alert(matches(".*reduce the number of files.*"));
	}

}
