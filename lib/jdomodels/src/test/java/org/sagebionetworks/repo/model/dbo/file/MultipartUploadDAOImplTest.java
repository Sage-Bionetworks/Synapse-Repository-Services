package org.sagebionetworks.repo.model.dbo.file;

import static org.junit.Assert.*;

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
	CreateMultipartRequest createRequest;
	
	@Before
	public void before() throws JSONObjectAdapterException{
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		multipartUplaodDAO.truncateAll();
		
		hash = "someHash";
		Long storageLocationId = null;
		String storageLocationToken = "locationToken";
		MultipartUploadRequest request = new MultipartUploadRequest();
		request.setFileName("foo.txt");
		request.setFileSizeBytes(123L);
		request.setMd5Hex("someMD5Hex");
		request.setPartSizeBytes(5L);
		request.setStorageLocationId(storageLocationId);
		String requestJSON = EntityFactory.createJSONStringForEntity(request);
		createRequest = new CreateMultipartRequest(userId, hash, requestJSON, storageLocationId, storageLocationToken);
	}

	
	@Test
	public void testCreate() throws JSONObjectAdapterException{
		// call under test
		MultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		assertNotNull(status.getUploadId());
		assertNotNull(status.getStartedOn());
		assertNotNull(status.getUpdatedOn());
		assertEquals(""+userId, status.getStartedBy());
		assertEquals(createRequest.getStorageLocationToken(), status.getStorageLocationToken());
		assertEquals(State.UPLOADING, status.getState());
		assertEquals(null, status.getResultFileHandleId());
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
	public void testCreateTokenNull() throws JSONObjectAdapterException{
		createRequest.setStorageLocationToken(null);
		// call under test
		multipartUplaodDAO.createUploadStatus(createRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateRequestStringNull() throws JSONObjectAdapterException{
		createRequest.setRequestString(null);
		// call under test
		multipartUplaodDAO.createUploadStatus(createRequest);
	}
	
	@Test
	public void testGetUploadStatusById(){
		MultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		// call under tests
		MultipartUploadStatus fetched = multipartUplaodDAO.getUploadStatus(status.getUploadId());
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
		MultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(createRequest);
		assertNotNull(status);
		// call under tests
		MultipartUploadStatus fetched = multipartUplaodDAO.getUploadStatus(userId, hash);
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
		MultipartUploadStatus fetched = multipartUplaodDAO.getUploadStatus(userId, hash);
		assertEquals(null, fetched);
	}
}
