package org.sagebionetworks.repo.model;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.web.NotFoundException;

public class CrowdUserDAOTest {

	private boolean isIntegrationTest() {
		String integrationTestEndpoint = System.getProperty("INTEGRATION_TEST_ENDPOINT");
		return integrationTestEndpoint!=null && integrationTestEndpoint.length()>0;
	}

	@BeforeClass
	public static void setUp() throws Exception {
		CrowdAuthUtil.acceptAllCertificates2();
	}

	private static final String TEST_USER = "demouser@sagebase.org";
	
	@Test
	public void testGetUser() throws Exception {
		if (!isIntegrationTest()) return;
		CrowdUserDAO userDAO = new CrowdUserDAO();
		User user = userDAO.getUser(TEST_USER);
		assertEquals(TEST_USER, user.getId());
		assertEquals(TEST_USER, user.getUserId());
		assertEquals("foo", user.getIamAccessId());
		assertEquals("bar", user.getIamSecretKey());
		DateFormat df = new SimpleDateFormat("yyyy-mm-dd");
		Date createDate = df.parse("2011-05-18");
		assertEquals(createDate, user.getCreationDate());
	}

	@Test
	public void testGetNonexistentUser() throws Exception {
		if (!isIntegrationTest()) return;
		CrowdUserDAO userDAO = new CrowdUserDAO();
		try {
			userDAO.getUser("foo");
		} catch (NotFoundException nfe) {
			// as expected
			return;
		}
		fail("exception expected");
	}
	
}
