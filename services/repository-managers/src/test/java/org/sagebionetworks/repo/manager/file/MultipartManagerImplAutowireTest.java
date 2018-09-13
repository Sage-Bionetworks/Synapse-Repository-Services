package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MultipartManagerImplAutowireTest {

	@Autowired
	private MultipartManager multipartManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	public UserManager userManager;
	
	// Only used to satisfy FKs
	private UserInfo adminUserInfo;
	private List<String> fileHandlesToDelete;
	
	@Before
	public void before() throws Exception {
		fileHandlesToDelete = new LinkedList<String>();
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@After
	public void after(){
		if(fileHandlesToDelete != null){
			for(String id: fileHandlesToDelete){
				try {
					fileHandleDao.delete(id);
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Test the upload of a local file.
	 * @throws IOException 
	 */
	@Test
	public void testMultipartUploadLocalFileNullFileName() throws IOException {
		File temp = File.createTempFile("testMultipartUploadLocalFile", ".txt");
		try {
			String fileBody = "This is the body of the file!!!!!";
			byte[] fileBodyBytes = fileBody.getBytes("UTF-8");
			String md5 = TransferUtils.createMD5(fileBodyBytes);
			FileUtils.writeStringToFile(temp, fileBody);
			String contentType = "text/plain";
			// Now upload the file to S3
			S3FileHandle handle = multipartManager.multipartUploadLocalFile(
					new LocalFileUploadRequest().withFileName(null).withUserId(adminUserInfo.getId().toString())
							.withFileToUpload(temp).withContentType(contentType).withListener(new ProgressListener() {
								@Override
								public void progressChanged(ProgressEvent progressEvent) {
									System.out.println(
											"FileUpload bytesTransfered: : " + progressEvent.getBytesTransferred());

								}
							}));
			assertNotNull(handle);
			fileHandlesToDelete.add(handle.getId());
			assertEquals(md5, handle.getContentMd5());
			assertEquals(temp.getName(), handle.getFileName());
			assertEquals(contentType, handle.getContentType());
			assertEquals(new Long(temp.length()), handle.getContentSize());
			assertEquals(adminUserInfo.getId().toString(), handle.getCreatedBy());
			assertNotNull(handle.getBucketName());
			assertNotNull(handle.getKey());
			assertTrue(handle.getKey().contains(temp.getName()));
			assertNotNull(handle.getContentSize());
		} finally {
			temp.delete();
		}
	}
	
	@Test
	public void testMultipartUploadLocalFileWithName() throws IOException {
		File temp = File.createTempFile("testMultipartUploadLocalFile", ".txt");
		try {
			String fileBody = "This is the body of the file!!!!!";
			byte[] fileBodyBytes = fileBody.getBytes("UTF-8");
			String md5 = TransferUtils.createMD5(fileBodyBytes);
			FileUtils.writeStringToFile(temp, fileBody);
			String contentType = "text/plain";
			String fileName = "aRealFileName";
			// Now upload the file to S3
			S3FileHandle handle = multipartManager.multipartUploadLocalFile(
					new LocalFileUploadRequest().withFileName(fileName).withUserId(adminUserInfo.getId().toString())
							.withFileToUpload(temp).withContentType(contentType).withListener(new ProgressListener() {
								@Override
								public void progressChanged(ProgressEvent progressEvent) {
									System.out.println(
											"FileUpload bytesTransfered: : " + progressEvent.getBytesTransferred());

								}
							}));
			assertNotNull(handle);
			fileHandlesToDelete.add(handle.getId());
			assertEquals(md5, handle.getContentMd5());
			assertEquals(fileName, handle.getFileName());
			assertEquals(contentType, handle.getContentType());
			assertEquals(new Long(temp.length()), handle.getContentSize());
			assertEquals(adminUserInfo.getId().toString(), handle.getCreatedBy());
			assertNotNull(handle.getBucketName());
			assertNotNull(handle.getKey());
			assertTrue(handle.getKey().contains(fileName));
			assertNotNull(handle.getContentSize());
		} finally {
			temp.delete();
		}
	}
}
