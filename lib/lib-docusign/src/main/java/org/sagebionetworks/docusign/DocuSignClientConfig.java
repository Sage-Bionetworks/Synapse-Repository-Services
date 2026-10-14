package org.sagebionetworks.docusign;

/**
 * Static configuration values used by the DocuSign client to authenticate via
 * the JWT Bearer Grant and call the DocuSign REST API.
 */
public interface DocuSignClientConfig {

	String getIntegrationKey();

	String getUserId();

	String getAccountId();

	byte[] getPrivateKeyBytes();

	String getBasePath();

	String getOAuthBasePath();
}
