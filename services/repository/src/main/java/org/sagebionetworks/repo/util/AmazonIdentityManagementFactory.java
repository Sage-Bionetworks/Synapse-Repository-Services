package org.sagebionetworks.repo.util;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

public interface AmazonIdentityManagementFactory {
	public AmazonIdentityManagement getAmazonIdentityManagement();
}
