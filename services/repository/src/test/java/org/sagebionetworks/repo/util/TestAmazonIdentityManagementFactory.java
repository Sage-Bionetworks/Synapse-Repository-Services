package org.sagebionetworks.repo.util;

import org.junit.Test;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

public class TestAmazonIdentityManagementFactory implements
		AmazonIdentityManagementFactory {
	@Test
	public void fake() throws Exception {
		// to suppress error message by testing framework
	}


	@Override
	public AmazonIdentityManagement getAmazonIdentityManagement() {
		return new TestAmazonIdentityManagement();
	}

}
