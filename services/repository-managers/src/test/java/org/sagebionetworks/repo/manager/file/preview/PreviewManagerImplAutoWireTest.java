package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.sagebionetworks.util.ContentDispositionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.model.ObjectMetadata;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PreviewManagerImplAutoWireTest {

	@Autowired
	private FileHandleManager fileUploadManager;

	@Autowired
	private PreviewManager previewManager;

	@Autowired
	public UserManager userManager;

	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private FileHandleDao fileMetadataDao;

	// Only used to satisfy FKs
	private UserInfo adminUserInfo;
	private List<S3FileHandleInterface> toDelete = new LinkedList<S3FileHandleInterface>();

	private static String LITTLE_IMAGE_NAME = "LittleImage.png";
	private static final String CSV_TEXT_FILE = "images/test.csv";

	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	private S3FileHandle createS3File(String filename) throws IOException, UnsupportedEncodingException {
		InputStream in = PreviewManagerImplAutoWireTest.class.getClassLoader().getResourceAsStream(filename);
		assertNotNull("Failed to find a test file on the classpath: " + filename, in);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(in, baos);
		byte[] fileContent = baos.toByteArray();
		baos.close();
		in.close();

		// Now upload the file.
		ContentType contentType = ContentType.create(ImagePreviewGenerator.IMAGE_PNG);
		S3FileHandle fileMetadata = fileUploadManager.createFileFromByteArray(adminUserInfo.getId().toString(), new Date(), fileContent,
				filename, contentType, null);
		toDelete.add(fileMetadata);
		return fileMetadata;
	}

	@After
	public void after() {
		if (toDelete != null && s3Client != null) {
			// Delete any files created
			for (S3FileHandleInterface meta : toDelete) {
				// delete the file from S3.
				s3Client.deleteObject(meta.getBucketName(), meta.getKey());
				// We also need to delete the data from the database
				fileMetadataDao.delete(meta.getId());
			}
		}
	}

	@Test
	public void testGeneratePreview() throws Exception {
		S3FileHandle fileMetadata = createS3File(LITTLE_IMAGE_NAME);

		// Test that we can generate a preview for this image
		fileMetadata.setContentType(ImagePreviewGenerator.IMAGE_PNG);
		PreviewFileHandle pfm = previewManager.generatePreview(fileMetadata);
		assertNotNull(pfm);
		assertNotNull(pfm.getId());
		assertNotNull(pfm.getContentType());
		assertNotNull(pfm.getContentSize());
		toDelete.add(pfm);
		System.out.println(pfm);
		// Now make sure this id was assigned to the file
		S3FileHandle fromDB = (S3FileHandle) fileMetadataDao.get(fileMetadata.getId());
		assertEquals("The preview was not assigned to the file", pfm.getId(), fromDB.getPreviewId());
		// Get the preview metadata from S3
		ObjectMetadata s3Meta = s3Client.getObjectMetadata(pfm.getBucketName(), pfm.getKey());
		assertNotNull(s3Meta);
		assertEquals(ImagePreviewGenerator.IMAGE_PNG, s3Meta.getContentType());
		assertEquals(ContentDispositionUtils.getContentDispositionValue(pfm.getFileName()), s3Meta.getContentDisposition());
	}

	@Test
	public void testCsvBeforeText() throws Exception {
		S3FileHandle fileMetadata = createS3File(CSV_TEXT_FILE);

		// Test that we can generate a preview as csv
		fileMetadata.setContentType("text/csv");
		fileMetadata.setFileName("anyname");
		PreviewFileHandle pfm = previewManager.generatePreview(fileMetadata);
		assertEquals("text/csv", pfm.getContentType());
		toDelete.add(pfm);
		S3FileHandle fromDB = (S3FileHandle) fileMetadataDao.get(fileMetadata.getId());
		assertEquals("The preview was not assigned to the file", pfm.getId(), fromDB.getPreviewId());

		// Test that we can generate a preview as text
		fileMetadata.setContentType("text/plain");
		fileMetadata.setFileName("anyname");
		pfm = previewManager.generatePreview(fileMetadata);
		assertEquals("text/plain", pfm.getContentType());
		toDelete.add(pfm);
		fromDB = (S3FileHandle) fileMetadataDao.get(fileMetadata.getId());
		assertEquals("The preview was not assigned to the file", pfm.getId(), fromDB.getPreviewId());
	}
}
