package org.sagebionetworks.web.server.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.web.server.ServerConstants;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Provides the rest API service URL
 * 
 * @author jmhill
 *
 */
public class ServiceUrlProvider {
	
	private static Logger logger = Logger.getLogger(ServiceUrlProvider.class.getName());
	private String repositoryServiceUrl = StackConfiguration.getRepositoryServiceEndpoint();
	private String authServicePrivateUrl = StackConfiguration.getAuthenticationServicePrivateEndpoint();
	private String authServicePublicUrl = StackConfiguration.getAuthenticationServicePublicEndpoint();
	private String portalBaseUrl = StackConfiguration.getPortalEndpoint();
	
	/**
	 * The repository service url 
	 * @return
	 * @throws URISyntaxException 
	 */
	public String getRepositoryServiceUrl() {
		if(repositoryServiceUrl == null){
			logger.info("Repository Service URL: " + repositoryServiceUrl);
			// Make sure it is a valid url
			try {
				new URI(repositoryServiceUrl);
			} catch (URISyntaxException e) {
				 throw new IllegalArgumentException(e);
			}
		}
		return repositoryServiceUrl;
	}

	/**
	 * The auth service url
	 * @return
	 * @throws URISyntaxException 
	 */
	public String getPrivateAuthBaseUrl() {
		if(authServicePrivateUrl == null){
			logger.info("Auth Service URL: " + repositoryServiceUrl);
			// Make sure it is a valid url
			try {
				new URI(authServicePrivateUrl);
			} catch (URISyntaxException e) {
				 throw new IllegalArgumentException(e);
			}
		}
		return authServicePrivateUrl;
	}

	/**
	 * The auth service url
	 * @return
	 * @throws URISyntaxException 
	 */
	public String getPublicAuthBaseUrl() {
		if(authServicePublicUrl == null){
			logger.info("Auth Service URL: " + repositoryServiceUrl);
			// Make sure it is a valid url
			try {
				new URI(authServicePublicUrl);
			} catch (URISyntaxException e) {
				 throw new IllegalArgumentException(e);
			}
		}
		return authServicePublicUrl;
	}

	/**
	 * The portal (this GWT project) endpoint
	 * @return
	 * @throws URISyntaxException 
	 */
	public String getPortalBaseUrl() {
		if(portalBaseUrl == null){
			logger.info("Portal Base URL: " + portalBaseUrl);
			// Make sure it is a valid url
			try {
				new URI(portalBaseUrl);
			} catch (URISyntaxException e) {
				 throw new IllegalArgumentException(e);
			}
		}
		return portalBaseUrl;
	}

	
	/**
	 * For testing purposes
	 * @param repositoryServiceUrl
	 */
	public void setRepositoryServiceUrl(String repositoryServiceUrl) {
		this.repositoryServiceUrl = repositoryServiceUrl;
	}

	/**
	 * For testing purposes
	 * @param authServiceUrl
	 */
	public void setAuthServicePrivateUrl(String authServicePrivateUrl) {
		this.authServicePrivateUrl = authServicePrivateUrl;
	}

	/**
	 * For testing purposes
	 * @param authServiceUrl
	 */
	public void setAuthServicePublicUrl(String authServicePublicUrl) {
		this.authServicePublicUrl = authServicePublicUrl;
	}

	/**
	 * For testing purposes
	 * @param portalBaseUrl
	 */
	public void setPortalBaseUrl(String portalBaseUrl) {
		this.portalBaseUrl = portalBaseUrl;
	}


}
