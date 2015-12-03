package org.sagebionetworks.repo.model.dbo.file;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MultipartUploadDAOImplTest {

	@Autowired
	MultipartUploadDAO multipartUplaodDAO;
	List<String> toDelete;
	
	Long userId;
	
	@Before
	public void before(){
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		for(String hash: toDelete){
			multipartUplaodDAO.deleteUploadStatus(userId, hash);
		}
	}
	
	@Test
	public void testCreate(){
		String hash = "someHash";
		String providerId = "awsId";
		MultipartUploadRequest request = new MultipartUploadRequest();
		request.setFileName("foo.txt");
		request.setFileSizeBytes(123L);
		request.setMd5Hex("someMD5Hex");
		request.setPartSizeBytes(5L);
		request.setStorageLocationId(null);
		
		MultipartUploadStatus status = multipartUplaodDAO.createUploadStatus(userId, providerId, hash, request);
		assertNotNull(status);
		assertNotNull(status.getStartedOn());
		assertNotNull(status.getUpdatedOn());
		toDelete.add(hash);		
	}
}
