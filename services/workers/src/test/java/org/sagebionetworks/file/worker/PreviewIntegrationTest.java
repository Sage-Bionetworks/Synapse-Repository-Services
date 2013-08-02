package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.preview.ImagePreviewGenerator;
import org.sagebionetworks.repo.manager.file.preview.TabCsvPreviewGenerator;
import org.sagebionetworks.repo.manager.file.preview.TextPreviewGenerator;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This test validates that when a file is created, the message propagates to the 
 * preview queue, is processed by the preview worker and a preview is created.
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PreviewIntegrationTest {

	private static String LITTLE_IMAGE_NAME = "LittleImage.png";
	private static String LITTLE_CSV_NAME = "previewtest.csv";
	private static String LITTLE_TAB_NAME = "previewtest.tab";
	private static String LITTLE_TXT_NAME = "previewtest.txt";
	public static final long MAX_WAIT = 30*1000; // 30 seconds
	
	@Autowired
	FileHandleManager fileUploadManager;
	@Autowired
	private UserProvider userProvider;
	@Autowired
	private MessageReceiver fileQueueMessageReveiver;
	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	FileHandleDao fileMetadataDao;
	
	List<S3FileHandleInterface> toDelete;
	UserInfo userInfo;
	S3FileHandle imageFileHandle, csvFileHandle, tabFileHandle, txtFileHandle;
	
	@Before
	public void before() throws Exception {
		// Before we start, make sure the queue is empty
		emptyQueue();
		// Create a file
		userInfo = userProvider.getTestUserInfo();
		toDelete = new LinkedList<S3FileHandleInterface>();
		// First upload a file that we want to generate a preview for.
		imageFileHandle = uploadFile(LITTLE_IMAGE_NAME, ImagePreviewGenerator.IMAGE_PNG);
		toDelete.add(imageFileHandle);
		
		csvFileHandle = uploadFile(LITTLE_CSV_NAME, TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES);
		toDelete.add(csvFileHandle);
		
		tabFileHandle = uploadFile(LITTLE_TAB_NAME, TabCsvPreviewGenerator.TEXT_TAB_SEPARATED_VALUES);
		toDelete.add(tabFileHandle);
		
		txtFileHandle = uploadFile(LITTLE_TXT_NAME, TextPreviewGenerator.TEXT_PLAIN);
		toDelete.add(txtFileHandle);
	}
	
	public S3FileHandle uploadFile(String fileName, String contentType) throws Exception{
		FileItemStream mockFiz = Mockito.mock(FileItemStream.class);
		InputStream in = PreviewIntegrationTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find a test file on the classpath: "+fileName, in);
		when(mockFiz.openStream()).thenReturn(in);
		when(mockFiz.getContentType()).thenReturn(contentType);
		when(mockFiz.getName()).thenReturn(fileName);
		// Now upload the file.
		return fileUploadManager.uploadFile(userInfo.getIndividualGroup().getId(), mockFiz);
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

	/**
	 * Empty the queue by processing all messages on the queue.
	 * @throws InterruptedException
	 */
	public void emptyQueue() throws InterruptedException {
		long start = System.currentTimeMillis();
		int count = 0;
		do{
			count = fileQueueMessageReveiver.triggerFired();
			System.out.println("Emptying the file message queue, there were at least: "+count+" messages on the queue");
			Thread.yield();
			long elapse = System.currentTimeMillis()-start;
			if(elapse > MAX_WAIT*2) throw new RuntimeException("Timed-out waiting process all messages that were on the queue before the tests started.");
		}while(count > 0);
	}

	
	
	public void testRoundTripHelper(S3FileHandle imageFileHandle) throws Exception {
		// If the preview system is setup correctly, then a preview should
		// get generated for the file that was uploaded in the before() method.
		assertNotNull(imageFileHandle);
		long start = System.currentTimeMillis();
		while(imageFileHandle.getPreviewId() == null){
			System.out.println("Waiting for a preview to be generated for the file: "+imageFileHandle);
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for a preview file to be generated", elapse < MAX_WAIT);
			imageFileHandle = (S3FileHandle) fileMetadataDao.get(imageFileHandle.getId());
		}
		// Get the preview
		PreviewFileHandle pfm = (PreviewFileHandle) fileMetadataDao.get(imageFileHandle.getPreviewId());
		assertNotNull(pfm);
		// Make sure the preview is deleted as well
		toDelete.add(pfm);
	}
	
	@Test
	public void testRoundTripImage() throws Exception {
		testRoundTripHelper(imageFileHandle);
	}
	
	@Test
	public void testRoundTripCsv() throws Exception {
		testRoundTripHelper(csvFileHandle);
	}
	
	@Test
	public void testRoundTripTab() throws Exception {
		testRoundTripHelper(tabFileHandle);
	}
	
	@Test
	public void testRoundTripTxt() throws Exception {
		testRoundTripHelper(txtFileHandle);
	}
}
