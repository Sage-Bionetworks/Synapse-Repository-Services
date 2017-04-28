package org.sagebionetworks.repo.model.dbo.file;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
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
	
	Long userId;
	String hash;
	String uploadToken;
	String bucket;
	String key;
	Integer numberOfParts;
	CreateMultipartRequest createRequest;
	String requestJSON;
	S3FileHandle file;
	
	@After
	public void after(){
		if(file != null){
			fileHandleDao.delete(file.getId());;
		}
	}
	
	@Before
	public void before() throws JSONObjectAdapterException{
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		multipartUplaodDAO.truncateAll();
		
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
		bucket = "someBucket";
		key = "someKey";
		numberOfParts = 11;
		requestJSON = EntityFactory.createJSONStringForEntity(request);
		createRequest = new CreateMultipartRequest(userId, hash, requestJSON, uploadToken, bucket, key,numberOfParts);
	}

	
	@Test
	public void testCreate() throws JSONObjectAdapterException{
		// call under test
		CompositeMultipartUploadStatus composite = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(composite);
		MultipartUploadStatus status = composite.getMultipartUploadStatus();
		assertNotNull(status);
		assertNotNull(status.getUploadId());
		assertNotNull(status.getStartedOn());
		assertNotNull(status.getUpdatedOn());
		assertEquals(""+userId, status.getStartedBy());
		assertEquals(MultipartUploadState.UPLOADING, status.getState());
		assertEquals(null, status.getResultFileHandleId());
		assertEquals(uploadToken, composite.getUploadToken());
		assertEquals(bucket, composite.getBucket());
		assertEquals(key, composite.getKey());
		assertEquals(numberOfParts, composite.getNumberOfParts());
	}
	
	@Test
	public void testPLFM_3701(){
		CompositeMultipartUploadStatus composite = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(composite);
		assertEquals(null, composite.getMultipartUploadStatus().getResultFileHandleId());
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("id", composite.getMultipartUploadStatus().getUploadId());
		DBOMultipartUpload dbo = basicDao.getObjectByPrimaryKey(DBOMultipartUpload.class, param);
		assertNotNull(dbo);
		assertEquals(null, dbo.getFileHandleId());
		assertEquals(composite.getMultipartUploadStatus().getUploadId(), ""+dbo.getId());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateNull() throws JSONObjectAdapterException{
		// call under test
		multipartUplaodDAO.createUploadStatus(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateUserNull() throws JSONObjectAdapterException{
		createRequest.setUserId(null);
		// call under test
		multipartUplaodDAO.createUploadStatus(createRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateHashNull() throws JSONObjectAdapterException{
		createRequest.setHash(null);
		// call under test
		multipartUplaodDAO.createUploadStatus(createRequest);
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateRequestStringNull() throws JSONObjectAdapterException{
		createRequest.setRequestBody(null);
		// call under test
		multipartUplaodDAO.createUploadStatus(createRequest);
	}
	
	@Test
	public void testGetUploadStatusById(){
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		// call under tests
		CompositeMultipartUploadStatus fetched = multipartUplaodDAO.getUploadStatus(status.getMultipartUploadStatus().getUploadId());
		assertEquals(status, fetched);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUploadStatusByNull(){
		multipartUplaodDAO.getUploadStatus(null);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetUploadStatusByIdNotFound(){
		// call under tests
		multipartUplaodDAO.getUploadStatus("-1");
	}
	
	@Test
	public void testGetUploadStatusByUserIdAndHash(){
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		// call under tests
		CompositeMultipartUploadStatus fetched = multipartUplaodDAO.getUploadStatus(userId, hash);
		assertEquals(status, fetched);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUploadStatusByUserIdNullAndHash(){
		userId = null;
		multipartUplaodDAO.getUploadStatus(userId, hash);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUploadStatusByUserIdAndHashNull(){
		hash = null;
		multipartUplaodDAO.getUploadStatus(userId, hash);
	}
	
	@Test
	public void testGetUploadStatusByUserIdAndHashDoesNotExist(){
		hash = "unknownHash";
		// call under tests
		CompositeMultipartUploadStatus fetched = multipartUplaodDAO.getUploadStatus(userId, hash);
		assertEquals(null, fetched);
	}
	
	@Test
	public void testAddPartToAndErrorToUpload(){
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
	public void testDeleteUploadStatus(){
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		// call under test.
		multipartUplaodDAO.deleteUploadStatus(userId, createRequest.getHash());
		
		try {
			multipartUplaodDAO.getUploadRequest(uploadId);
			fail("Should no longer exist");
		} catch (NotFoundException e) {
			// expected
		}
	}
	
	@Test
	public void testAddPartUpdateEtag(){
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		// Call under test
		multipartUplaodDAO.addPartToUpload(uploadId, 1, "partOneMD5Hex");

		CompositeMultipartUploadStatus updated = multipartUplaodDAO.getUploadStatus(uploadId);
		assertNotNull(updated);
		assertNotNull(updated.getEtag());
		assertFalse("Adding a part must update the etag of the master row.",status.getEtag().equals(updated.getEtag()));
	}
	
	@Test
	public void testSetPartFailedUpdateEtag(){
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		// Call under test
		// also call under test
		multipartUplaodDAO.setPartToFailed(uploadId, 10, "some kind of error");

		CompositeMultipartUploadStatus updated = multipartUplaodDAO.getUploadStatus(uploadId);
		assertNotNull(updated);
		assertNotNull(updated.getEtag());
		assertFalse("setting part to failed must update the etag of the master row.",status.getEtag().equals(updated.getEtag()));
	}
	
	@Test
	public void testGetUploadRequest(){
		CompositeMultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		String uploadId = status.getMultipartUploadStatus().getUploadId();
		// call under test
		String requestBody = multipartUplaodDAO.getUploadRequest(uploadId);
		assertEquals(requestJSON, requestBody);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetUploadRequestNotFound(){
		//call under test.
		multipartUplaodDAO.getUploadRequest("-1");
	}
	
	@Test
	public void testSetUploadComplete(){
		// setup a file.
		S3FileHandle file = TestUtils.createS3FileHandle(userId.toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
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
		// call under test
		CompositeMultipartUploadStatus result = multipartUplaodDAO.setUploadComplete(uploadId, file.getId());
		assertNotNull(result);
		assertEquals(MultipartUploadState.COMPLETED, result.getMultipartUploadStatus().getState());
		assertEquals(file.getId(), result.getMultipartUploadStatus().getResultFileHandleId());
		// the etag must change
		assertFalse("Completing an upload must update the etag of the master row.",status.getEtag().equals(result.getEtag()));
		// the part state should be cleared
		partMD5s = multipartUplaodDAO.getAddedPartMD5s(uploadId);
		assertNotNull(partMD5s);
		assertEquals("Setting an upload complete should clear all part state.",0, partMD5s.size());
	}
	
}
