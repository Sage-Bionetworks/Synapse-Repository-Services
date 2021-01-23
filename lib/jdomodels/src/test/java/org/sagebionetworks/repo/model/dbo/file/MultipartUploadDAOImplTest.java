package org.sagebionetworks.repo.model.dbo.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartErrors;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.helper.MultipartUploadDBOHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MultipartUploadDAOImplTest {

	@Autowired
	MultipartUploadDAO multipartUplaodDAO;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private MultipartUploadDBOHelper helper;

	Long userId;
	String hash;
	String uploadToken;
	UploadType uploadType;
	String bucket;
	String key;
	Integer numberOfParts;
	CreateMultipartRequest createRequest;
	String requestJSON;

	@BeforeEach
	public void before() throws JSONObjectAdapterException {

		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

		multipartUplaodDAO.truncateAll();
		fileHandleDao.truncateTable();

		hash = "someHash";
		Long storageLocationId = null;
		MultipartUploadRequest request = new MultipartUploadRequest();
		request.setFileName("foo.txt");
		request.setFileSizeBytes(123L);
		request.setContentMD5Hex("someMD5Hex");
		request.setContentType("plain/text");
		request.setPartSizeBytes(5L);
		request.setStorageLocationId(storageLocationId);
		// Upload tokens can be large
		uploadToken = "n0LRZqFh9zvMZAIY_PInQBJdKxrqRwbOa8W4JJ.X1DiiqI8bJsh.LOWMAENIemWgfLNwCrs0J2xYiIPcKvK6uW9igwCoaYWgSrkLuwIbaJ6au85CYlCGfK8oUcDByxiI";
		uploadType = UploadType.S3;
		bucket = "someBucket";
		key = "someKey";
		numberOfParts = 11;
		requestJSON = EntityFactory.createJSONStringForEntity(request);

		createRequest = new CreateMultipartRequest(userId, hash, requestJSON, uploadToken, uploadType, bucket, key,
				numberOfParts, request.getPartSizeBytes());
	}
	
	@AfterEach
	public void after() {
		multipartUplaodDAO.truncateAll();
		fileHandleDao.truncateTable();
	}


	@Test
	public void testCreate() throws JSONObjectAdapterException {
		// call under test
		CompositeMultipartUploadStatus composite = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(composite);
		MultipartUploadStatus status = composite.getMultipartUploadStatus();
		assertNotNull(status);
		assertNotNull(status.getUploadId());
		assertNotNull(status.getStartedOn());
		assertNotNull(status.getUpdatedOn());
		assertEquals("" + userId, status.getStartedBy());
		assertEquals(MultipartUploadState.UPLOADING, status.getState());
		assertEquals(null, status.getResultFileHandleId());
		assertEquals(uploadToken, composite.getUploadToken());
		assertEquals(bucket, composite.getBucket());
		assertEquals(key, composite.getKey());
		assertEquals(numberOfParts, composite.getNumberOfParts());
		assertEquals(MultiPartRequestType.UPLOAD, composite.getRequestType());
		assertEquals(createRequest.getPartSize(), composite.getPartSize());
		assertNull(composite.getSourceFileHandleId());
		assertNull(composite.getSourceFileEtag());
		assertNull(composite.getSourceFileSize());
		assertNull(composite.getSourceBucket());
		assertNull(composite.getSourceKey());
	}
	
	@Test
	public void testCreateWithSourceFileHandleId() throws JSONObjectAdapterException {
		S3FileHandle file = TestUtils.createS3FileHandle(userId.toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		file = (S3FileHandle) fileHandleDao.createFile(file);
		
		String fileEtag = "s3Etag";
		
		createRequest.setSourceFileHandleId(file.getId());
		createRequest.setSourceFileEtag(fileEtag);
		
		// call under test
		CompositeMultipartUploadStatus composite = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(composite);
		MultipartUploadStatus status = composite.getMultipartUploadStatus();
		assertNotNull(status);
		assertNotNull(status.getUploadId());
		assertNotNull(status.getStartedOn());
		assertNotNull(status.getUpdatedOn());
		assertEquals("" + userId, status.getStartedBy());
		assertEquals(MultipartUploadState.UPLOADING, status.getState());
		assertEquals(null, status.getResultFileHandleId());
		assertEquals(uploadToken, composite.getUploadToken());
		assertEquals(bucket, composite.getBucket());
		assertEquals(key, composite.getKey());
		assertEquals(numberOfParts, composite.getNumberOfParts());
		assertEquals(MultiPartRequestType.COPY, composite.getRequestType());
		assertEquals(createRequest.getPartSize(), composite.getPartSize());
		assertEquals(Long.valueOf(createRequest.getSourceFileHandleId()), composite.getSourceFileHandleId());
		assertEquals(createRequest.getSourceFileEtag(), composite.getSourceFileEtag());
		assertEquals(file.getBucketName(), composite.getSourceBucket());
		assertEquals(file.getKey(), composite.getSourceKey());
		assertEquals(file.getContentSize(), composite.getSourceFileSize());
	}

	@Test
	public void testPLFM_3701() {
		CompositeMultipartUploadStatus composite = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(composite);
		assertEquals(null, composite.getMultipartUploadStatus().getResultFileHandleId());
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("id", composite.getMultipartUploadStatus().getUploadId());
		DBOMultipartUpload dbo = basicDao.getObjectByPrimaryKey(DBOMultipartUpload.class, param);
		assertNotNull(dbo);
		assertEquals(null, dbo.getFileHandleId());
		assertEquals(composite.getMultipartUploadStatus().getUploadId(), "" + dbo.getId());
	}

	@Test
	public void testCreateNull() throws JSONObjectAdapterException {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			multipartUplaodDAO.createUploadStatus(null);
		});
	}

	@Test
	public void testCreateUserNull() throws JSONObjectAdapterException {
		createRequest.setUserId(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			multipartUplaodDAO.createUploadStatus(createRequest);
		});
	}

	@Test
	public void testCreateHashNull() throws JSONObjectAdapterException {
		createRequest.setHash(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			multipartUplaodDAO.createUploadStatus(createRequest);
		});
	}

	@Test
	public void testCreateRequestStringNull() throws JSONObjectAdapterException {
		createRequest.setRequestBody(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			multipartUplaodDAO.createUploadStatus(createRequest);
		});
	}
	
	@Test
	public void testCreateRequestWithNoPartSize() throws JSONObjectAdapterException {
		createRequest.setPartSize(null);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			multipartUplaodDAO.createUploadStatus(createRequest);
		}).getMessage();
		
		assertEquals("partSize is required.", errorMessage);
	}

	@Test
	public void testGetUploadStatusById() {
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		// call under tests
		CompositeMultipartUploadStatus fetched = multipartUplaodDAO
				.getUploadStatus(status.getMultipartUploadStatus().getUploadId());
		assertEquals(status, fetched);
	}

	@Test
	public void testGetUploadStatusByNull() {

		assertThrows(IllegalArgumentException.class, () -> {
			multipartUplaodDAO.getUploadStatus(null);
		});
	}

	@Test
	public void testGetUploadStatusByIdNotFound() {
		assertThrows(NotFoundException.class, () -> {
			// call under tests
			multipartUplaodDAO.getUploadStatus("-1");
		});
	}

	@Test
	public void testGetUploadStatusByUserIdAndHash() {
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		// call under tests
		CompositeMultipartUploadStatus fetched = multipartUplaodDAO.getUploadStatus(userId, hash);
		assertEquals(status, fetched);
	}

	@Test
	public void testGetUploadStatusByUserIdNullAndHash() {
		userId = null;

		assertThrows(IllegalArgumentException.class, () -> {
			multipartUplaodDAO.getUploadStatus(userId, hash);
		});
	}

	@Test
	public void testGetUploadStatusByUserIdAndHashNull() {
		hash = null;
		assertThrows(IllegalArgumentException.class, () -> {
			multipartUplaodDAO.getUploadStatus(userId, hash);
		});
	}

	@Test
	public void testGetUploadStatusByUserIdAndHashDoesNotExist() {
		hash = "unknownHash";
		// call under tests
		CompositeMultipartUploadStatus fetched = multipartUplaodDAO.getUploadStatus(userId, hash);
		assertEquals(null, fetched);
	}

	@Test
	public void testAddPartToAndErrorToUpload() {
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		// Should have not md5s since none have been added
		List<PartMD5> partMD5s = multipartUplaodDAO.getAddedPartMD5s(uploadId);
		assertNotNull(partMD5s);
		assertEquals(0, partMD5s.size());
		// Call under test
		multipartUplaodDAO.addPartToUpload(uploadId, 1, "partOneMD5Hex");
		multipartUplaodDAO.addPartToUpload(uploadId, 9, "partNineMD5Hex");
		// also call under test
		multipartUplaodDAO.setPartToFailed(uploadId, 10, "some kind of error");
		// both should be added.
		partMD5s = multipartUplaodDAO.getAddedPartMD5s(uploadId);
		assertNotNull(partMD5s);
		assertEquals(2, partMD5s.size());
		assertEquals(new PartMD5(1, "partOneMD5Hex"), partMD5s.get(0));
		assertEquals(new PartMD5(9, "partNineMD5Hex"), partMD5s.get(1));
		// Get the errors
		List<PartErrors> partErrors = multipartUplaodDAO.getPartErrors(uploadId);
		assertNotNull(partErrors);
		assertEquals(1, partErrors.size());
		assertEquals(new PartErrors(10, "some kind of error"), partErrors.get(0));

		String partsState = multipartUplaodDAO.getPartsState(uploadId, numberOfParts);
		assertEquals("10000000100", partsState);
	}
	
	@Test
	public void updateUploadStatusHash() throws InterruptedException {
		multipartUplaodDAO.createUploadStatus(createRequest);
		
		CompositeMultipartUploadStatus status = multipartUplaodDAO.getUploadStatus(userId, hash);
		
		String newHash = "Updated_" + hash;
		
		// Sleep for a second to allow the updated on to change 
		Thread.sleep(1000);
		
		// Call under test
		multipartUplaodDAO.setUploadStatusHash(userId, hash, newHash);
				
		assertNull(multipartUplaodDAO.getUploadStatus(userId, hash));
		
		CompositeMultipartUploadStatus newStatus = multipartUplaodDAO.getUploadStatus(userId, newHash);
		
		assertNotEquals(status.getEtag(), newStatus.getEtag());
		assertTrue(newStatus.getMultipartUploadStatus().getUpdatedOn().after(status.getMultipartUploadStatus().getUpdatedOn()));
		
		// Sync the updated data
		status.setEtag(newStatus.getEtag());
		status.getMultipartUploadStatus().setUpdatedOn(newStatus.getMultipartUploadStatus().getUpdatedOn());
		
		assertEquals(status, newStatus);
		
	}

	@Test
	public void testAddPartUpdateEtag() throws InterruptedException {
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		
		// Sleep for a second to allow the update of the updated on
		Thread.sleep(1000);
		
		// Call under test
		multipartUplaodDAO.addPartToUpload(uploadId, 1, "partOneMD5Hex");

		CompositeMultipartUploadStatus updated = multipartUplaodDAO.getUploadStatus(uploadId);
		
		assertNotEquals(status.getEtag(), updated.getEtag(), "Adding a part must update the etag of the master row.");
		assertTrue(updated.getMultipartUploadStatus().getUpdatedOn().after(status.getMultipartUploadStatus().getUpdatedOn()));
	}

	@Test
	public void testSetPartFailedUpdateEtag() throws InterruptedException {
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		
		// Sleep for a second to allow the update of the updated on
		Thread.sleep(1000);
		
		// Call under test
		multipartUplaodDAO.setPartToFailed(uploadId, 10, "some kind of error");

		CompositeMultipartUploadStatus updated = multipartUplaodDAO.getUploadStatus(uploadId);

		assertNotEquals(status.getEtag(), updated.getEtag(), "Adding a part must update the etag of the master row.");
		assertTrue(updated.getMultipartUploadStatus().getUpdatedOn().after(status.getMultipartUploadStatus().getUpdatedOn()));
	}

	@Test
	public void testGetUploadRequest() {
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		// call under test
		String requestBody = multipartUplaodDAO.getUploadRequest(uploadId);
		assertEquals(requestJSON, requestBody);
	}

	@Test
	public void testGetUploadRequestNotFound() {
		assertThrows(NotFoundException.class, () -> {
			// call under test.
			multipartUplaodDAO.getUploadRequest("-1");
		});
	}

	@Test
	public void testSetUploadComplete() throws InterruptedException {
		// setup a file.
		S3FileHandle file = TestUtils.createS3FileHandle(userId.toString(),
				idGenerator.generateNewId(IdType.FILE_IDS).toString());
		file = (S3FileHandle) fileHandleDao.createFile(file);

		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		assertEquals(MultipartUploadState.UPLOADING, status.getMultipartUploadStatus().getState());
		String uploadId = status.getMultipartUploadStatus().getUploadId();

		// Add a part so we can confirm the are removed upon completion.
		int partNumber = 1;
		multipartUplaodDAO.addPartToUpload(uploadId, partNumber, "partOneMD5Hex");
		List<PartMD5> partMD5s = multipartUplaodDAO.getAddedPartMD5s(uploadId);
		assertNotNull(partMD5s);
		assertEquals(1, partMD5s.size());
		
		// Sleep for a second to allow the update of the updated on
		Thread.sleep(1000);
		
		// call under test
		CompositeMultipartUploadStatus result = multipartUplaodDAO.setUploadComplete(uploadId, file.getId());
		assertNotNull(result);
		assertEquals(MultipartUploadState.COMPLETED, result.getMultipartUploadStatus().getState());
		assertEquals(file.getId(), result.getMultipartUploadStatus().getResultFileHandleId());
		// the etag must change
		assertNotEquals(status.getEtag(), result.getEtag(), "Adding a part must update the etag of the master row.");
		assertTrue(result.getMultipartUploadStatus().getUpdatedOn().after(status.getMultipartUploadStatus().getUpdatedOn()));
		// the part state should be cleared
		partMD5s = multipartUplaodDAO.getAddedPartMD5s(uploadId);
		assertNotNull(partMD5s);
		assertEquals(0, partMD5s.size(), "Setting an upload complete should clear all part state.");
	}
	
	@Test
	public void testGetUploadsModifedBefore() throws InterruptedException {
		
		Instant created = Instant.now().minus(10, ChronoUnit.DAYS);
		
		String upload1 = helper.create( dbo -> {
			dbo.setRequestHash("someHash");
			dbo.setStartedOn(Date.from(created));
			dbo.setUpdatedOn(Date.from(created));
		}).getId().toString();
		
		String upload2 = helper.create( dbo -> {
			dbo.setRequestHash("anotherHash");
			dbo.setStartedOn(Date.from(created));
			dbo.setUpdatedOn(Date.from(created.plus(1, ChronoUnit.DAYS)));
		}).getId().toString();
		
		List<String> expected = Arrays.asList(upload1, upload2);
		
		int numberOfdays = 1;
		long batchSize = 10;
		
		// Call under test
		List<String> result = multipartUplaodDAO.getUploadsModifiedBefore(numberOfdays, batchSize);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGetUploadsModifedBeforeBatchSize() throws InterruptedException {
		Instant created = Instant.now().minus(10, ChronoUnit.DAYS);
		
		String upload1 = helper.create( dbo -> {
			dbo.setRequestHash("someHash");
			dbo.setStartedOn(Date.from(created));
			dbo.setUpdatedOn(Date.from(created));
		}).getId().toString();
		
		helper.create( dbo -> {
			dbo.setRequestHash("anotherHash");
			dbo.setStartedOn(Date.from(created));
			dbo.setUpdatedOn(Date.from(created.plus(1, ChronoUnit.DAYS)));
		});
		
		List<String> expected = Arrays.asList(upload1);
		
		int numberOfdays = 1;
		long batchSize = 1;
		
		// Call under test
		List<String> result = multipartUplaodDAO.getUploadsModifiedBefore(numberOfdays, batchSize);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGetUploadsModifedBeforeDays() throws InterruptedException {
		Instant created = Instant.now().minus(10, ChronoUnit.DAYS);
		
		String upload1 = helper.create( dbo -> {
			dbo.setRequestHash("someHash");
			dbo.setStartedOn(Date.from(created));
			dbo.setUpdatedOn(Date.from(created));
		}).getId().toString();
		
		helper.create( dbo -> {
			dbo.setRequestHash("anotherHash");
			dbo.setStartedOn(Date.from(created));
			dbo.setUpdatedOn(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
		});
		
		List<String> expected = Arrays.asList(upload1);
		
		int numberOfdays = 2;
		long batchSize = 10;
		
		// Call under test
		List<String> result = multipartUplaodDAO.getUploadsModifiedBefore(numberOfdays, batchSize);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGetUploadsModifedBeforeZeroDays() throws InterruptedException {
		Instant created = Instant.now().minus(10, ChronoUnit.DAYS);
		
		String upload1 = helper.create( dbo -> {
			dbo.setRequestHash("someHash");
			dbo.setStartedOn(Date.from(created));
			dbo.setUpdatedOn(Date.from(created));
		}).getId().toString();
		
		String upload2 = helper.create( dbo -> {
			dbo.setRequestHash("anotherHash");
			dbo.setStartedOn(Date.from(created));
			dbo.setUpdatedOn(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)));
		}).getId().toString();
		
		List<String> expected = Arrays.asList(upload1, upload2);
		
		int numberOfdays = 0;
		long batchSize = 10;
		
		// Call under test
		List<String> result = multipartUplaodDAO.getUploadsModifiedBefore(numberOfdays, batchSize);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testDeleteUploadStatus() {
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		
		// Call under test
		multipartUplaodDAO.deleteUploadStatus(uploadId);
		
		assertThrows(NotFoundException.class, () -> {
			multipartUplaodDAO.getUploadStatus(uploadId);
		});
	}

}
