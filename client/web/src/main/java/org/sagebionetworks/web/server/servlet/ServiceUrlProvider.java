package org.sagebionetworks.web.server.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

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
	private String restEndpoint;
	private String restServletPrefix;
	private String authEndpoint;
	private String authServletPrefix;
	private String baseUrl = null;
	private String authBaseUrl = null;

	/**
	 * Injected via Guice from the ServerConstants.properties file.
	 * 
	 * @param url
	 */
	@Inject
	public void setRestEndpoint(@Named(ServerConstants.KEY_REST_API_ENDPOINT) String endpoint) {
		this.restEndpoint = endpoint;
		logger.info("rest endpoint: " + endpoint);
	}

	/**
	 * Injected via Guice from the ServerConstants.properties file.
	 * 
	 * @param url
	 */
	@Inject
	public void setAuthEndpoint(@Named(ServerConstants.KEY_AUTH_API_ENDPOINT) String endpoint) {
		this.authEndpoint = endpoint;
		logger.info("auth endpoint: " + endpoint);
	}

	/**
	 * Injected via Guice from the ServerConstants.properties file.
	 * 
	 * @param url
	 */
	@Inject
	public void setServletPrefix(@Named(ServerConstants.KEY_REST_API_SERVLET_PREFIX) String prefix) {
		this.restServletPrefix = prefix;
		logger.info("rest prefix: " + prefix);
	}
	
	/**
	 * Injected via Guice from the ServerConstants.properties file.
	 * 
	 * @param url
	 */
	@Inject
	public void setAuthPrefix(@Named(ServerConstants.KEY_AUTH_API_SERVLET_PREFIX) String prefix) {
		this.authServletPrefix = prefix;
		logger.info("auth prefix: " + prefix);
	}
	
	/**
	 * The base url = restEndpoint + restServletPrefix
	 * @return
	 * @throws URISyntaxException 
	 */
	public String getBaseUrl() {
		if(baseUrl == null){
			if(this.restEndpoint == null) throw new IllegalArgumentException("The property: "+ServerConstants.KEY_REST_API_ENDPOINT+" must be set");
			if(this.restServletPrefix == null) throw new IllegalArgumentException("The property: "+ServerConstants.KEY_REST_API_SERVLET_PREFIX+" must be set");
			baseUrl = this.restEndpoint+this.restServletPrefix;
			logger.info("Base rest url: " + baseUrl);
			// Make sure it is a valid url
			try {
				new URI(baseUrl);
			} catch (URISyntaxException e) {
				 throw new IllegalArgumentException(e);
			}
		}
		return baseUrl;
	}

	/**
	 * The auth base url = authEndpoint + authServletPrefix
	 * @return
	 * @throws URISyntaxException 
	 */
	public String getAuthBaseUrl() {
		if(authBaseUrl == null){
			if(this.authEndpoint == null) throw new IllegalArgumentException("The property: "+ServerConstants.KEY_AUTH_API_ENDPOINT+" must be set");
			if(this.authServletPrefix == null) throw new IllegalArgumentException("The property: "+ServerConstants.KEY_AUTH_API_SERVLET_PREFIX+" must be set");
			authBaseUrl = this.authEndpoint+this.authServletPrefix;
			logger.info("Base auth url: " + baseUrl);
			// Make sure it is a valid url
			try {
				new URI(authBaseUrl);
			} catch (URISyntaxException e) {
				 throw new IllegalArgumentException(e);
			}
		}
		return authBaseUrl;
	}

}
