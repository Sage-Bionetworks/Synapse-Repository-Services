package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.preview.ImagePreviewGenerator;
import org.sagebionetworks.repo.manager.file.preview.TabCsvPreviewGenerator;
import org.sagebionetworks.repo.manager.file.preview.TextPreviewGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * This test validates that when a file is created, the message propagates to the 
 * preview queue, is processed by the preview worker and a preview is created.
 * @author John
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PreviewWorkerIntegrationTest {

	private static String LITTLE_IMAGE_NAME = "LittleImage.png";
	private static String LITTLE_CSV_NAME = "previewtest.csv";
	private static String LITTLE_TAB_NAME = "previewtest.tab";
	private static String LITTLE_TXT_NAME = "previewtest.txt";
	public static final long MAX_WAIT = 30*1000; // 30 seconds
	
	@Autowired
	private FileHandleManager fileUploadManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private SynapseS3Client s3Client;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
		
	private UserInfo adminUserInfo;
	private List<S3FileHandle> toDelete = new LinkedList<>();

	@BeforeEach
	public void before() throws Exception {
		// Create a file
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	public S3FileHandle uploadFile(String fileName, String mimeType) throws Exception{
		InputStream in = PreviewWorkerIntegrationTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull(in, "Failed to find a test file on the classpath: "+fileName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			IOUtils.copy(in, baos);
		} finally {
			baos.close();
		}
		// Now upload the file.
		ContentType contentType = ContentType.create(mimeType, "UTF-8");
		return fileUploadManager.createFileFromByteArray(
				adminUserInfo.getId().toString(), new Date(), baos.toByteArray(), null, contentType, null);
	}
	
	@AfterEach
	public void after(){
		if(toDelete != null && s3Client != null){
			// Delete any files created
			for(S3FileHandle meta: toDelete){
				// delete the file from S3.
				s3Client.deleteObject(meta.getBucketName(), meta.getKey());
				// We also need to delete the data from the database
				fileMetadataDao.delete(meta.getId());
			}
		}
	}	
	
	public void testRoundTripHelper(String fileName, String mimeType) throws Exception {
		S3FileHandle fileHandle = uploadFile(fileName, mimeType);
		toDelete.add(fileHandle);
		// If the preview system is setup correctly, then a preview should
		// get generated for the file that was uploaded in the before() method.
		assertNotNull(fileHandle);
		long start = System.currentTimeMillis();
		while (fileHandle.getPreviewId() == null) {
			System.out.println("Waiting for a preview to be generated for the file: " + fileHandle);
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue(elapse < MAX_WAIT, "Timed out waiting for a preview file to be generated");
			fileHandle = (S3FileHandle) fileMetadataDao.get(fileHandle.getId());
		}
		// Get the preview
		S3FileHandle pfm = (S3FileHandle) fileMetadataDao.get(fileHandle.getPreviewId());
		assertNotNull(pfm);
		// Make sure the preview is deleted as well
		toDelete.add(pfm);
	}
	
	@Test
	public void testRoundTripImage() throws Exception {
		testRoundTripHelper(LITTLE_IMAGE_NAME, ImagePreviewGenerator.IMAGE_PNG);
	}
	
	@Test
	public void testRoundTripCsv() throws Exception {
		testRoundTripHelper(LITTLE_CSV_NAME, TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES);
	}
	
	@Test
	public void testRoundTripTab() throws Exception {
		testRoundTripHelper(LITTLE_TAB_NAME, "text/tsv");
	}
	
	@Test
	public void testRoundTripTxt() throws Exception {
		testRoundTripHelper(LITTLE_TXT_NAME, TextPreviewGenerator.TEXT_PLAIN);
	}
}
