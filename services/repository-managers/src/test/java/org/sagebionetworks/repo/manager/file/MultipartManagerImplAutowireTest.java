package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
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
	private AmazonS3Client s3Client;
	
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
	
	@Test
	public void testChunckedFileUploadV1() throws Exception {
		String fileBody = "This is the body of the file!!!!!";
		byte[] fileBodyBytes = fileBody.getBytes("UTF-8");
		String md5 = TransferUtils.createMD5(fileBodyBytes);
		// First create a chunked file token
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		String userId = adminUserInfo.getId().toString();
		String fileName = "foo.bar";
		ccftr.setFileName(fileName);
		ccftr.setContentType("text/plain");
		ccftr.setContentMD5(md5);
		ChunkedFileToken token = multipartManager.createChunkedFileUploadToken(ccftr, null, userId);
		assertNotNull(token);
		assertNotNull(token.getKey());
		assertNotNull(token.getUploadId());
		assertNotNull(md5, token.getContentMD5());
		// the key must start with the user's id
		assertTrue(token.getKey().startsWith(userId));
		// Now create a pre-signed URL for the first part
		ChunkRequest cpr = new ChunkRequest();
		cpr.setChunkedFileToken(token);
		cpr.setChunkNumber(1l);
		URL preSigned = multipartManager.createChunkedFileUploadPartURL(cpr, null);
		assertNotNull(preSigned);
		String urlString = preSigned.toString();
		// This was added as a regression test for PLFM-1925.  When we upgraded to AWS client 1.4.3, it changes how the URLs were prepared and broke
		// both file upload and download.
		assertTrue("If the presigned url does not start with https://s3.amazonaws.com it will cause SSL failures. See PLFM-1925",urlString.startsWith("https://s3.amazonaws.com", 0));
		// Before we put data to the URL the part should not exist
		assertFalse(multipartManager.doesPartExist(token, 1, null));
		// Now upload the file to the URL
		// Use the URL to upload a part.
		HttpPut httppost = new HttpPut(preSigned.toString());
		StringEntity entity = new StringEntity(fileBody, "UTF-8");
		entity.setContentType(ccftr.getContentType());
		httppost.setEntity(entity);
		HttpResponse response = DefaultHttpClientSingleton.getInstance().execute(httppost);
		String text = EntityUtils.toString(response.getEntity());
		assertEquals(200, response.getStatusLine().getStatusCode());
		System.out.println(text);
		// The part should now exist
		assertTrue(multipartManager.doesPartExist(token, 1, null));
	
		// Make sure we can get the pre-signed url again if we need to.
		preSigned = multipartManager.createChunkedFileUploadPartURL(cpr, null);
		assertNotNull(preSigned);
		
		// Next add the part
		ChunkResult part = multipartManager.copyPart(token, 1, null);
		
		// We need a list of parts
		List<ChunkResult> partList = new LinkedList<ChunkResult>();
		partList.add(part);
		CompleteChunkedFileRequest ccfr = new CompleteChunkedFileRequest();
		ccfr.setChunkedFileToken(token);
		ccfr.setChunkResults(partList);
		// We are now read to create our file handle from the parts
		S3FileHandle multiPartHandle = multipartManager.completeChunkFileUpload(ccfr, null, userId);
		assertNotNull(multiPartHandle);
		assertNotNull(multiPartHandle.getId());
		fileHandlesToDelete.add(multiPartHandle.getId());
		System.out.println(multiPartHandle);
		assertNotNull(multiPartHandle.getBucketName());
		assertNotNull(multiPartHandle.getKey());
		assertNotNull(multiPartHandle.getContentSize());
		assertNotNull(multiPartHandle.getContentType());
		assertNotNull(multiPartHandle.getCreatedOn());
		assertNotNull(multiPartHandle.getCreatedBy());
		assertEquals(md5, multiPartHandle.getContentMd5());
		// The part should be deleted
		assertFalse("The part should have been deleted upon completion of the multi-part upload",
				multipartManager.doesPartExist(token, 1, null));
	}
	
	/**
	 * Test the upload of a local file.
	 * @throws IOException 
	 */
	@Test
	public void testMultipartUploadLocalFile() throws IOException{
		File temp  = File.createTempFile("testMultipartUploadLocalFile", ".txt");
		try{
			String fileBody = "This is the body of the file!!!!!";
			byte[] fileBodyBytes = fileBody.getBytes("UTF-8");
			String md5 = TransferUtils.createMD5(fileBodyBytes);
			FileUtils.writeStringToFile(temp, fileBody);
			String contentType = "text/plain";
			// Now upload the file to S3
			S3FileHandle handle = multipartManager.multipartUploadLocalFile(null, adminUserInfo.getId().toString(), temp, contentType,
					new ProgressListener() {
				@Override
				public void progressChanged(ProgressEvent progressEvent) {
					System.out.println("FileUpload bytesTransfered: : "+progressEvent.getBytesTransferred());
					
				}});
			assertNotNull(handle);
			fileHandlesToDelete.add(handle.getId());
			assertEquals(md5, handle.getContentMd5());
			assertEquals(temp.getName(), handle.getFileName());
			assertEquals(contentType, handle.getContentType());
			assertEquals(new Long(temp.length()), handle.getContentSize());
			assertEquals(adminUserInfo.getId().toString(), handle.getCreatedBy());
			assertNotNull(handle.getBucketName());
			assertNotNull(handle.getKey());
			assertNotNull(handle.getContentSize());
		}finally{
			temp.delete();
		}
	}
}
