package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.web.UrlHelpers;
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

	private static final String TEST_USER1 = TestUserDAO.TEST_USER_NAME;
	private static final String TEST_USER2 = "testuser2@test.org";
	private static final String TEST_MD5 = "4053f00b39aae693a6969f37102e2764";

	private Project project;
	private Dataset dataset;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		testHelper.setUp();
		testHelper.setTestUser(TEST_USER1);

		project = new Project();
		project = testHelper.createEntity(project, null);

		dataset = new Dataset();
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
		Layer layer = new Layer();
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
				"^/" + layer.getId() + "/\\d+" + initialPath + "$"));
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
				"^/" + code.getId() + "/\\d+/" + initialPath + "$"));
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

}
