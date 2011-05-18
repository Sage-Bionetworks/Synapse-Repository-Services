package org.sagebionetworks.repo.model;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.web.NotFoundException;

public class CrowdGroupMembershipDAOTest {
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
	public void testGetUserGroups() throws Exception {
		if (!isIntegrationTest()) return;
		CrowdGroupMembershipDAO dao = new CrowdGroupMembershipDAO();
		Collection<String> groups = dao.getUserGroupNames(TEST_USER);
		Set<String> expected = new HashSet<String>(
				Arrays.asList(new String[]{"platform"})
		);
		Set<String> actual = new HashSet<String>(groups);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGetNonexistentUser() throws Exception {
		if (!isIntegrationTest()) return;
		CrowdGroupMembershipDAO dao = new CrowdGroupMembershipDAO();
		try {
			System.out.println(dao.getUserGroupNames("foo"));
		} catch (NotFoundException nfe) {
			// as expected
			return;
		}
		fail("exception expected");
	}
	

}
