package org.sagebionetworks.repo.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;
import com.amazonaws.services.securitytoken.model.GetFederationTokenResult;

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

		Credentials creds = new Credentials();
		creds.setAccessKeyId("fakeAccessKeyId");
		creds.setSecretAccessKey("fakeSecretAccessKey");
		creds.setSessionToken("fakeSessionToken");

		GetFederationTokenResult result = new GetFederationTokenResult();
		result.setCredentials(creds);

		when(mock.getFederationToken(Mockito.<GetFederationTokenRequest>anyObject())).thenReturn(result);
		return mock;
	}

}
