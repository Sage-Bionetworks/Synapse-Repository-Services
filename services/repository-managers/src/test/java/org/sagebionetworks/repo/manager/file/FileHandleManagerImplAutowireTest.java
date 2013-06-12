package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CORSRule.AllowedMethods;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringInputStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleManagerImplAutowireTest {
	
	public static final long MAX_UPLOAD_WORKER_TIME_MS = 20*1000;
	
	List<S3FileHandle> toDelete;
	
	@Autowired
	FileHandleManager fileUploadManager;
	
	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	public UserProvider testUserProvider;
	
	private UserInfo userInfo;
	
	/**
	 * This is the metadata about the files we uploaded.
	 */
	private List<S3FileHandle> expectedMetadata;
	private String[] fileContents;
	private List<FileItemStream> fileStreams;
	
	@Before
	public void before() throws Exception{
		assertNotNull(testUserProvider);
		userInfo = testUserProvider.getTestUserInfo();
		toDelete = new LinkedList<S3FileHandle>();
		// Setup the mock file to upload.
		int numberFiles = 2;
		expectedMetadata = new LinkedList<S3FileHandle>();
		fileStreams = new LinkedList<FileItemStream>();
		fileContents = new String[numberFiles];
		for(int i=0; i<numberFiles; i++){
			fileContents[i] = "This is the contents for file: "+i;
			byte[] fileBytes = fileContents[i].getBytes();
			String fileName = "foo-"+i+".txt";
			String contentType = "text/plain";
			FileItemStream fis = Mockito.mock(FileItemStream.class);
			when(fis.getContentType()).thenReturn(contentType);
			when(fis.getName()).thenReturn(fileName);
			when(fis.openStream()).thenReturn(new StringInputStream(fileContents[i]));
			fileStreams.add(fis);
			// Set the expected metadata for this file.
			S3FileHandle metadata = new S3FileHandle();
			metadata.setContentType(contentType);
			metadata.setContentMd5( BinaryUtils.toHex((MessageDigest.getInstance("MD5").digest(fileBytes))));
			metadata.setContentSize(new Long(fileBytes.length));
			metadata.setFileName(fileName);
			expectedMetadata.add(metadata);
		}
	}
	
	@After
	public void after(){
		if(toDelete != null && s3Client != null){
			// Delete any files created
			for(S3FileHandle meta: toDelete){
				// delete the file from S3.
				s3Client.deleteObject(meta.getBucketName(), meta.getKey());
				// We also need to delete the data from the database
				fileHandleDao.delete(meta.getId());
			}
		}
	}
	

	@Test
	public void testUploadfiles() throws FileUploadException, IOException, ServiceUnavailableException, NoSuchAlgorithmException, DatastoreException, NotFoundException{
		FileItemIterator mockIterator = Mockito.mock(FileItemIterator.class);
		// The first file.
		// Mock two streams
		when(mockIterator.hasNext()).thenReturn(true, true, false);
		// Use the first two files.
		when(mockIterator.next()).thenReturn(fileStreams.get(0), fileStreams.get(1));
		// Upload the files.
		FileUploadResults results = fileUploadManager.uploadfiles(userInfo, new HashSet<String>(), mockIterator);
		assertNotNull(results);
		assertNotNull(results.getFiles());
		toDelete.addAll(results.getFiles());
		assertEquals(2, results.getFiles().size());
		// Now verify the results
		for(int i=0; i<2; i++){
			S3FileHandle metaResult = results.getFiles().get(i);
			assertNotNull(metaResult);
			S3FileHandle expected = expectedMetadata.get(i);
			assertNotNull(expected);
			// Validate the expected values
			assertEquals(expected.getFileName(), metaResult.getFileName());
			assertEquals(expected.getContentMd5(), metaResult.getContentMd5());
			assertEquals(expected.getContentSize(), metaResult.getContentSize());
			assertEquals(expected.getContentType(), metaResult.getContentType());
			assertNotNull("An id should have been assigned to this file", metaResult.getId());
			assertNotNull("CreatedOn should have been filled in.", metaResult.getCreatedOn());
			assertEquals("CreatedBy should match the user that created the file.", userInfo.getIndividualGroup().getId(), metaResult.getCreatedBy());
			assertEquals(StackConfiguration.getS3Bucket(), metaResult.getBucketName());
			assertNotNull(metaResult.getKey());
			assertTrue("The key should start with the userID", metaResult.getKey().startsWith(userInfo.getIndividualGroup().getId()));			
			// Validate this is in the database
			S3FileHandle fromDB = (S3FileHandle) fileHandleDao.get(metaResult.getId());
			assertEquals(metaResult, fromDB);
			// Test the Pre-Signed URL
			URL presigned = fileUploadManager.getRedirectURLForFileHandle(metaResult.getId());
			assertNotNull(presigned);
			String urlString = presigned.toString();
			// This was added as a regression test for PLFM-1925.  When we upgraded to AWS client 1.4.3, it changes how the URLs were prepared and broke
			// both file upload and download.
			assertTrue("If the presigned url does not start with https://s3.amazonaws.com it will cause SSL failures. See PLFM-1925",urlString.startsWith("https://s3.amazonaws.com", 0));
		}
	}
	
	/**
	 * The cross-origin resource sharing (CORS) setting are required so the browser javascript code can talk directly to S3.
	 * 
	 */
	@Test
	public void testCORSSettings(){
		BucketCrossOriginConfiguration bcoc = fileUploadManager.getBucketCrossOriginConfiguration();
		assertNotNull(bcoc);
		assertNotNull(bcoc.getRules());
		assertEquals(1, bcoc.getRules().size());
		CORSRule rule = bcoc.getRules().get(0);
		assertNotNull(rule);
		assertEquals(FileHandleManager.AUTO_GENERATED_ALLOW_ALL_CORS_RULE_ID, rule.getId());
		assertNotNull(rule.getAllowedOrigins());
		assertEquals(1, rule.getAllowedOrigins().size());
		assertEquals("*", rule.getAllowedOrigins().get(0));
		assertNotNull(rule.getAllowedMethods());
		assertTrue(rule.getAllowedMethods().contains(AllowedMethods.GET));
		assertTrue(rule.getAllowedMethods().contains(AllowedMethods.PUT));
		assertTrue(rule.getAllowedMethods().contains(AllowedMethods.POST));
		assertTrue(rule.getAllowedMethods().contains(AllowedMethods.HEAD));
		assertEquals(300, rule.getMaxAgeSeconds());
		// the wildcard headers in not working
		assertNotNull(rule.getAllowedHeaders());
		assertEquals(1, rule.getAllowedHeaders().size());
		assertEquals("*", rule.getAllowedHeaders().get(0));
	}
	
	@Test
	public void testChunckedFileUpload() throws ClientProtocolException, IOException{
		String fileBody = "This is the body of the file!!!!!";
		byte[] fileBodyBytes = fileBody.getBytes("UTF-8");
		String md5 = TransferUtils.createMD5(fileBodyBytes);
		// First create a chunked file token
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		String fileName = "foo.bar";
		ccftr.setFileName(fileName);
		ccftr.setContentType("text/plain");
		ccftr.setContentMD5(md5);
		ChunkedFileToken token = fileUploadManager.createChunkedFileUploadToken(userInfo, ccftr);
		assertNotNull(token);
		assertNotNull(token.getKey());
		assertNotNull(token.getUploadId());
		assertNotNull(md5, token.getContentMD5());
		// the key must start with the user's id
		assertTrue(token.getKey().startsWith(userInfo.getIndividualGroup().getId()));
		// Now create a pre-signed URL for the first part
		ChunkRequest cpr = new ChunkRequest();
		cpr.setChunkedFileToken(token);
		cpr.setChunkNumber(1l);
		URL preSigned = fileUploadManager.createChunkedFileUploadPartURL(userInfo, cpr);
		assertNotNull(preSigned);
		String urlString = preSigned.toString();
		// This was added as a regression test for PLFM-1925.  When we upgraded to AWS client 1.4.3, it changes how the URLs were prepared and broke
		// both file upload and download.
		assertTrue("If the presigned url does not start with https://s3.amazonaws.com it will cause SSL failures. See PLFM-1925",urlString.startsWith("https://s3.amazonaws.com", 0));
		String text = putStringToPresignedURL(fileBody, ccftr, preSigned);
		System.out.println(text);
	
		// Make sure we can get the pre-signed url again if we need to.
		preSigned = fileUploadManager.createChunkedFileUploadPartURL(userInfo, cpr);
		assertNotNull(preSigned);
		
		// Next add the part
		ChunkResult part = fileUploadManager.addChunkToFile(userInfo, cpr);
		
		// We need a lsit of parts
		List<ChunkResult> partList = new LinkedList<ChunkResult>();
		partList.add(part);
		CompleteChunkedFileRequest ccfr = new CompleteChunkedFileRequest();
		ccfr.setChunkedFileToken(token);
		ccfr.setChunkResults(partList);
		// We are now read to create our file handle from the parts
		S3FileHandle multiPartHandle = fileUploadManager.completeChunkFileUpload(userInfo, ccfr);
		assertNotNull(multiPartHandle);
		toDelete.add(multiPartHandle);
		System.out.println(multiPartHandle);
		assertNotNull(multiPartHandle.getBucketName());
		assertNotNull(multiPartHandle.getKey());
		assertNotNull(multiPartHandle.getContentSize());
		assertNotNull(multiPartHandle.getContentType());
		assertNotNull(multiPartHandle.getCreatedOn());
		assertNotNull(multiPartHandle.getCreatedBy());
		assertEquals(md5, multiPartHandle.getContentMd5());
		// Delete the file
		s3Client.deleteObject(multiPartHandle.getBucketName(), multiPartHandle.getKey());
		
	}
	
	@Test
	public void testChunckedFileUploadAsynch() throws Exception{
		String fileBody = "This is the body of the file!!!!!";
		byte[] fileBodyBytes = fileBody.getBytes("UTF-8");
		String md5 = TransferUtils.createMD5(fileBodyBytes);
		// First create a chunked file token
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		String fileName = "foo.bar";
		ccftr.setFileName(fileName);
		ccftr.setContentType("text/plain");
		ccftr.setContentMD5(md5);
		ChunkedFileToken token = fileUploadManager.createChunkedFileUploadToken(userInfo, ccftr);
		assertNotNull(token);
		assertNotNull(token.getKey());
		assertNotNull(token.getUploadId());
		assertNotNull(md5, token.getContentMD5());
		// the key must start with the user's id
		assertTrue(token.getKey().startsWith(userInfo.getIndividualGroup().getId()));
		// Now create a pre-signed URL for the first part
		ChunkRequest cpr = new ChunkRequest();
		cpr.setChunkedFileToken(token);
		cpr.setChunkNumber(1l);
		URL preSigned = fileUploadManager.createChunkedFileUploadPartURL(userInfo, cpr);
		assertNotNull(preSigned);
		// Use the URL to upload a part.
		String text = putStringToPresignedURL(fileBody, ccftr, preSigned);
		System.out.println(text);
			
		// Start the asynch multi-part complete.
		CompleteAllChunksRequest cacr = new CompleteAllChunksRequest();
		cacr.setChunkedFileToken(token);
		cacr.setChunkNumbers(new LinkedList<Long>());
		cacr.getChunkNumbers().add(1l);
		UploadDaemonStatus daemonStatus = fileUploadManager.startUploadDeamon(userInfo, cacr);
		assertNotNull(daemonStatus);
		assertEquals(State.PROCESSING, daemonStatus.getState());
		assertEquals(null, daemonStatus.getFileHandleId());
		System.out.println(daemonStatus.toString());
		// Wait for the daemon to finish
		daemonStatus = waitForUploadDaemon(daemonStatus);
		assertNotNull(daemonStatus);
		System.out.println(daemonStatus.toString());
		assertEquals(State.COMPLETED, daemonStatus.getState());
		assertEquals(100, daemonStatus.getPercentComplete(), 0.0001);
		assertEquals(userInfo.getIndividualGroup().getId(), daemonStatus.getStartedBy());
		assertEquals(null, daemonStatus.getErrorMessage());
		assertNotNull(daemonStatus.getFileHandleId());
		// Get the file handle
		S3FileHandle multiPartHandle = (S3FileHandle) fileUploadManager.getRawFileHandle(userInfo, daemonStatus.getFileHandleId());
		assertNotNull(multiPartHandle);
		toDelete.add(multiPartHandle);
		System.out.println(multiPartHandle);
		assertNotNull(multiPartHandle.getBucketName());
		assertNotNull(multiPartHandle.getKey());
		assertNotNull(multiPartHandle.getContentSize());
		assertNotNull(multiPartHandle.getContentType());
		assertNotNull(multiPartHandle.getCreatedOn());
		assertNotNull(multiPartHandle.getCreatedBy());
		assertEquals(md5, multiPartHandle.getContentMd5());
		// Delete the file
		s3Client.deleteObject(multiPartHandle.getBucketName(), multiPartHandle.getKey());
		
	}

	/**
	 * Helper to wait for an upload deamon to finish.
	 * @param daemonStatus
	 * @return
	 * @throws InterruptedException
	 * @throws NotFoundException
	 */
	private UploadDaemonStatus waitForUploadDaemon(UploadDaemonStatus daemonStatus)
			throws InterruptedException, NotFoundException {
		// Wait for the daemon status
		long start = System.currentTimeMillis();
		while(State.COMPLETED != daemonStatus.getState()){
			assertFalse("Upload daemon failed: "+daemonStatus.getErrorMessage(), State.FAILED == daemonStatus.getState());
			System.out.println("Waiting for upload daemon to complete multi-part upload...");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for upload to finish",System.currentTimeMillis() - start < MAX_UPLOAD_WORKER_TIME_MS);
			daemonStatus = fileUploadManager.getUploadDaemonStatus(userInfo, daemonStatus.getDaemonId());
		}
		return daemonStatus;
	}

	/**
	 * PUT a string to pre-siged URL.
	 * @param fileBody
	 * @param ccftr
	 * @param preSigned
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private String putStringToPresignedURL(String fileBody,	CreateChunkedFileTokenRequest ccftr, URL preSigned)
			throws UnsupportedEncodingException, IOException,
			ClientProtocolException {
		HttpPut httppost = new HttpPut(preSigned.toString());
		StringEntity entity = new StringEntity(fileBody, "UTF-8");
		entity.setContentType(ccftr.getContentType());
		httppost.setEntity(entity);
		HttpResponse response = DefaultHttpClientSingleton.getInstance().execute(httppost);
		String text = EntityUtils.toString(response.getEntity());
		assertEquals(200, response.getStatusLine().getStatusCode());
		return text;
	}
	
}
