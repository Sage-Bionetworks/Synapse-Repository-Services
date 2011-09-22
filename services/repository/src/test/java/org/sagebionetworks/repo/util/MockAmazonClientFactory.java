package org.sagebionetworks.repo.util;

import static org.mockito.Mockito.mock;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;

/**
 * @author deflaux
 *
 */
public class MockAmazonClientFactory implements
		AmazonClientFactory {

	@Override
	public AmazonIdentityManagement getAmazonIdentityManagementClient() {
		AmazonIdentityManagement mock = mock(AmazonIdentityManagementClient.class);
		return mock;
	}


	@Override
	public AWSSecurityTokenService getAWSSecurityTokenServiceClient() {
		AWSSecurityTokenService mock = mock(AWSSecurityTokenServiceClient.class);
		return mock;
	}

}
