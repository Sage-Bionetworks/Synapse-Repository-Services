package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.PreviewState;
import org.sagebionetworks.repo.web.NotFoundException;

public class AttachmentManagerUnitTest {
	
	AmazonS3Utility mockUtil;
	IdGenerator mockIdGenerator;
	AttachmentManagerImpl manager;
	ExecutorService mockThreadPool;
	String userId = "007";
	
	@Before
	public void before(){
		mockUtil = Mockito.mock(AmazonS3Utility.class);
		mockIdGenerator = Mockito.mock(IdGenerator.class);
		mockThreadPool = Mockito.mock(ExecutorService.class);
		manager = new AttachmentManagerImpl(mockUtil, mockIdGenerator, mockThreadPool);
	}

	@Test
	public void testIsPreviewTypeTrue(){
		String[] validName = new String[]{
				"one.jpg",
				"two.jpeg",
				"three.JPEG",
				"four.GIF",
				"five.bmp",
				"six.png",
				"seven.png",
				"eight.wbmp",
		};
		// All of these type should be true
		for(String name: validName){
			assertTrue("This is a valid image name: "+name,AttachmentManagerImpl.isPreviewType(name));
		}
	}
	
	@Test
	public void testIsPreviewTypeFalse(){
		String[] notImageName = new String[]{
				"one.txt",
				"two.doc",
				"three.pdf",
				"four.zip",
				"five",
				"six.tar",
		};
		// All of these type should be true
		for(String name: notImageName){
			assertFalse("This is not a valid image name: "+name, AttachmentManagerImpl.isPreviewType(name));
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAttachmentDataNullName(){
		AttachmentData data = new AttachmentData();
		data.setName(null);
		AttachmentManagerImpl.validateAttachmentData(data);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAttachmentDataNullIDAndName(){
		AttachmentData data = new AttachmentData();
		data.setName("test.jpg");
		data.setTokenId(null);
		data.setUrl(null);
		AttachmentManagerImpl.validateAttachmentData(data);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAttachmentDataBadURL(){
		AttachmentData data = new AttachmentData();
		data.setName("test.jpg");
		data.setTokenId(null);
		data.setUrl("this is not a url");
		AttachmentManagerImpl.validateAttachmentData(data);
	}
		
	@Test
	public void testValidateAttachmentValidToken(){
		AttachmentData data = new AttachmentData();
		data.setName("test.jpg");
		data.setTokenId("123");
		AttachmentManagerImpl.validateAttachmentData(data);
	}
	
	@Test
	public void testValidateAttachmentValidURL(){
		AttachmentData data = new AttachmentData();
		data.setName("test.jpg");
		data.setUrl("http://google.com/test.jpg");
		AttachmentManagerImpl.validateAttachmentData(data);
	}
	
	@Test
	public void testGetDownloadUrlWithToken() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		AttachmentData data = new AttachmentData();
		data.setName("some.jpg");
		data.setTokenId("123");
		String entityId = "sys123";
		PresignedUrl preUrl = new PresignedUrl();
		preUrl.setPresignedUrl("http://locolhoast:8080/some.jpg");
		File expected = new File("some.tmp");
		when(mockUtil.downloadFromS3(any(String.class))).thenReturn(expected);
		File result = manager.downloadImage("syn123", data);
		assertNotNull(result);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetDownloadUrlWithUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		AttachmentData data = new AttachmentData();
		data.setName("some.jpg");
		String fileName = "images/shortWide.gif";
		URL url = AttachmentManagerUnitTest.class.getClassLoader().getResource(fileName);
		assertNotNull("Could not find: "+fileName+" on the classpath", url);
		data.setUrl(url.toString());
		when(mockUtil.downloadFromS3(any(String.class))).thenThrow(new IllegalArgumentException("this should not have been called"));
		File result = manager.downloadImage("syn123", data);
		assertNotNull(result);
		// This should be a temp copy of the file
		assertTrue(result.getName().startsWith("AttachmentManager"));
		assertTrue(result.length() > 100);
		// Delete the temp file
		result.delete();
	}
	
	@Test
	public void testValidateAndCheckForPreview() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		AttachmentData data = new AttachmentData();
		data.setName("shortWide.gif");
		String fileName = "images/shortWide.gif";
		URL url = AttachmentManagerUnitTest.class.getClassLoader().getResource(fileName);
		assertNotNull("Could not find: "+fileName+" on the classpath", url);
		data.setUrl(url.toString());
		String entityId = "123";
		// When it fails it should set the state to failed
		Long previewId = new Long(456);
		when(mockIdGenerator.generateNewId()).thenReturn(previewId);
		when(mockUtil.uploadToS3(any(File.class), any(String.class))).thenReturn(Boolean.TRUE);
		manager.validateAndCheckForPreview(entityId, data);
		assertEquals(PreviewState.PREVIEW_EXISTS, data.getPreviewState());
		assertEquals(previewId.toString()+"/shortWide.gif", data.getPreviewId());

	}
}
