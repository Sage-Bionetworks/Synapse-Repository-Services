package org.sagebionetworks.repo.util;

import org.sagebionetworks.StackConfiguration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;

public class AmazonIdentityManagementFactoryImpl implements AmazonIdentityManagementFactory {
	/**
	 * 
	 */
	public static final String FAKE_ACCESS_ID = "thisIsAFakeAWSAccessId";
	/**
	 * 
	 */
	public static final String FAKE_SECRET_KEY = "thisIsAFakeAWSSecretKey";

	// The IAM user who has permissions to make new IAM users
	private String iamCanCreateUserCredsAccessId = FAKE_ACCESS_ID;
	private String iamCanCreateUserCredsSecretKey = FAKE_SECRET_KEY;
	private AWSCredentials iamCanCreateUsersCreds;
	
	private AmazonIdentityManagement aim = null;

	public AmazonIdentityManagement getAmazonIdentityManagement() {
		if (aim==null) {
			aim = newAmazonIdentityManagement();
		}
		return aim;
	}
	
	private AmazonIdentityManagement newAmazonIdentityManagement() {
		if ((null != StackConfiguration.getIAMUserId()) 
				&& (null != StackConfiguration.getIAMUserKey())) {
			// Dev Note: these particular environment variable names are what
			// Elastic Beanstalk supports for passing creds via environment
			// properties
			// https://forums.aws.amazon.com/thread.jspa?messageID=217139&#217139
			iamCanCreateUserCredsAccessId = StackConfiguration.getIAMUserId();
			iamCanCreateUserCredsSecretKey = StackConfiguration.getIAMUserKey();
		}

		iamCanCreateUsersCreds = new BasicAWSCredentials(
				iamCanCreateUserCredsAccessId, iamCanCreateUserCredsSecretKey);
		
		ClientConfiguration awsClientConfig = new ClientConfiguration();
		// The javadocs say the protocol defaults to HTTPS, but let's set it here to be on the safe side
		awsClientConfig.setProtocol(Protocol.HTTPS);			
		AmazonIdentityManagement iamClient = new AmazonIdentityManagementClient(iamCanCreateUsersCreds,
				awsClientConfig);
		
		return iamClient;
	}

}
