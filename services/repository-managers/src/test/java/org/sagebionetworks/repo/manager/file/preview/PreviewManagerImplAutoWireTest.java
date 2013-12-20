package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.fileupload.FileItemStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PreviewManagerImplAutoWireTest {
	
	private static String LITTLE_IMAGE_NAME = "LittleImage.png";
	
	@Autowired
	private FileHandleManager fileUploadManager;
	
	@Autowired
	private PreviewManager previewManager;
	
	@Autowired
	public UserManager userManager;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	// Only used to satisfy FKs
	private UserInfo adminUserInfo;
	private List<S3FileHandleInterface> toDelete = new LinkedList<S3FileHandleInterface>();
	
	private S3FileHandle originalfileMetadata;
	
	
	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		toDelete = new LinkedList<S3FileHandleInterface>();
		// First upload a file that we want to generate a preview for.
		FileItemStream mockFiz = Mockito.mock(FileItemStream.class);
		InputStream in = PreviewManagerImplAutoWireTest.class.getClassLoader().getResourceAsStream(LITTLE_IMAGE_NAME);
		assertNotNull("Failed to find a test file on the classpath: "+LITTLE_IMAGE_NAME, in);
		when(mockFiz.openStream()).thenReturn(in);
		when(mockFiz.getContentType()).thenReturn(ImagePreviewGenerator.IMAGE_PNG);
		when(mockFiz.getName()).thenReturn(LITTLE_IMAGE_NAME);
		// Now upload the file.
		originalfileMetadata = fileUploadManager.uploadFile(adminUserInfo.getIndividualGroup().getId(), mockFiz);
		toDelete.add(originalfileMetadata);
		System.out.println("Max preview bytes:"+previewManager.getMaxPreivewMemoryBytes());
	}
	
	@After
	public void after(){
		if(toDelete != null && s3Client != null){
			// Delete any files created
			for(S3FileHandleInterface meta: toDelete){
				// delete the file from S3.
				s3Client.deleteObject(meta.getBucketName(), meta.getKey());
				// We also need to delete the data from the database
				fileMetadataDao.delete(meta.getId());
			}
		}
	}
	
	@Test
	public void testGeneratePreview() throws TemporarilyUnavailableException, ExceedsMaximumResources, Exception{
		// Test that we can generate a preview for this image
		PreviewFileHandle pfm = previewManager.generatePreview(originalfileMetadata);
		assertNotNull(pfm);
		assertNotNull(pfm.getId());
		assertNotNull(pfm.getContentType());
		assertNotNull(pfm.getContentSize());
		toDelete.add(pfm);
		System.out.println(pfm);
		// Now make sure this id was assigned to the file
		S3FileHandle fromDB = (S3FileHandle) fileMetadataDao.get(originalfileMetadata.getId());
		assertEquals("The preview was not assigned to the file",pfm.getId(), fromDB.getPreviewId());
		// Get the preview metadata from S3
		ObjectMetadata s3Meta =s3Client.getObjectMetadata(pfm.getBucketName(), pfm.getKey());
		assertNotNull(s3Meta);
		assertEquals(ImagePreviewGenerator.IMAGE_PNG, s3Meta.getContentType());
		assertEquals(TransferUtils.getContentDispositionValue(pfm.getFileName()), s3Meta.getContentDisposition());
	}

}
