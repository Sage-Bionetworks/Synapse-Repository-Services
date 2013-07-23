package org.sagebionetworks.client;

import static org.mockito.Matchers.*;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.fileuploader.FileUploader;
import org.sagebionetworks.client.fileuploader.FileUploaderView;
import org.sagebionetworks.client.fileuploader.UploadStatus;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class FileUploaderTest {

	FileUploader fileUploader;
	FileUploaderView mockView;
	Synapse mockSynapseClient;
	Project parent;
	final String parentId = "syn1234";
	String displayname = "name";
	UserSessionData usd;
	Path tmpdir = null;
	
	@Before
	public void before() throws Exception {
		mockView = mock(FileUploaderView.class);
		fileUploader = new FileUploader(mockView);		
		verify(mockView).setPresenter(fileUploader);
		
		mockSynapseClient = mock(Synapse.class);

		usd = new UserSessionData();		
		UserProfile profile = new UserProfile();
		profile.setDisplayName(displayname);
		usd.setProfile(profile);
		
		parent = new Project();
		parent.setId(parentId);
		parent.setName(parentId);

		// for configure
		when(mockSynapseClient.getUserSessionData()).thenReturn(usd);
		when(mockSynapseClient.getEntityById(parentId)).thenReturn(parent);

	}	
	
	@After
	public void after() throws Exception {
		if(tmpdir != null) removeRecursive(tmpdir);		
	}
	
	@Test
	public void testConfigureSessionSuccess() throws Exception {
		fileUploader.configure(mockSynapseClient, parentId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView, never()).alert(anyString());
	}

	@Test
	public void testConfigureSessionUnathorized() throws Exception {
		when(mockSynapseClient.getUserSessionData()).thenThrow(new SynapseUnauthorizedException());
		fileUploader.configure(mockSynapseClient, parentId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView).alert(matches(".*session.*expired.*"));
	}

	@Test
	public void testConfigureSessionOther() throws Exception {
		when(mockSynapseClient.getUserSessionData()).thenThrow(new SynapseException());
		fileUploader.configure(mockSynapseClient, parentId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView).alert(matches(".*Error.*"));
	}

	@Test
	public void testConfigureSessionGetParentSuccess() throws Exception {
		fileUploader.configure(mockSynapseClient, parentId);
		verify(mockSynapseClient).getEntityById(parentId);
		verify(mockView).setUploadingIntoMessage(anyString());
	}

	@Test
	public void testConfigureSessionGetParentNotFound() throws Exception {
		when(mockSynapseClient.getEntityById(anyString())).thenThrow(new SynapseNotFoundException());
		fileUploader.configure(mockSynapseClient, parentId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView).alert(matches(".*Not Found.*"));
	}

	@Test
	public void testConfigureSessionGetParentForbidden() throws Exception {
		when(mockSynapseClient.getEntityById(anyString())).thenThrow(new SynapseForbiddenException());
		fileUploader.configure(mockSynapseClient, parentId);
		verify(mockSynapseClient).getUserSessionData();
		verify(mockView).alert(matches(".*Access Denied.*"));
	}

	@Test
	public void testUploadFiles() throws Exception {
		tmpdir = Files.createTempDirectory("FileUploaderTest");
		Path tmpfile = Files.createTempFile(tmpdir, "tmpFile", "txt");
		File tmpFileFile = tmpfile.toFile();				
		List<File> files = new ArrayList<File>();
		files.add(tmpFileFile);
		
		S3FileHandle s3fileHandle = new S3FileHandle();
		s3fileHandle.setId("id");
		
		FileEntity entity = new FileEntity();
		entity.setId("syn456");
		entity.setName("name");
		entity.setDataFileHandleId(s3fileHandle.getId());			
		
		when(mockSynapseClient.createFileHandle(any(File.class), anyString())).thenReturn(s3fileHandle);
		when(mockSynapseClient.createEntity(any(Entity.class))).thenReturn(entity);
		
		fileUploader.uploadFiles(files);
		
		UploadStatus status = UploadStatus.NOT_UPLOADED;
		while(status != UploadStatus.UPLOADED && status != UploadStatus.FAILED) {
			Thread.sleep(500); // let futures complete
			status = fileUploader.getFileUplaodStatus(tmpFileFile);
		}
		
//		verify(mockSynapseClient).createFileHandle(any(File.class), eq("text/plain"));			
	}
	

	
	
	
	
	/*
	 * Private Helper methods
	 */
	private static void removeRecursive(Path path) throws IOException {
	    Files.walkFileTree(path, new SimpleFileVisitor<Path>()
	    {
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
	                throws IOException
	        {
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
	        {
	            // try to delete the file anyway, even if its attributes
	            // could not be read, since delete-only access is
	            // theoretically possible
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
	        {
	            if (exc == null)
	            {
	                Files.delete(dir);
	                return FileVisitResult.CONTINUE;
	            }
	            else
	            {
	                // directory iteration failed; propagate exception
	                throw exc;
	            }
	        }
	    });
	}
	
}
