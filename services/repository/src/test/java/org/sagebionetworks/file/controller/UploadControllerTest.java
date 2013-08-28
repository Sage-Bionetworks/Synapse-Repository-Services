package org.sagebionetworks.file.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.controller.DispatchServletSingleton;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UploadControllerTest {

	@Autowired
	UserManager userManager;
	@Autowired
	FileHandleDao fileMetadataDao;

	private String userName;
	private String ownerId;

	S3FileHandle handleOne;
	PreviewFileHandle handleTwo;
	List<String> toDelete;

	@Before
	public void before() throws Exception {
		toDelete = new LinkedList<String>();
		// get user IDs
		userName = TestUserDAO.TEST_USER_NAME;
		ownerId = userManager.getUserInfo(userName).getIndividualGroup()
				.getId();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(ownerId);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne = fileMetadataDao.createFile(handleOne);
		toDelete.add(handleOne.getId());
		// Create a preview
		handleTwo = new PreviewFileHandle();
		handleTwo.setCreatedBy(ownerId);
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("previewFileKey");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("bar.txt");
		handleTwo = fileMetadataDao.createFile(handleTwo);
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), handleTwo.getId());
		toDelete.add(handleTwo.getId());
	}

	@After
	public void after() throws Exception {
		for (String id : toDelete) {
			fileMetadataDao.delete(id);
		}
	}

	@Test
	public void testGetFileHandle() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		// Request the real object
		request.setRequestURI("/fileHandle/" + handleOne.getId());
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		DispatchServletSingleton.getInstance().service(request, response);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		String body = response.getContentAsString();
		S3FileHandle handle = EntityFactory.createEntityFromJSONString(body,
				S3FileHandle.class);
		assertEquals(handleOne.getId(), handle.getId());
	}
	
	@Test
	public void testClearPreview() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		// Request the real object
		request.setRequestURI("/fileHandle/" + handleOne.getId() + "/filepreview");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		DispatchServletSingleton.getInstance().service(request, response);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
	}

	@Test
	public void testExternalFileHandle() throws Exception {
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setExternalURL("http://www.google.com");
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/externalFileHandle");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		String body = EntityFactory.createJSONStringForEntity(efh);
		request.setContent(body.getBytes("UTF-8"));
		DispatchServletSingleton.getInstance().service(request, response);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		ExternalFileHandle returned = EntityFactory.createEntityFromJSONString(response.getContentAsString(),ExternalFileHandle.class);
		assertNotNull(returned);
		toDelete.add(returned.getId());
		assertEquals(efh.getExternalURL(), returned.getExternalURL());
	}
	
	@Test
	public void testPLFM_1944() throws Exception{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		// Request a non-existent object
		request.setRequestURI("/fileHandle/" + "-123");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		DispatchServletSingleton.getInstance().service(request, response);
		assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
		String body = response.getContentAsString();
		ErrorResponse er = EntityFactory.createEntityFromJSONString(body, ErrorResponse.class);
		assertEquals("The resource you are attempting to access cannot be found", er.getReason());
	}
}
