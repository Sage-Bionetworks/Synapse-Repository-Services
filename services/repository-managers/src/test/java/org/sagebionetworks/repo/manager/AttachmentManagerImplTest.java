package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ExampleEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.PreviewState;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for the AttachmentManager
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AttachmentManagerImplTest {

	public static final long MAX_PREVIEW_WAIT = 40*1000;
	@Autowired
	AttachmentManager attachmentManager;
	
	@Autowired
	AmazonS3Utility s3Utility;
	
	@Autowired
	IdGenerator idGenerator;
	
	List<String> keysToDelete;
	
	@Before
	public void before(){
		keysToDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		if(keysToDelete != null && s3Utility != null){
			for(String key: keysToDelete){
				System.out.println("Deleting S3 object: "+key);
				s3Utility.deleteFromS3(key);
			}
		}
	}
	
	@Test
	public void testCreatePreviewImageLocalUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException, InterruptedException{
		AttachmentData data = new AttachmentData();
		data.setName("shortWide.gif");
		String fileName = "images/shortWide.gif";
		URL url = AttachmentManagerUnitTest.class.getClassLoader().getResource(fileName);
		assertNotNull("Could not find: "+fileName+" on the classpath", url);
		data.setUrl(url.toString());
		String entityId = "123";
		ExampleEntity entity = new ExampleEntity();
		entity.setId(entityId);
		entity.setAttachments(new ArrayList<AttachmentData>());
		entity.getAttachments().add(data);
		// Make sure we can create a preview
		attachmentManager.checkAttachmentsForPreviews(entity);
		// Validate that an image was upload to S3.
		assertEquals(PreviewState.PREVIEW_EXISTS, data.getPreviewState());
		assertNotNull(data.getPreviewId());
		// Make sure we can download the preview
		String key = S3TokenManagerImpl.createAttachmentPathNoSlash(entity.getId(), data.getPreviewId());
		waitForPreview(key);
		// Make sure this preview gets deleted
		keysToDelete.add(key);
		// Wait for the key file to be finsihed
		File temp = s3Utility.downloadFromS3(key);
		assertNotNull(temp);
		System.out.println(temp.getAbsolutePath());
		assertTrue("The preview image should be larger than zero bytes", temp.length() > 0);
		assertTrue("The preview image should be smaller than 100K bytes", temp.length() < 100*1000);
		temp.delete();
	}
	@Ignore
	@Test
	public void testCreatePreviewImageS3() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException, InterruptedException{
		String entityId = idGenerator.generateNewId().toString();
		AttachmentData data = new AttachmentData();
		data.setName("shortWide.gif");
		String fileName = "images/shortWide.gif";
		URL url = AttachmentManagerUnitTest.class.getClassLoader().getResource(fileName);
		assertNotNull("Could not find: "+fileName+" on the classpath", url);
		File toUpload = new File(url.getFile());
		String tokenId = S3TokenManagerImpl.createTokenId(idGenerator.generateNewId(), data.getName());
		String attachmentS3Key = S3TokenManagerImpl.createAttachmentPathNoSlash(entityId, tokenId);
		// Upload this file
		s3Utility.uploadToS3(toUpload, attachmentS3Key);
		// Make sure this file gets deleted
		keysToDelete.add(attachmentS3Key);
		// Now create the preview
		data.setTokenId(tokenId);
		ExampleEntity entity = new ExampleEntity();
		entity.setId(entityId);
		entity.setAttachments(new ArrayList<AttachmentData>());
		entity.getAttachments().add(data);
		// Make sure we can create a preview
		attachmentManager.checkAttachmentsForPreviews(entity);
		// Validate that an image was upload to S3.
		assertEquals(PreviewState.PREVIEW_EXISTS, data.getPreviewState());
		assertNotNull(data.getPreviewId());
		// Make sure we can download the preview
		String previewKey = S3TokenManagerImpl.createAttachmentPathNoSlash(entity.getId(), data.getPreviewId());
		// Wait for the preview
		waitForPreview(previewKey);
		// Make sure this preview gets deleted
		keysToDelete.add(previewKey);
		File temp = s3Utility.downloadFromS3(previewKey);
		assertNotNull(temp);
		System.out.println(temp.getAbsolutePath());
		assertTrue("The preview image should be larger than zero bytes", temp.length() > 0);
		assertTrue("The preview image should be smaller than 100K bytes", temp.length() < 100*1000);
		temp.delete();
	}
	
	/**
	 * Wait for a preview to be created.
	 * @param key
	 * @throws InterruptedException
	 */
	private void waitForPreview(String key) throws InterruptedException {
		long start = System.currentTimeMillis();
		while(!s3Utility.doesExist(key)){
			System.out.println("Waiting for preview to be created...");
			Thread.sleep(1000);
			long now = System.currentTimeMillis();
			long elapse = now-start;
			assertTrue("Timed out waiting for a preview to be created", elapse < MAX_PREVIEW_WAIT);
		}
	}

	
}
