package org.sagebionetworks.repo.model.dbo.file;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.Multipart.State;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MultipartUploadDAOImplTest {

	@Autowired
	MultipartUploadDAO multipartUplaodDAO;
	
	Long userId;
	String hash;
	String uploadToken;
	String bucket;
	String key;
	Integer numberOfParts;
	CreateMultipartRequest createRequest;
	
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
		
		uploadToken = "someUploadToken";
		bucket = "someBucket";
		key = "someKey";
		numberOfParts = 11;
		String requestJSON = EntityFactory.createJSONStringForEntity(request);
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
		assertEquals(State.UPLOADING, status.getState());
		assertEquals(null, status.getResultFileHandleId());
		assertEquals(uploadToken, composite.getUploadToken());
		assertEquals(bucket, composite.getBucket());
		assertEquals(key, composite.getKey());
		assertEquals(numberOfParts, composite.getNumberOfParts());
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
	
}
