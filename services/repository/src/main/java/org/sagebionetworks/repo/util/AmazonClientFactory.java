package org.sagebionetworks.repo.util;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;

/**
 * This interface wraps the logic to fetch Synapse AWS credentials from the right spot and create instances of AWS webservice clients
 * @author deflaux
 *
 */
public interface AmazonClientFactory {
	/**
	 * @return a client for the Identity and Access Management Service
	 */
	public AmazonIdentityManagement getAmazonIdentityManagementClient();
	/**
	 * @return a client for the Security Token Service
	 */
	public AWSSecurityTokenService getAWSSecurityTokenServiceClient();
}
