package org.sagebionetworks.repo.manager.file;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assume;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.AddPartState;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.Md5Utils;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MultipartManagerV2ImplAutowireTest {

	private static final Pattern ETAG_PATTERN = Pattern.compile("<ETag>&quot;(.+)&quot;</ETag>");
	
	@Autowired
	StackConfiguration stackConfiguration;

	@Autowired
	MultipartManagerV2 multipartManagerV2;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private PrincipalAliasDAO principalAliasDao;

	@Autowired
	private SynapseGoogleCloudStorageClient googleCloudStorageClient;

	@Value("${dev-googlecloud-bucket}")
	private String googleCloudBucket;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private SynapseS3Client s3Client;

	static SimpleHttpClient simpleHttpClient;

	private UserInfo adminUserInfo;
	private List<String> fileHandlesToDelete;

	MultipartUploadRequest request;
	String fileDataString;
	byte[] fileDataBytes;
	String fileMD5Hex;
	
	S3FileHandle sourceFileHandle;
	FileEntity sourceEntity;
	StorageLocationSetting destination;
	S3FileHandle copyFileHandle;

	ExternalGoogleCloudStorageLocationSetting googleCloudStorageLocationSetting;

	@BeforeAll
	public static void beforeClass() {
		simpleHttpClient = new SimpleHttpClientImpl();
	}

	@BeforeEach
	public void before() throws Exception {
		multipartManagerV2.truncateAll();
		fileHandleDao.truncateTable();
		
		// used to put data to a pre-signed url.
		fileHandlesToDelete = new LinkedList<>();
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
		
		// Creates a dummy file handle and entity for the copy
		sourceFileHandle = fileHandleManager.createFileFromByteArray(adminUserInfo.getId().toString(), new Date(), "contents".getBytes(StandardCharsets.UTF_8), "foo.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		
		sourceEntity = new FileEntity();
		
		sourceEntity.setDataFileHandleId(sourceFileHandle.getId());

		sourceEntity.setName("testFileEntity");
		
		String id = entityManager.createEntity(adminUserInfo, sourceEntity, null);
		
		sourceEntity.setId(id);
		
		destination = new S3StorageLocationSetting();
		
		destination = projectSettingsManager.createStorageLocationSetting(adminUserInfo, destination);

		if (stackConfiguration.getGoogleCloudEnabled()) {
			// Create the owner.txt on the bucket
			String baseKey = "integration-test/MultipartManagerV2AutowiredTest-" + UUID.randomUUID().toString();
			googleCloudStorageClient.putObject(googleCloudBucket, baseKey + "/owner.txt", new ByteArrayInputStream(principalAliasDao.getUserName(adminUserInfo.getId()).getBytes(StandardCharsets.UTF_8)));

			googleCloudStorageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
			googleCloudStorageLocationSetting.setBucket(googleCloudBucket);
			googleCloudStorageLocationSetting.setBaseKey(baseKey);
			googleCloudStorageLocationSetting.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
			googleCloudStorageLocationSetting = projectSettingsManager.createStorageLocationSetting(adminUserInfo, googleCloudStorageLocationSetting);
		}
	}

	@AfterEach
	public void after() {
		
		entityManager.deleteEntity(adminUserInfo, sourceEntity.getId());
		
		// delete the file from S3.
		if (sourceFileHandle != null) {
			s3Client.deleteObject(sourceFileHandle.getBucketName(), sourceFileHandle.getKey());
		}
		
		if (copyFileHandle != null) {
			s3Client.deleteObject(copyFileHandle.getBucketName(), copyFileHandle.getKey());
		}
		
		multipartManagerV2.truncateAll();
		fileHandleDao.truncateTable();
		
		if (stackConfiguration.getGoogleCloudEnabled()) {
			try {
				projectSettingsManager.deleteProjectSetting(adminUserInfo, googleCloudStorageLocationSetting.getStorageLocationId().toString());
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testMultipartUpload() throws Exception {
		// step one start the upload.
		MultipartUploadStatus status = startUpload();
		String contentType = null;
		// step two get pre-signed URLs for the parts
		String preSignedUrl = getPresignedURLForPart(status.getUploadId(), contentType).getUploadPresignedUrl();
		// step three put the part to the URL
		putStringToURL(preSignedUrl, fileDataString, contentType);
		// step four add the part to the upload
		addPart(status.getUploadId(), fileMD5Hex);
		// Step five complete the upload
		MultipartUploadStatus finalStatus = completeUpload(status.getUploadId());
		// validate the results
		assertNotNull(finalStatus);
		assertEquals("1", finalStatus.getPartsState());
		assertEquals(MultipartUploadState.COMPLETED, finalStatus.getState());
		assertNotNull(finalStatus.getResultFileHandleId());
		fileHandlesToDelete.add(finalStatus.getResultFileHandleId());
	}

	@Test
	public void testMultipartUploadGoogleCloud() throws Exception {
		Assume.assumeTrue(stackConfiguration.getGoogleCloudEnabled());
		request.setStorageLocationId(googleCloudStorageLocationSetting.getStorageLocationId());
		// step one start the upload.
		MultipartUploadStatus status = startUpload();
		String contentType = "application/octet-stream";
		// step two get pre-signed URLs for the parts
		String preSignedUrl = getPresignedURLForPart(status.getUploadId(), contentType).getUploadPresignedUrl();
		// step three put the part to the URL
		putStringToURL(preSignedUrl, fileDataString, contentType);
		// step four add the part to the upload
		addPart(status.getUploadId(), fileMD5Hex);
		// Step five complete the upload
		MultipartUploadStatus finalStatus = completeUpload(status.getUploadId());
		// validate the results
		assertNotNull(finalStatus);
		assertEquals("1", finalStatus.getPartsState());
		assertEquals(MultipartUploadState.COMPLETED, finalStatus.getState());
		assertNotNull(finalStatus.getResultFileHandleId());
		fileHandlesToDelete.add(finalStatus.getResultFileHandleId());

	}

	@Test
	public void testMultipartUploadWithContentType() throws Exception {
		// step one start the upload.
		MultipartUploadStatus status = startUpload();
		String contentType = "application/octet-stream";
		// step two get pre-signed URLs for the parts
		String preSignedUrl = getPresignedURLForPart(status.getUploadId(), contentType).getUploadPresignedUrl();
		// step three put the part to the URL
		putStringToURL(preSignedUrl, fileDataString, contentType);
		// step four add the part to the upload
		addPart(status.getUploadId(), fileMD5Hex);
		// Step five complete the upload
		MultipartUploadStatus finalStatus = completeUpload(status.getUploadId());
		// validate the results
		assertNotNull(finalStatus);
		assertEquals("1", finalStatus.getPartsState());
		assertEquals(MultipartUploadState.COMPLETED, finalStatus.getState());
		assertNotNull(finalStatus.getResultFileHandleId());
		fileHandlesToDelete.add(finalStatus.getResultFileHandleId());
	}
	
	@Test
	public void testMultipartUploadCopy() throws Exception {
		FileHandleAssociation association = new FileHandleAssociation();
		
		association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		association.setAssociateObjectId(sourceEntity.getId());
		association.setFileHandleId(sourceFileHandle.getId());
		
		// Starts the multipart copy
		MultipartUploadStatus status = startUploadCopy(association, destination.getStorageLocationId());
		
		// Fetch the part pre-signed url
		PartPresignedUrl preSignedUrl = getPresignedURLForPart(status.getUploadId(), null);
		
		// Make the request to S3
		String eTag = emptyPUT(preSignedUrl.getUploadPresignedUrl(), preSignedUrl.getSignedHeaders());
		
		// Add the part
		addPart(status.getUploadId(), eTag);
		
		// Complete the copy
		status = completeUpload(status.getUploadId());
		
		assertEquals(MultipartUploadState.COMPLETED, status.getState());
		
		copyFileHandle = (S3FileHandle) fileHandleDao.get(status.getResultFileHandleId());
		
		assertNotEquals(sourceFileHandle.getId(), copyFileHandle.getId());
		assertEquals(sourceFileHandle.getFileName(), copyFileHandle.getFileName());
		assertEquals(sourceFileHandle.getContentMd5(), copyFileHandle.getContentMd5());
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
	
	private MultipartUploadStatus startUploadCopy(FileHandleAssociation association, Long storageLocationId) {
		
		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();
		
		request.setSourceFileHandleAssociation(association);
		request.setStorageLocationId(storageLocationId);
		request.setPartSizeBytes(PartUtils.MIN_PART_SIZE_BYTES);
		
		boolean forceRestart = true;
		MultipartUploadStatus status = multipartManagerV2.startOrResumeMultipartUploadCopy(adminUserInfo, request, forceRestart);
		assertNotNull(status);
		assertNotNull(status.getUploadId());
		return status;
	}
	
	private PartPresignedUrl getPresignedURLForPart(String uploadId, String contentType){
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
		return bpuur.getPartPresignedUrls().get(0);
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
	
	private String emptyPUT(String url, Map<String, String> headers) throws Exception {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(url);
		request.setHeaders(headers);
		
		SimpleHttpResponse response = simpleHttpClient.put(request, null);
		
		assertEquals(200, response.getStatusCode());
		
		Matcher matcher = ETAG_PATTERN.matcher(response.getContent());
		
		if (matcher.find()) {			
			return matcher.group(1);
		}
		
		throw new IllegalStateException("Could not extract ETag value");
	}
	
	/**
	 * Add the first part to the upload.
	 * @param uploadId
	 */
	private void addPart(String uploadId, String partMD5Hex){
		int partNumber = 1;
		AddPartResponse response = multipartManagerV2.addMultipartPart(adminUserInfo, uploadId, partNumber, partMD5Hex);
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
