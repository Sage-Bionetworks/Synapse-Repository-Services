package org.sagebionetworks.web.server.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.services.NodeService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.users.AclAccessType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RestTemplate;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

/**
 * The server-side implementation of the DatasetService. This serverlet will
 * communicate with the platform API via REST.
 * 
 * @author dburdick
 * 
 */
@SuppressWarnings("serial")
public class NodeServiceImpl extends RemoteServiceServlet implements
		NodeService {

	private static Logger logger = Logger.getLogger(NodeServiceImpl.class
			.getName());

	
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
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/");
		builder.append(ServiceUtils.REPOSVC_SUFFIX_PATH_SCHEMA);		
		String url = builder.toString();		
		logger.info("GET: " + url);
		return getJsonStringForUrl(url, HttpMethod.GET);
	}

	@Override
	public String getNodeJSON(NodeType type, String id) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
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
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);		 		
		String url = builder.toString();		
		return getJsonStringForUrl(url, HttpMethod.POST, propertiesJson);
	}

	@Override
	public String updateNode(NodeType type, String id, String propertiesJson, String eTag) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		String url = builder.toString();		
		return getJsonStringForUrl(url, HttpMethod.PUT, propertiesJson, eTag);
	}
	
	@Override
	public void deleteNode(NodeType type, String id) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		String url = builder.toString();		
		getJsonStringForUrl(url, HttpMethod.DELETE);
	}

	@Override
	public String getNodeAnnotationsJSON(NodeType type, String id) {
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_PATH_ANNOTATIONS);
		String url = builder.toString();	
		return getJsonStringForUrl(url, HttpMethod.GET);
	}
	
	@Override
	public String getNodePreview(NodeType type, String id) {
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_PATH_PREVIEW);
		String url = builder.toString();	
		return getJsonStringForUrl(url, HttpMethod.GET);
	}
	
	@Override
	public String getNodeLocations(NodeType type, String id) {
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_LOCATION_PATH);
		String url = builder.toString();	
		return getJsonStringForUrl(url, HttpMethod.GET);
	}


	@Override
	public String updateNodeAnnotations(NodeType type, String id, String annotationsJson, String etag) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_PATH_ANNOTATIONS);
		String url = builder.toString();		
		return getJsonStringForUrl(url, HttpMethod.PUT, annotationsJson, etag);
	}


	@Override
	public String getNodeAclJSON(NodeType type, String id) {
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_PATH_ACL);
		String url = builder.toString();	
		return getJsonStringForUrl(url, HttpMethod.GET);
	}

	@Override
	public String createAcl(NodeType type, String id, String userGroupId, List<AclAccessType> accessTypes) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_PATH_ACL);
		String url = builder.toString();		
		
		// convert 
		JSONObject obj = new JSONObject();
		try {			
			if(userGroupId != null && accessTypes != null) {
				JSONArray resourceAccessArray = new JSONArray();				
				JSONObject resourceAccessObj = new JSONObject();
				resourceAccessObj.put("groupName", userGroupId);
				JSONArray accessTypeArray = new JSONArray();
				for(AclAccessType accessType : accessTypes) {
					accessTypeArray.put(accessType);
				}
				resourceAccessObj.put("accessType", accessTypeArray);				
				resourceAccessArray.put(resourceAccessObj);				
				obj.put("resourceAccess", resourceAccessArray);
				obj.put("id", id);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String requestJson = obj.toString();

		return getJsonStringForUrl(url, HttpMethod.POST, requestJson);
	}

	@Override
	public String updateAcl(NodeType type, String id, String aclJson, String etag) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_PATH_ACL);
		String url = builder.toString();		
		
		return getJsonStringForUrl(url, HttpMethod.PUT, aclJson, etag);	
	}

	@Override
	public String deleteAcl(NodeType type, String id) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, type);
		builder.append("/" + id);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_PATH_ACL);
		String url = builder.toString();		

		return getJsonStringForUrl(url, HttpMethod.DELETE);		
	}
	
	@Override
	public boolean hasAccess(NodeType resourceType, String resourceId, AclAccessType accessType) {
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, resourceType);
		builder.append("/" + resourceId);	
		builder.append("/" + ServiceUtils.REPOSVC_PATH_HAS_ACCESS);
		builder.append("?accessType="+ accessType);
		String url = builder.toString();	
		HttpMethod method = HttpMethod.GET;

		JSONObject obj = new JSONObject();
		try {
			obj.put("accessType", accessType.toString());			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String requestJson = obj.toString();
		Map<String, String> map = new HashMap<String, String>();
		map.put("accessType", accessType.toString());
		
		// First make sure the service is ready to go.
		validateService();
		
		logger.info(method.toString() + ": " + url + ", accessType="+ accessType);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);
		
		// Make the actual call.
		ResponseEntity<String> response = templateProvider.getTemplate().exchange(url, method, entity, String.class, map);

		if (response.getStatusCode() == HttpStatus.OK) {			
			String responseStr = response.getBody();			
			try {
				JSONObject result = new JSONObject(responseStr);
				if(result.has("result")) {
					return result.getBoolean("result");
				} else {				
					return false;
				}
			} catch (JSONException e) {				
				throw new UnknownError("Malformed response");				
			}
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}		
	}

	@Override
	public String getNodeType(String resourceId) {
		// First make sure the service is ready to go.
		validateService();
		
		// Build up the path
		StringBuilder builder = ServiceUtils.getBaseUrlBuilder(urlProvider, NodeType.ENTITY);
		builder.append("/" + resourceId);
		builder.append("/" + ServiceUtils.REPOSVC_SUFFIX_PATH_TYPE);
		String url = builder.toString();		

		return getJsonStringForUrl(url, HttpMethod.GET);		
	}

	
	/*
	 * Private Methods
	 */
	private String getJsonStringForUrl(String url, HttpMethod method) {
		return getJsonStringForUrl(url, method, null, null);
	}
	
	private String getJsonStringForUrl(String url, HttpMethod method, String entityString) {
		return getJsonStringForUrl(url, method, entityString, null);
	}
		
	private String getJsonStringForUrl(String url, HttpMethod method, String entityString, String etag) {
		// First make sure the service is ready to go.
		validateService();

		String logString = method.toString() + ": " + url;
		if(entityString != null) {
			logString += ", " + "JSON: " + entityString; 
		}		
		logger.info(logString);
		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		if(etag != null) headers.set(DisplayConstants.SERVICE_HEADER_ETAG_KEY, etag);
		if(entityString == null) entityString = "";
		HttpEntity<String> entity = new HttpEntity<String>(entityString, headers);
		RestTemplate restTemplate = templateProvider.getTemplate();
		//ResponseExtractor<String> responseExtractor = new MyResponseExtractor(String.class, restTemplate.getMessageConverters());		

		// check every variable going into the statement such that any NullPointerExceptions are only from NO_CONTENT responses
		if(url == null || method == null || entity == null || restTemplate == null)
			throw new NullPointerException();
		try {
			// Make the actual call.
			ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
			if (response.getStatusCode() == HttpStatus.OK 
					|| response.getStatusCode() == HttpStatus.CREATED
					|| response.getStatusCode() == HttpStatus.NO_CONTENT) {							
				return response.getBody();
			} else {
				throw new UnknownError("Status code:" + response.getStatusCode().value());
			}
		} catch (NullPointerException ex) { 
			// TODO : this is not ideal, but the .execute() method with a custom response extractor does not allow for passing an HttpEntity
			// catch RestTemplate's poor handling of a NO_CONTENT response
			return "";	
		} catch (HttpClientErrorException ex) {
			return ServiceUtils.handleHttpClientErrorException(ex);
		}		
	}
		
	private class MyResponseExtractor extends
			HttpMessageConverterExtractor<String> {
		public MyResponseExtractor(Class<String> responseType,
				List<HttpMessageConverter<?>> messageConverters) {
			super(responseType, messageConverters);
		}

		@Override
		public String extractData(ClientHttpResponse response)
				throws IOException {
			String result;
			if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
				result = super.extractData(response);
			} else {
				result = null;
			}
			return result;
		}
	}
	
}
