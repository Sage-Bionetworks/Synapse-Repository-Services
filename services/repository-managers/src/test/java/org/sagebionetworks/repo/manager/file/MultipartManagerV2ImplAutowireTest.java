package org.sagebionetworks.repo.manager.file;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.junit.Assume;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.AddPartState;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
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
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.Md5Utils;

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
	
	@Value("${dev-test-s3-bucket}")
	private String externalS3Bucket;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private SynapseS3Client s3Client;
	
	@Autowired
	private SynapseGoogleCloudStorageClient gcClient;
	
	@Autowired
	private StorageLocationDAO storageLocationDao;

	static SimpleHttpClient simpleHttpClient;

	private UserInfo adminUserInfo;
	
	ExternalS3StorageLocationSetting copyDestination;
	S3FileHandle copyFileHandle;

	ExternalGoogleCloudStorageLocationSetting googleCloudStorageLocationSetting;
	
	List<String> entitiesToDelete;
	List<CloudProviderFileHandleInterface> fileHandlesToDelete;
	List<Long> storageLocationsToDelete;
	
	@BeforeAll
	public static void beforeClass() {
		simpleHttpClient = new SimpleHttpClientImpl();
	}

	@BeforeEach
	public void before() throws Exception {
		entitiesToDelete = new ArrayList<>();
		fileHandlesToDelete = new ArrayList<>();
		storageLocationsToDelete = new ArrayList<>();
		
		multipartManagerV2.truncateAll();
		fileHandleDao.truncateTable();
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		String baseKey = "integration-test/MultipartManagerV2AutowiredTest-" + UUID.randomUUID().toString();
		String ownerTxtKey =  baseKey + "/owner.txt";
		byte[] ownerConent = principalAliasDao.getUserName(adminUserInfo.getId()).getBytes(StandardCharsets.UTF_8);
		
		copyDestination = new ExternalS3StorageLocationSetting();
		copyDestination.setBucket(externalS3Bucket);
		copyDestination.setBaseKey(baseKey);
		copyDestination.setUploadType(UploadType.S3);
		
		ObjectMetadata ownerMeta = new ObjectMetadata();
		
		String md5 = MD5ChecksumHelper.getMD5Checksum(ownerConent);
		String hexMd5 = BinaryUtils.toBase64(BinaryUtils.fromHex(md5));
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentMD5(hexMd5);
		meta.setContentLength(ownerConent.length);
		
		s3Client.putObject(externalS3Bucket, ownerTxtKey, new ByteArrayInputStream(ownerConent), ownerMeta);
		
		copyDestination = projectSettingsManager.createStorageLocationSetting(adminUserInfo, copyDestination);
		
		storageLocationsToDelete.add(copyDestination.getStorageLocationId());

		if (stackConfiguration.getGoogleCloudEnabled()) {

			googleCloudStorageClient.putObject(googleCloudBucket, ownerTxtKey, new ByteArrayInputStream(ownerConent));

			googleCloudStorageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
			googleCloudStorageLocationSetting.setBucket(googleCloudBucket);
			googleCloudStorageLocationSetting.setBaseKey(baseKey);
			googleCloudStorageLocationSetting.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
			googleCloudStorageLocationSetting = projectSettingsManager.createStorageLocationSetting(adminUserInfo, googleCloudStorageLocationSetting);
		
			storageLocationsToDelete.add(googleCloudStorageLocationSetting.getStorageLocationId());
		}
	}

	@AfterEach
	public void after() {

		for (String entityId : entitiesToDelete) {
			entityManager.deleteEntity(adminUserInfo, entityId);
		}
		
		for (CloudProviderFileHandleInterface fileHandle : fileHandlesToDelete) {
			if (fileHandle instanceof S3FileHandle) {
				s3Client.deleteObject(fileHandle.getBucketName(), fileHandle.getKey());
			} else if (fileHandle instanceof GoogleCloudFileHandle) {
				gcClient.deleteObject(fileHandle.getBucketName(), fileHandle.getKey());
			}	
		}
		
		multipartManagerV2.truncateAll();
		fileHandleDao.truncateTable();
		
		for (Long storageLocationId :  storageLocationsToDelete) {
			storageLocationDao.delete(storageLocationId);
		}
	}

	@Test
	public void testMultipartUpload() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		String fileContent = "This is the content of the file";
		Long storageLocationId = null;
		boolean useContentTypeForParts = false;
		
		doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
	}

	@Test
	public void testMultipartUploadWithContentType() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		String fileContent = "This is the content of the file";
		Long storageLocationId = null;
		boolean useContentTypeForParts = true;
		
		doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
	}
	
	@Test
	public void testMultipartUploadWithMultipleRestarts() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		String fileContent = "This is the content of the file";
		Long storageLocationId = null;
		boolean useContentTypeForParts = true;
		
		// First upload
		doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		// Second upload, it's forced restarted (same request, this should change the hash of the previous upload)
		doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		// Third upload, it's forced restarted again (same request, this should change the hash again of the previous upload)
		doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		
		List<String> uploads = multipartManagerV2.getUploadsOrderByUpdatedOn(10);
		
		assertEquals(3, uploads.size());
	}

	@Test
	public void testMultipartUploadGoogleCloud() throws Exception {
		Assume.assumeTrue(stackConfiguration.getGoogleCloudEnabled());
		
		String fileName = "foo.txt";
		String contentType = "application/octet-stream";
		String fileContent = "This is the content of the file";
		Long storageLocationId = googleCloudStorageLocationSetting.getStorageLocationId();
		boolean useContentTypeForParts = true;
		
		doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
	}
	
	@Test
	public void testGCMultipartUploadCompletedClear() throws Exception {
		Assume.assumeTrue(stackConfiguration.getGoogleCloudEnabled());
		
		String fileName = "foo.txt";
		String contentType = "application/octet-stream";
		String fileContent = "This is the content of the file";
		Long storageLocationId = googleCloudStorageLocationSetting.getStorageLocationId();
		boolean useContentTypeForParts = false;
		
		GoogleCloudFileHandle sourceFile = (GoogleCloudFileHandle) doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		
		List<String> ids = multipartManagerV2.getUploadsOrderByUpdatedOn(10);
		
		assertEquals(1, ids.size());
		
		String uploadId = ids.iterator().next();
		
		// Call under test
		multipartManagerV2.clearMultipartUpload(uploadId);
		
		// There shouldn't be any other now
		assertEquals(Collections.emptyList(), multipartManagerV2.getUploadsModifiedBefore(0, 10));

		// The file should still exist in GC
		googleCloudStorageClient.getObject(sourceFile.getBucketName(), sourceFile.getKey());
	}
	
	@Test
	public void testGCMultipartUploadUploadingClear() throws Exception {
		Assume.assumeTrue(stackConfiguration.getGoogleCloudEnabled());
		
		String fileName = "foo.txt";
		String contentType = "application/octet-stream";
		String fileContent = "This is the content of the file";
		Long storageLocationId = googleCloudStorageLocationSetting.getStorageLocationId();
		
		byte[] fileDataBytes = fileContent.getBytes(StandardCharsets.UTF_8);
		String fileMD5Hex = BinaryUtils.toHex(Md5Utils.computeMD5Hash(fileDataBytes));
		Long partSizeBytes = PartUtils.MIN_PART_SIZE_BYTES;
		
		// step one start the upload.
		MultipartUploadStatus status = startUpload(fileName, contentType, fileMD5Hex, Long.valueOf(fileDataBytes.length), storageLocationId, partSizeBytes);
	
		List<String> ids = multipartManagerV2.getUploadsOrderByUpdatedOn(10);
		
		assertEquals(Arrays.asList(status.getUploadId()), ids);
		
		String uploadId = ids.iterator().next();
		
		// Call under test
		multipartManagerV2.clearMultipartUpload(uploadId);
		
		// There shouldn't be any other now
		assertEquals(Collections.emptyList(), multipartManagerV2.getUploadsModifiedBefore(0, 10));
	}
	
	@Test
	public void testMultipartUploadCopyFromAtomicUpload() throws Exception {
		
		String userId = adminUserInfo.getId().toString();
		Date modifiedOn = new Date();
		
		String fileName = "foo.txt";
		ContentType contentType = ContentTypeUtil.TEXT_PLAIN_UTF8;
		byte[] fileContent = "contents".getBytes(StandardCharsets.UTF_8);
		String contentEncoding = null;

		// Creates a dummy file handle and entity for the copy
		S3FileHandle sourceFile = fileHandleManager.createFileFromByteArray(userId, modifiedOn, 
				fileContent, fileName, contentType, contentEncoding);
		
		FileEntity sourceEntity = doCreateEntity(sourceFile);
		
		doMultipartCopy(sourceEntity, copyDestination);
	}
	

	@Test
	public void testMultipartUploadCopyFromMultipartUpload() throws Exception {
		// The previous test used a source file that was uploaded using a single S3 upload (no multipart)
		// We verify that the copy works when the file was uploaded using a mutipart upload
		String fileName = "foo.txt";
		String contentType = "plain/text";
		String fileContent = "This is the content of the file";
		Long storageLocationId = null;
		boolean useContentTypeForParts = false;
		
		S3FileHandle sourceFile = (S3FileHandle) doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		
		FileEntity sourceEntity = doCreateEntity(sourceFile);
		
		doMultipartCopy(sourceEntity, copyDestination);
	}
	
	@Test
	public void testS3MultipartUploadCompletedClear() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		String fileContent = "This is the content of the file";
		Long storageLocationId = null;
		boolean useContentTypeForParts = false;
		
		S3FileHandle sourceFile = (S3FileHandle) doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
	
		List<String> ids = multipartManagerV2.getUploadsOrderByUpdatedOn(10);
		
		assertEquals(1, ids.size());
		
		String uploadId = ids.iterator().next();
		
		// Call under test
		multipartManagerV2.clearMultipartUpload(uploadId);
		
		// There shouldn't be any other now
		assertEquals(Collections.emptyList(), multipartManagerV2.getUploadsOrderByUpdatedOn(10));
		
		// The file should still exist in S3
		s3Client.getObjectMetadata(sourceFile.getBucketName(), sourceFile.getKey());
		
	}
	
	@Test
	public void testS3MultipartUploadUploadingClear() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		String fileContent = "This is the content of the file";
		Long storageLocationId = null;
		
		byte[] fileDataBytes = fileContent.getBytes(StandardCharsets.UTF_8);
		String fileMD5Hex = BinaryUtils.toHex(Md5Utils.computeMD5Hash(fileDataBytes));
		Long partSizeBytes = PartUtils.MIN_PART_SIZE_BYTES;
		
		// step one start the upload.
		MultipartUploadStatus status = startUpload(fileName, contentType, fileMD5Hex, Long.valueOf(fileDataBytes.length), storageLocationId, partSizeBytes);
	
		List<String> ids = multipartManagerV2.getUploadsOrderByUpdatedOn(10);
		
		assertEquals(Arrays.asList(status.getUploadId()), ids);
		
		String uploadId = ids.iterator().next();
		
		// Call under test
		multipartManagerV2.clearMultipartUpload(uploadId);
		
		// There shouldn't be any other now
		assertEquals(Collections.emptyList(), multipartManagerV2.getUploadsOrderByUpdatedOn(10));
		
	}
	
	@Test
	public void testS3MultipartCopyCompleteClear() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		String fileContent = "This is the content of the file";
		Long storageLocationId = null;
		boolean useContentTypeForParts = false;
		
		S3FileHandle sourceFile = (S3FileHandle) doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		
		FileEntity sourceEntity = doCreateEntity(sourceFile);
		
		S3FileHandle targetFile = (S3FileHandle) doMultipartCopy(sourceEntity, copyDestination);
	
		List<String> ids = multipartManagerV2.getUploadsOrderByUpdatedOn(10);
		
		// There are two multipart uploads now
		assertEquals(2, ids.size());
		
		// The last one is the copy
		String uploadId = ids.get(1);
		
		// Call under test
		multipartManagerV2.clearMultipartUpload(uploadId);
		
		// There should only be the original upload now
		assertEquals(Arrays.asList(ids.get(0)), multipartManagerV2.getUploadsOrderByUpdatedOn(10));
		
		// The source and target files should still exist in S3
		s3Client.getObjectMetadata(sourceFile.getBucketName(), sourceFile.getKey());
		s3Client.getObjectMetadata(targetFile.getBucketName(), targetFile.getKey());
	}
	
	@Test
	public void testS3MultipartCopyUploadingClear() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		String fileContent = "This is the content of the file";
		Long storageLocationId = null;
		boolean useContentTypeForParts = false;
		
		S3FileHandle sourceFile = (S3FileHandle) doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		
		FileEntity sourceEntity = doCreateEntity(sourceFile);
		
		FileHandleAssociation association = new FileHandleAssociation();
		
		association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		association.setAssociateObjectId(sourceEntity.getId());
		association.setFileHandleId(sourceEntity.getDataFileHandleId());
		
		// Starts the multipart copy
		MultipartUploadStatus status = startUploadCopy(association, copyDestination.getStorageLocationId(), PartUtils.MIN_PART_SIZE_BYTES);
	
		List<String> ids = multipartManagerV2.getUploadsOrderByUpdatedOn(10);
		
		// There are two multipart uploads now
		assertEquals(2, ids.size());
		assertEquals(status.getUploadId(), ids.get(1));
		
		// The last one is the copy
		String uploadId = ids.get(1);
		
		// Call under test
		multipartManagerV2.clearMultipartUpload(uploadId);
		
		// There should only be the original upload now
		assertEquals(Arrays.asList(ids.get(0)), multipartManagerV2.getUploadsOrderByUpdatedOn(10));
		
		// The source file should still exist in S3
		s3Client.getObjectMetadata(sourceFile.getBucketName(), sourceFile.getKey());
		
	}
	
	// The following tests can be enabled after we setup a VPC endpoint
	
	@Test
	@Disabled("This test uploads a large file, can be enabled once we have a VPC endpoint")
	public void testMultipartUploadCopyFromMultipartUploadWithMultipleParts() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		// A little bit more than one part
		String fileContent = RandomStringUtils.random((int) PartUtils.MIN_PART_SIZE_BYTES + 200, true, true);
		Long storageLocationId = null;
		boolean useContentTypeForParts = false;
		
		S3FileHandle sourceFile = (S3FileHandle) doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		
		FileEntity sourceEntity = doCreateEntity(sourceFile);
		
		doMultipartCopy(sourceEntity, copyDestination);
	}
	
	@Test
	@Disabled("This test uploads a large file, can be enabled once we have a VPC endpoint")
	public void testMultipartUploadCopyFromMultipartUploadWithMultiplePartsSinglePartCopy() throws Exception {
		String fileName = "foo.txt";
		String contentType = "plain/text";
		// A little bit more than one part
		String fileContent = RandomStringUtils.random((int) PartUtils.MIN_PART_SIZE_BYTES + 200, true, true);
		Long storageLocationId = null;
		boolean useContentTypeForParts = false;
		
		S3FileHandle sourceFile = (S3FileHandle) doMultipartUpload(fileName, contentType, fileContent, storageLocationId, useContentTypeForParts);
		
		FileEntity sourceEntity = doCreateEntity(sourceFile);
		
		// Uses the max part size to do a one shot copy
		doMultipartCopy(sourceEntity, copyDestination, PartUtils.MAX_PART_SIZE_BYTES);
	}
	
	private FileEntity doCreateEntity(CloudProviderFileHandleInterface fileHandle) {
		FileEntity fileEntity = new FileEntity();
		
		fileEntity.setDataFileHandleId(fileHandle.getId());

		fileEntity.setName("testFileEntity_" + UUID.randomUUID());
		
		String id = entityManager.createEntity(adminUserInfo, fileEntity, null);
		
		fileEntity.setId(id);
		
		entitiesToDelete.add(id);
		
		return fileEntity;
	}
	
	private CloudProviderFileHandleInterface doMultipartUpload(String fileName, String contentType, String contentString, Long storageLocationId, boolean contentTypeForParts) throws Exception {
		byte[] fileDataBytes = contentString.getBytes(StandardCharsets.UTF_8);
		String fileMD5Hex = BinaryUtils.toHex(Md5Utils.computeMD5Hash(fileDataBytes));
		Long partSizeBytes = PartUtils.MIN_PART_SIZE_BYTES;
		
		// step one start the upload.
		MultipartUploadStatus status = startUpload(fileName, contentType, fileMD5Hex, Long.valueOf(fileDataBytes.length), storageLocationId, partSizeBytes);
		
		int numberOfParts = status.getPartsState().length();
		
		int partStartBytePosition = 0;
		
		for (Long partNumber = 1L; partNumber <= numberOfParts; partNumber ++) {
			
			String partContentType = contentTypeForParts ? contentType : null;
			
			// step two get pre-signed URLs for the parts
			PartPresignedUrl preSignedUrl = getPresignedUrlForParts(status.getUploadId(), partContentType, Arrays.asList(partNumber)).get(0);

			int partEndBytePosition = (int) Math.min(partStartBytePosition + partSizeBytes, fileDataBytes.length);
			
			byte[] partData = Arrays.copyOfRange(fileDataBytes, partStartBytePosition, partEndBytePosition);
			
			String partMd5 = BinaryUtils.toHex(Md5Utils.computeMD5Hash(partData));
			
			// step three put the part to the URL
			putPartToURL(preSignedUrl.getUploadPresignedUrl(), partData, preSignedUrl.getSignedHeaders());
			
			// step four add the part to the upload
			addPartToMultipart(status.getUploadId(), partMd5, partNumber);
			
			partStartBytePosition += partSizeBytes;
		}
		
		// Step five complete the upload
		MultipartUploadStatus finalStatus = completeUpload(status.getUploadId());
		
		// validate the results
		assertNotNull(finalStatus);
		assertEquals(StringUtils.repeat('1', numberOfParts), finalStatus.getPartsState());
		assertEquals(MultipartUploadState.COMPLETED, finalStatus.getState());
		assertNotNull(finalStatus.getResultFileHandleId());

		CloudProviderFileHandleInterface fileHandle = (CloudProviderFileHandleInterface) fileHandleDao.get(finalStatus.getResultFileHandleId());
		
		fileHandlesToDelete.add(fileHandle);

		return fileHandle;
	}
	
	private CloudProviderFileHandleInterface doMultipartCopy(FileEntity sourceEntity, StorageLocationSetting destination) throws Exception {
		return doMultipartCopy(sourceEntity, destination, PartUtils.MIN_PART_SIZE_BYTES);
	}
	
	private CloudProviderFileHandleInterface doMultipartCopy(FileEntity sourceEntity, StorageLocationSetting destination, Long partSize) throws Exception {
		FileHandleAssociation association = new FileHandleAssociation();
		
		association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		association.setAssociateObjectId(sourceEntity.getId());
		association.setFileHandleId(sourceEntity.getDataFileHandleId());
		
		CloudProviderFileHandleInterface sourceFileHandle = (CloudProviderFileHandleInterface) fileHandleDao.get(sourceEntity.getDataFileHandleId());
		
		// Starts the multipart copy
		MultipartUploadStatus status = startUploadCopy(association, destination.getStorageLocationId(), partSize);
		
		int numerOfParts = status.getPartsState().length();
		
		for (Long partNumber = 1L; partNumber <= numerOfParts; partNumber++) {

			// Fetch the part pre-signed url
			PartPresignedUrl preSignedUrl = getPresignedUrlForParts(status.getUploadId(), null, Arrays.asList(partNumber)).get(0);
			
			// Make the request to S3
			String eTag = emptyPUT(preSignedUrl.getUploadPresignedUrl(), preSignedUrl.getSignedHeaders());
			
			// Confirm the added part
			addPartToMultipart(status.getUploadId(), eTag, partNumber);
		}
		
		// Complete the copy
		status = completeUpload(status.getUploadId());
		
		assertEquals(MultipartUploadState.COMPLETED, status.getState());
		
		CloudProviderFileHandleInterface newFileHandle = (CloudProviderFileHandleInterface) fileHandleDao.get(status.getResultFileHandleId());
		
		assertNotEquals(sourceFileHandle.getId(), newFileHandle.getId());
		assertNotEquals(sourceFileHandle.getStorageLocationId(), newFileHandle.getStorageLocationId());
		assertEquals(destination.getStorageLocationId(), newFileHandle.getStorageLocationId());
		assertEquals(sourceFileHandle.getFileName(), newFileHandle.getFileName());
		assertEquals(sourceFileHandle.getContentMd5(), newFileHandle.getContentMd5());
		assertEquals(sourceFileHandle.getContentSize(), newFileHandle.getContentSize());
		
		fileHandlesToDelete.add(newFileHandle);
		
		return newFileHandle;
	}
	
	
	/**
	 * Start the upload.
	 * @return
	 */
	private MultipartUploadStatus startUpload(String fileName, String contentType, String contentMd5, Long fileSize, Long storageLocationId, Long partSizeBytes) {
		MultipartUploadRequest request = new MultipartUploadRequest();
		
		request.setContentMD5Hex(contentMd5);
		request.setContentType(contentType);
		request.setFileName(fileName);
		request.setFileSizeBytes(fileSize);
		request.setPartSizeBytes(partSizeBytes);
		request.setStorageLocationId(storageLocationId);
		
		boolean forceRestart = true;
		
		MultipartUploadStatus status = multipartManagerV2.startOrResumeMultipartUpload(adminUserInfo, request, forceRestart);
		assertNotNull(status);
		assertNotNull(status.getUploadId());
		return status;
	}
	
	private MultipartUploadStatus startUploadCopy(FileHandleAssociation association, Long storageLocationId, Long partSizeBytes) {
		
		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();
		
		request.setSourceFileHandleAssociation(association);
		request.setStorageLocationId(storageLocationId);
		request.setPartSizeBytes(partSizeBytes);
		
		boolean forceRestart = true;
		MultipartUploadStatus status = multipartManagerV2.startOrResumeMultipartUploadCopy(adminUserInfo, request, forceRestart);
		assertNotNull(status);
		assertNotNull(status.getUploadId());
		return status;
	}
	
	private List<PartPresignedUrl> getPresignedUrlForParts(String uploadId, String contentType, List<Long> partNumbers) {
		BatchPresignedUploadUrlRequest batchURLRequest = new BatchPresignedUploadUrlRequest();
		batchURLRequest.setUploadId(uploadId);
		batchURLRequest.setContentType(contentType);
		batchURLRequest.setPartNumbers(partNumbers);
		BatchPresignedUploadUrlResponse bpuur = multipartManagerV2.getBatchPresignedUploadUrls(adminUserInfo, batchURLRequest);
		return bpuur.getPartPresignedUrls();
	}
	
	/**
	 * PUT the given types to the given URL.
	 * @param url
	 * @param toUpload
	 * @param contentType
	 * @throws Exception
	 */
	private void putPartToURL(String url, byte[] partData, Map<String, String> headers) throws Exception{
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(url);
		request.setHeaders(headers);
		InputStream toPut = new ByteArrayInputStream(partData);
		SimpleHttpResponse response = simpleHttpClient.putToURL(request, toPut, partData.length);
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
	
	private void addPartToMultipart(String uploadId, String partMD5Hex, Long partNumber) {
		AddPartResponse response = multipartManagerV2.addMultipartPart(adminUserInfo, uploadId, partNumber.intValue(), partMD5Hex);
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
