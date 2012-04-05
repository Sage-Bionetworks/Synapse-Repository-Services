package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
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

	@Autowired
	AttachmentManager attachmentManager;
	
	@Autowired
	AmazonS3Utility s3Utility;
	
	@Test
	public void testCreatePreviewImage() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
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
		String key = S3TokenManagerImpl.createAttachmentPath(entity.getId(), data.getPreviewId());
		File temp = s3Utility.downloadFromS3(key);
		assertNotNull(temp);
		System.out.println(temp.getAbsolutePath());
		assertTrue("The preview image should be larger than zero bytes", temp.length() > 0);
		assertTrue("The preview image should be smaller than 100K bytes", temp.length() < 100*1000);
		temp.delete();
	}
	

	
}
