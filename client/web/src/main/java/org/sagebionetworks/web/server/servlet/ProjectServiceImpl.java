package org.sagebionetworks.web.server.servlet;

import java.util.Date;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.services.ProjectService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.Project;

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

		Project temp = new Project();
		temp.setName("Default Project Name");
		temp.setDescription("Default Project description. Irony: Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of de Finibus Bonorum et Malorum (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance. The first line of Lorem Ipsum, Lorem ipsum dolor sit amet.., comes from a line in section 1.10.32. The standard chunk of Lorem Ipsum used since the 1500s is reproduced below for those interested. Sections 1.10.32 and 1.10.33 from de Finibus Bonorum et Malorum by Cicero are also reproduced in their exact original form, accompanied by English versions from the 1914 translation by H. Rackham.");
		temp.setId("1");
		temp.setCreationDate(new Date());
		temp.setCreator("Dave");		
		temp.setStatus("Active");		
		return temp;
		
//		// First make sure the service is ready to go.
//		validateService();
//		// Build up the path
//		StringBuilder builder = new StringBuilder();
//		builder.append(urlProvider.getBaseUrl());
//		builder.append(PATH_PROJECT);
//		// the values to the keys
//		Map<String, String> map = new HashMap<String, String>();
//		map.put(KEY_PROJECT_ID, id);
//		String url = builder.toString();
//		logger.info("GET: " + url);
//		// Setup the header
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		HttpEntity<String> entity = new HttpEntity<String>("", headers);
//		// Make the actual call.
//		ResponseEntity<Project> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, Project.class, map);
//
//		if (response.getStatusCode() == HttpStatus.OK) {
//			return response.getBody();
//		} else {
//			// TODO: better error handling
//			throw new UnknownError("Status code:"
//					+ response.getStatusCode().value());
//		}
	}


}
