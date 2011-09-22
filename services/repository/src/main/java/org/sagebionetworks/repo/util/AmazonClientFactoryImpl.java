package org.sagebionetworks.repo.util;

import org.sagebionetworks.StackConfiguration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;

/**
 * @author deflaux
 *
 */
public class AmazonClientFactoryImpl implements AmazonClientFactory {
	
	String accessId = (null != StackConfiguration.getIAMUserId()) ? StackConfiguration.getIAMUserId() : "thisIsAFakeAWSAccessId";
	String secretKey = (null != StackConfiguration.getIAMUserKey()) ? StackConfiguration.getIAMUserKey() : "thisIsAFakeAWSSecretKey";

	@Override
	public AmazonIdentityManagement getAmazonIdentityManagementClient() {

		AWSCredentials credentials = new BasicAWSCredentials(accessId, secretKey);
		
		ClientConfiguration awsClientConfig = new ClientConfiguration();
		// The javadocs say the protocol defaults to HTTPS, but let's set it here to be on the safe side
		awsClientConfig.setProtocol(Protocol.HTTPS);			
		AmazonIdentityManagement iamClient = new AmazonIdentityManagementClient(credentials,
				awsClientConfig);
		
		return iamClient;
	}

	@Override
	public AWSSecurityTokenService getAWSSecurityTokenServiceClient() {
		AWSCredentials credentials = new BasicAWSCredentials(accessId, secretKey);
		
		ClientConfiguration awsClientConfig = new ClientConfiguration();
		// The javadocs say the protocol defaults to HTTPS, but let's set it here to be on the safe side
		awsClientConfig.setProtocol(Protocol.HTTPS);			
		AWSSecurityTokenService stsClient = new AWSSecurityTokenServiceClient(credentials,
				awsClientConfig);
		
		return stsClient;
	}

}
