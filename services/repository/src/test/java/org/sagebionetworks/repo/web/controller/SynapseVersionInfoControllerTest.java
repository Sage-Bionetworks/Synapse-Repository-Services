package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;

/**
 *
 * @author xschildw
 */
public class SynapseVersionInfoControllerTest extends AbstractAutowiredControllerJunit5TestBase {

	@Test
	public void testGetVersionInfo() throws Exception {
		SynapseVersionInfo vi;
		vi = servletTestHelper.getVersionInfo();
		assertTrue(vi.getVersion().length() > 0);
		assertNotNull(vi.getStackInstance());
	}
	
}
