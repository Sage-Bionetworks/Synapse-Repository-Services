package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AmazonS3Utility;
import org.sagebionetworks.repo.manager.S3TokenManagerImpl;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class S3TokenControllerTest {

	@Autowired
	private ServletTestHelper testHelper;
	@Autowired
	AmazonS3Utility s3Utility;

	private static final String TEST_USER1 = TestUserDAO.TEST_USER_NAME;
	private static final String TEST_USER2 = "testuser2@test.org";
	private static final String TEST_MD5 = "4053f00b39aae693a6969f37102e2764";

	private Project project;
	private Study dataset;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		testHelper.setUp();
		testHelper.setTestUser(TEST_USER1);

		project = new Project();
		project.setName("proj");
		project = testHelper.createEntity(project, null);

		dataset = new Study();
		dataset.setName("study");
		dataset.setParentId(project.getId());
		dataset = testHelper.createEntity(dataset, null);

		// Add a public read ACL to the project object
		AccessControlList projectAcl = testHelper.getEntityACL(project);
		ResourceAccess ac = new ResourceAccess();
		ac
				.setGroupName(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS
						.name());
		ac.setAccessType(new HashSet<ACCESS_TYPE>());
		ac.getAccessType().add(ACCESS_TYPE.READ);
		projectAcl.getResourceAccess().add(ac);
		projectAcl = testHelper.updateEntityAcl(project, projectAcl);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		testHelper.setTestUser(TEST_USER1);
		testHelper.tearDown();
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCreateS3TokenAbsolutePath() throws Exception {
		Data layer = new Data();
		layer.setParentId(dataset.getId());
		layer.setType(LayerTypeNames.E);
		layer = testHelper.createEntity(layer, null);

		// an absolute path to a file
		String initialPath = "/data/123.zip";

		S3Token token = new S3Token();
		token.setPath(initialPath);
		token.setMd5(TEST_MD5);

		token = testHelper.createObject(layer.getS3Token(), token);

		assertEquals(StackConfiguration.getS3Bucket(), token.getBucket());
		assertTrue(token.getPath().matches(
				"^/" + KeyFactory.stringToKey(layer.getId()) + "/\\d+" + initialPath + "$"));
		assertEquals(TEST_MD5, token.getMd5());
		assertEquals("application/zip", token.getContentType());
		assertNotNull(token.getSecretAccessKey());
		assertNotNull(token.getAccessKeyId());
		assertNotNull(token.getSessionToken());
		assertTrue(token.getPresignedUrl().matches(
				"^http.*" + initialPath + ".*"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCreateS3TokenRelativePath() throws Exception {
		Code code = new Code();
		code.setParentId(project.getId());
		code = testHelper.createEntity(code, null);

		// a relative path to a file
		String initialPath = "foo.java";
		S3Token token = new S3Token();
		token.setPath(initialPath);
		token.setMd5(TEST_MD5);

		token = testHelper.createObject(code.getS3Token(), token);

		assertEquals(StackConfiguration.getS3Bucket(), token.getBucket());
		assertTrue(token.getPath().matches(
				"^/" + KeyFactory.stringToKey(code.getId()) + "/\\d+/" + initialPath + "$"));
		assertEquals(TEST_MD5, token.getMd5());
		assertEquals("text/plain", token.getContentType());
		assertNotNull(token.getSecretAccessKey());
		assertNotNull(token.getAccessKeyId());
		assertNotNull(token.getSessionToken());
		assertTrue(token.getPresignedUrl().matches(
				"^http.*" + initialPath + ".*"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCreateS3TokenInsufficientPermissions() throws Exception {
		String initialPath = "foo.java";
		S3Token token = new S3Token();
		token.setPath(initialPath);
		token.setMd5(TEST_MD5);
		token = testHelper.createObject(dataset.getUri() + "/"
				+ UrlHelpers.S3TOKEN, token);

		testHelper.setTestUser(TEST_USER2);
		try {
			token = testHelper.createObject(dataset.getUri() + "/"
					+ UrlHelpers.S3TOKEN, token);
			fail("expected exception not thrown");
		} catch (ServletTestHelperException ex) {
			assertTrue(ex
					.getMessage()
					.startsWith(
							"update access is required to obtain an S3Token for entity"));
			assertEquals(HttpStatus.FORBIDDEN.value(), ex.getHttpStatus());
		}
	}
	@Ignore
	@Test
	public void testcreateS3AttachmentToken() throws JSONObjectAdapterException, ServletException, IOException, DatastoreException{
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("someImage.jpg");
		startToken.setMd5(TEST_MD5);
		
		String fileName = "images/notAnImage.txt";
		URL toUpUrl = S3TokenControllerTest.class.getClassLoader().getResource(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", toUpUrl);
		File toUpload = new File(toUpUrl.getFile());
		// Create the token
		S3AttachmentToken resultToken = testHelper.createS3AttachmentToken(TestUserDAO.TEST_USER_NAME, project.getId(), startToken);
		System.out.println(resultToken);
		assertNotNull(resultToken);
		assertNotNull(resultToken.getTokenId());
		assertNotNull(resultToken.getPresignedUrl());
		
		// Upload it
		String path = S3TokenManagerImpl.createAttachmentPathNoSlash(project.getId(), resultToken.getTokenId());
		s3Utility.uploadToS3(toUpload, path);

		// Make sure we can get a signed download URL for this attachment.
		long now = System.currentTimeMillis();
		long oneMinuteFromNow = now + (60*1000);
		PresignedUrl url = testHelper.getAttachmentUrl(TestUserDAO.TEST_USER_NAME, project.getId(), resultToken.getTokenId());
		System.out.println(url);
		assertNotNull(url);
		assertNotNull(url.getPresignedUrl());
		URL urlReal = new URL(url.getPresignedUrl());
		// Check that it expires quickly.
		String[] split = urlReal.getQuery().split("&");
		assertTrue(split.length > 1);
		String[] expiresSplit = split[0].split("=");
		assertEquals("Expires", expiresSplit[0]);
		//It should expire within a minute max
		Long expirsInt = Long.parseLong(expiresSplit[1]);
		long expiresMil = expirsInt.longValue() * 1000l;
		System.out.println("Now: "+new Date(now));
		System.out.println("Expires: "+new Date(expiresMil));
		assertTrue("This URL should expire in under a minute!", expiresMil < oneMinuteFromNow);
		assertTrue("This URL should expire after now!", now <= expiresMil);
		
		// Delete the file
		s3Utility.deleteFromS3(path);
	}

}
