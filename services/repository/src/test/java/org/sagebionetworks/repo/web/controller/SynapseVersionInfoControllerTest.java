package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;

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
