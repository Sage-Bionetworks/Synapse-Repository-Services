package org.sagebionetworks.web.server.servlet;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.services.ProjectService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.Project;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
public class ProjectServiceImpl extends RemoteServiceServlet implements
		ProjectService {

	private static Logger logger = Logger.getLogger(ProjectServiceImpl.class
			.getName());

	public static final String KEY_PROJECT_ID = "idKey";
	public static final String PATH_PROJECT = "project/{"+KEY_PROJECT_ID+"}";	
	
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
	public Project getProject(String id) {		
		// First make sure the service is ready to go.
		validateService();
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl() + "/");
		builder.append(PATH_PROJECT);
		// the values to the keys
		Map<String, String> map = new HashMap<String, String>();
		map.put(KEY_PROJECT_ID, id);
		String url = builder.toString();
		logger.info("GET: " + url);
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		// If the user data is stored in a cookie, then fetch it and the session token to the header.
		UserDataProvider.addUserDataToHeader(this.getThreadLocalRequest(), headers);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		// Make the actual call.
		ResponseEntity<Project> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, Project.class, map);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}


}
