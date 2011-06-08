package org.sagebionetworks.web.server.servlet;

import java.util.logging.Logger;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.services.NodeService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.NodeType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

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

	public static final String PATH_DATASET = "dataset";
	public static final String PATH_LAYER = "layer";
	public static final String PATH_PROJECT = "project";
	public static final String ANNOTATIONS_PATH = "annotations";
	
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
		builder.append("/");
		builder.append(PATH_SCHEMA);		
		String url = builder.toString();		
		logger.info("GET: " + url);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
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
		builder.append("/");
		builder.append(id);		
		String url = builder.toString();		
		return getJsonStringForUrl(url, HttpMethod.GET);
	}
	
	@Override
	public String createNode(NodeType type, String propertiesJson) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilder(type);		 		
		String url = builder.toString();		
		logger.info("POST: " + url + ", JSON: " + propertiesJson);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(propertiesJson, headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, HttpMethod.POST, entity, String.class);

		if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			throw new RestClientException("Status code:" + response.getStatusCode().value());
		}		
	}

	@Override
	public String updateNode(NodeType type, String id, String propertiesJson, String eTag) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilder(type);
		builder.append("/" + id);
		String url = builder.toString();		
		logger.info("PUT: " + url + ", JSON: " + propertiesJson);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(DisplayConstants.SERVICE_HEADER_ETAG_KEY, eTag);
		HttpEntity<String> entity = new HttpEntity<String>(propertiesJson, headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, HttpMethod.PUT, entity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			throw new RestClientException("Status code:" + response.getStatusCode().value());
		}
	}
	
	@Override
	public void deleteNode(NodeType type, String id) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilder(type);
		builder.append("/" + id);
		String url = builder.toString();		
		logger.info("PUT: " + url);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, HttpMethod.PUT, entity, String.class);

		if (response.getStatusCode() != HttpStatus.NO_CONTENT && response.getStatusCode() != HttpStatus.OK) {
			throw new RestClientException("Status code:" + response.getStatusCode().value());
		}
	}

	@Override
	public String getNodeAnnotationsJSON(NodeType type, String id) {
		// Build up the path
		StringBuilder builder = getBaseUrlBuilder(type);
		builder.append("/" + id);
		builder.append("/" + ANNOTATIONS_PATH);
		String url = builder.toString();	
		return getJsonStringForUrl(url, HttpMethod.GET);
	}

	@Override
	public String updateNodeAnnotations(NodeType type, String id, String annotationsJson, String etag) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilder(type);
		builder.append("/" + id);
		builder.append("/" + ANNOTATIONS_PATH);
		String url = builder.toString();		
		logger.info("PUT: " + url + ", JSON: " + annotationsJson);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(DisplayConstants.SERVICE_HEADER_ETAG_KEY, etag);
		HttpEntity<String> entity = new HttpEntity<String>(annotationsJson, headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, HttpMethod.PUT, entity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			throw new RestClientException("Status code:" + response.getStatusCode().value());
		}

	}

	
	/*
	 * Private Methods
	 */
	private String getJsonStringForUrl(String url, HttpMethod method) {
		// First make sure the service is ready to go.
		validateService();

		logger.info(method.toString() + ": " + url);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {			
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
		
	}
	
	private StringBuilder getBaseUrlBuilder(NodeType type) {
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl());
		// set path based on type
		switch(type) {
		case DATASET:
			builder.append(PATH_DATASET);
			break;
		case PROJECT:
			builder.append(PATH_PROJECT);
			break;
		default:
			throw new IllegalArgumentException("Unsupported type:" + type.toString());
		}
		return builder;
	}

	private StringBuilder getBaseUrlBuilderTwoLayer(NodeType layerTwotype, NodeType layerOneType, String layerOneId) {	
		StringBuilder sb = getBaseUrlBuilder(layerOneType);
		sb.append("/" + layerOneId + "/");
		sb.append(PATH_LAYER);
		return sb;
	}
	
	
	/*
	 * Temp hacky two layer impls
	 */
	@Override
	public String createNodeTwoLayer(NodeType type, String propertiesJson, NodeType layerOneType, String layerOneId) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilderTwoLayer(type, layerOneType, layerOneId);	 		
		String url = builder.toString();		
		logger.info("POST: " + url + ", JSON: " + propertiesJson);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(propertiesJson, headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, HttpMethod.POST, entity, String.class);

		if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			throw new RestClientException("Status code:" + response.getStatusCode().value());
		}		
	}
	
	@Override 
	public String updateNodeTwoLayer(NodeType type, String id, String propertiesJson, String eTag, NodeType layerOneType, String layerOneId) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilderTwoLayer(type, layerOneType, layerOneId);
		builder.append("/" + id);
		String url = builder.toString();		
		logger.info("PUT: " + url + ", JSON: " + propertiesJson);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(DisplayConstants.SERVICE_HEADER_ETAG_KEY, eTag);
		HttpEntity<String> entity = new HttpEntity<String>(propertiesJson, headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, HttpMethod.PUT, entity, String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			throw new RestClientException("Status code:" + response.getStatusCode().value());
		}
	}

	
	@Override
	public String getNodeJSONSchemaTwoLayer(NodeType type, NodeType layerOneType, String layerOneId) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilderTwoLayer(type, layerOneType, layerOneId);
		builder.append("/");
		builder.append(PATH_SCHEMA);		
		String url = builder.toString();		
		logger.info("GET: " + url);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
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
	public String getNodeJSONTwoLayer(NodeType type, String id, NodeType layerOneType, String layerOneId) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = getBaseUrlBuilderTwoLayer(type, layerOneType, layerOneId);
		builder.append("/");
		builder.append(id);		
		String url = builder.toString();		
		logger.info("GET: " + url);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
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
	
}
