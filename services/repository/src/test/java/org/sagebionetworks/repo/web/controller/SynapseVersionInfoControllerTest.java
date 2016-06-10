package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author xschildw
 */
public class SynapseVersionInfoControllerTest extends AbstractAutowiredControllerTestBase {

	@Test
	public void testGetVersionInfo() throws Exception {
		SynapseVersionInfo vi;
		vi = servletTestHelper.getVersionInfo();
		assertTrue(vi.getVersion().length() > 0);
	}
	
}
