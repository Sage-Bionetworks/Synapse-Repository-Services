package org.sagebionetworks.repo.util;

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

//	// The integration test IAM user
//	private String iamIntegrationTestCredsAccessId = FAKE_ACCESS_ID;
//	private String iamIntegrationTestCredsSecretKey = FAKE_SECRET_KEY;
	//private AWSCredentials iamIntegrationTestCreds;
	
	private AmazonIdentityManagement aim = null;

	public AmazonIdentityManagement getAmazonIdentityManagement() {
		if (aim==null) {
			aim = newAmazonIdentityManagement();
		}
		return aim;
	}
	
	private AmazonIdentityManagement newAmazonIdentityManagement() {
		if ((null != System.getProperty("AWS_ACCESS_KEY_ID"))
				&& (null != System.getProperty("AWS_SECRET_KEY"))) {
			// Dev Note: these particular environment variable names are what
			// Elastic Beanstalk supports for passing creds via environment
			// properties
			// https://forums.aws.amazon.com/thread.jspa?messageID=217139&#217139
			iamCanCreateUserCredsAccessId = System
					.getProperty("AWS_ACCESS_KEY_ID");
			iamCanCreateUserCredsSecretKey = System
					.getProperty("AWS_SECRET_KEY");
		}

		iamCanCreateUsersCreds = new BasicAWSCredentials(
				iamCanCreateUserCredsAccessId, iamCanCreateUserCredsSecretKey);
		
			
		AmazonIdentityManagement iamClient = new AmazonIdentityManagementClient(iamCanCreateUsersCreds);
		
		return iamClient;
	}

}
