package org.sagebionetworks.web.server.servlet;

import java.util.logging.Logger;

import org.sagebionetworks.web.client.services.NodeService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.NodeType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

/**
 * The server-side implementation of the DatasetService. This serverlet will
 * communicate with the platform API via REST.
 * 
 * @author jmhill
 * 
 */
@SuppressWarnings("serial")
public class NodeServiceImpl extends RemoteServiceServlet implements
		NodeService {

	private static Logger logger = Logger.getLogger(NodeServiceImpl.class
			.getName());

	public static final String PATH_DATASET = "dataset/";
	public static final String PATH_LAYER = "layer/";
	public static final String PATH_PROJECT = "project/";
	
	public static final String PATH_SCHEMA = "test/schema"; // the "test" can be any string
	
	
	private RestTemplateProvider templateProvider = null;
	private ServiceUrlProvider urlProvider;

	/**
	 * The rest template will be injected via Guice.
	 * 
	 * @param template
	 */
	@Inject
	public void setRestTemplate(RestTemplateProvider template) {
		this.templateProvider = template;
	}

	/**
	 * Injected vid Gin
	 * @param provider
	 */
	@Inject
	public void setServiceUrlProvider(ServiceUrlProvider provider){
		this.urlProvider = provider;
	}
	
	/**
	 * Validate that the service is ready to go. If any of the injected data is
	 * missing then it cannot run. Public for tests.
	 */
	public void validateService() {
		if (templateProvider == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.web.server.RestTemplateProvider was not injected into this service");
		if (templateProvider.getTemplate() == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.web.server.RestTemplateProvider returned a null template");
		if (urlProvider == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.rest.api.root.url was not set");
	}

	@Override
	public String getNodeJSONSchema(NodeType type) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilder(type);
		builder.append(PATH_SCHEMA);		
		String url = builder.toString();		
		logger.info("GET: " + url);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {			
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}

	@Override
	public String getNodeJSON(NodeType type, String id) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilder(type);
		builder.append(id);		
		String url = builder.toString();		
		logger.info("GET: " + url);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {			
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}
	

	/*
	 * Private Methods
	 */
	private StringBuilder getBaseUrlBuilder(NodeType type) {
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl());
		// set path based on type
		switch(type) {
		case DATASET:
			builder.append(PATH_DATASET);
			break;
		case LAYER:
			builder.append(PATH_LAYER);
			break;
		case PROJECT:
			builder.append(PATH_PROJECT);
			break;
		default:
			//throw new IllegalArgumentException("Unsupported type:" + type.toString());
		}
		return builder;
	}
	
}
