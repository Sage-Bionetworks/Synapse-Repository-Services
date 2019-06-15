package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.AddPartState;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.Md5Utils;
import com.amazonaws.util.StringInputStream;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MultipartManagerV2ImplAutowireTest {

	@Autowired
	MultipartManagerV2 multipartManagerV2;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	public UserManager userManager;

	static SimpleHttpClient simpleHttpClient;

	private UserInfo adminUserInfo;
	private List<String> fileHandlesToDelete;

	MultipartUploadRequest request;
	String fileDataString;
	byte[] fileDataBytes;
	String fileMD5Hex;

	@BeforeClass
	public static void beforeClass() {
		simpleHttpClient = new SimpleHttpClientImpl();
	}

	@Before
	public void before() throws Exception {		
		// used to put data to a pre-signed url.
		fileHandlesToDelete = new LinkedList<String>();
		adminUserInfo = userManager
				.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER
						.getPrincipalId());

		fileDataString = "The text of this file";
		fileDataBytes = fileDataString.getBytes("UTF-8");
		request = new MultipartUploadRequest();
		request.setContentType("plain/text");
		request.setFileName("foo.txt");
		request.setFileSizeBytes(new Long(fileDataBytes.length));
		request.setPartSizeBytes(PartUtils.MIN_PART_SIZE_BYTES);
		request.setStorageLocationId(null);
		// calculate the MD5
		byte[] md5 = Md5Utils.computeMD5Hash(fileDataBytes);
		fileMD5Hex = BinaryUtils.toHex(md5);
		request.setContentMD5Hex(fileMD5Hex);
	}

	@After
	public void after() {
		if (fileHandlesToDelete != null) {
			for (String id : fileHandlesToDelete) {
				try {
					fileHandleDao.delete(id);
				} catch (Exception e) {
				}
			}
		}
		multipartManagerV2.truncateAll();
	}

	@Test
	public void testMultipartUpload() throws Exception {
		// step one start the upload.
		MultipartUploadStatus status = startUpload();
		String contentType = null;
		// step two get pre-signed URLs for the parts
		String preSignedUrl = getPresignedURLForPart(status.getUploadId(), contentType);
		validateUrl(preSignedUrl);
		// step three put the part to the URL
		putStringToURL(preSignedUrl, fileDataString, contentType);
		// step four add the part to the upload
		addPart(status.getUploadId());
		// Step five complete the upload
		MultipartUploadStatus finalStatus = completeUpload(status.getUploadId());
		// validate the results
		assertNotNull(finalStatus);
		assertEquals("1", finalStatus.getPartsState());
		assertEquals(MultipartUploadState.COMPLETED, finalStatus.getState());
		assertNotNull(finalStatus.getResultFileHandleId());
		fileHandlesToDelete.add(finalStatus.getResultFileHandleId());
	}
	
	/**
	 * Validation added for SYNPY-409 & PLFM-4183
	 */
	void validateUrl(String preSignedUrl) throws MalformedURLException {
		URL url = new URL(preSignedUrl);
		Map<String, String> map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(url.getQuery());
		String expiresString = map.get("Expires");
		assertNotNull("Expected the hacked 'Expires' parameter to be added to the URL for PLFM-4183", expiresString);
		long expires = Long.parseLong(expiresString);
		assertTrue("The hacked 'Expires' parameter should not be expired", (System.currentTimeMillis()/1000) < expires);
	}

	@Test
	public void testMultipartUploadWithContentType() throws Exception {
		// step one start the upload.
		MultipartUploadStatus status = startUpload();
		String contentType = "application/octet-stream";
		// step two get pre-signed URLs for the parts
		String preSignedUrl = getPresignedURLForPart(status.getUploadId(), contentType);
		// step three put the part to the URL
		putStringToURL(preSignedUrl, fileDataString, contentType);
		// step four add the part to the upload
		addPart(status.getUploadId());
		// Step five complete the upload
		MultipartUploadStatus finalStatus = completeUpload(status.getUploadId());
		// validate the results
		assertNotNull(finalStatus);
		assertEquals("1", finalStatus.getPartsState());
		assertEquals(MultipartUploadState.COMPLETED, finalStatus.getState());
		assertNotNull(finalStatus.getResultFileHandleId());
		fileHandlesToDelete.add(finalStatus.getResultFileHandleId());
	}

	/**
	 * Start the upload.
	 * @return
	 */
	private MultipartUploadStatus startUpload() {
		boolean forceRestart = true;
		MultipartUploadStatus status = multipartManagerV2
				.startOrResumeMultipartUpload(adminUserInfo, request,
						forceRestart);
		assertNotNull(status);
		assertNotNull(status.getUploadId());
		return status;
	}
	
	/**
	 * Get a pre-signed URL for the one part.
	 * @param uploadId
	 * @return
	 */
	private String getPresignedURLForPart(String uploadId, String contentType){
		BatchPresignedUploadUrlRequest batchURLRequest = new BatchPresignedUploadUrlRequest();
		batchURLRequest.setUploadId(uploadId);
		batchURLRequest.setContentType(contentType);
		Long partNumber = 1L;
		batchURLRequest.setPartNumbers(Lists.newArrayList(partNumber));
		BatchPresignedUploadUrlResponse bpuur = multipartManagerV2
				.getBatchPresignedUploadUrls(adminUserInfo, batchURLRequest);
		assertNotNull(bpuur);
		assertNotNull(bpuur.getPartPresignedUrls());
		assertEquals(1, bpuur.getPartPresignedUrls().size());
		PartPresignedUrl partUrl = bpuur.getPartPresignedUrls().get(0);
		return partUrl.getUploadPresignedUrl();
	}
	
	/**
	 * PUT the given types to the given URL.
	 * @param url
	 * @param toUpload
	 * @param contentType
	 * @throws Exception
	 */
	private void putStringToURL(String url, String toUpload, String contentType) throws Exception{
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(url);
		if(contentType != null){
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type", contentType);
			request.setHeaders(headers);
		}
		InputStream toPut = new StringInputStream(toUpload);
		SimpleHttpResponse response = simpleHttpClient.putToURL(request, toPut, toUpload.getBytes("UTF-8").length);
		assertEquals(200, response.getStatusCode());
	}
	
	/**
	 * Add the first part to the upload.
	 * @param uploadId
	 */
	private void addPart(String uploadId){
		int partNumber = 1;
		// Since this upload is one part, the part MD5 is the same as the file MD5.
		String partMD5Hex = fileMD5Hex;
		AddPartResponse response = multipartManagerV2.addMultipartPart(adminUserInfo, uploadId, partNumber,partMD5Hex);
		assertEquals(null, response.getErrorMessage());
		assertEquals(AddPartState.ADD_SUCCESS, response.getAddPartState());
	}
	
	/**
	 * Complete the multipart-upload.
	 * @param uploadId
	 * @return
	 */
	private MultipartUploadStatus completeUpload(String uploadId){
		return multipartManagerV2.completeMultipartUpload(adminUserInfo, uploadId);
	}

}
