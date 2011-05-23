package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * This is a an integration test for the BasicController.
 * 
 * @author jmhill
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DefaultControllerAutowiredTest {

	// Used for cleanup
	@Autowired
	GenericEntityController entityController;

	static private Log log = LogFactory
			.getLog(DefaultControllerAutowiredTest.class);

	private ObjectMapper objectMapper = new ObjectMapper();

	private static HttpServlet dispatchServlet;

	private List<String> toDelete;

	@Before
	public void before() {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		// Map test objects to their urls

	}

	@After
	public void after() throws UnauthorizedException {
		if (entityController != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					entityController.deleteEntity(
							AuthUtilConstants.ANONYMOUS_USER_ID, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}

	@BeforeClass
	public static void beforeClass() throws ServletException {
		// Setup the servlet once
		// Create a Spring MVC DispatcherServlet so that we can test our URL
		// mapping, request format, response format, and response status
		// code.
		MockServletConfig servletConfig = new MockServletConfig("repository");
		servletConfig.addInitParameter("contextConfigLocation",
				"classpath:test-context.xml");
		dispatchServlet = new DispatcherServlet();
		dispatchServlet.init(servletConfig);
	}

	@Test
	public void testCreate() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = callCreate(UrlHelpers.PROJECT, project);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		assertNotNull(clone.getEtag());
	}

	@Test
	public void testGetById() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = callCreate(UrlHelpers.PROJECT, project);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		assertNotNull(clone.getEtag());

		// Now get the project object
		Project fromGet = callGetObject(UrlHelpers.PROJECT, Project.class,
				clone.getId());
		assertNotNull(fromGet);
		// Should match the clone
		assertEquals(clone, fromGet);
	}

	@Test
	public void testGetList() throws Exception {
		// Create a project
		int number = 3;
		for (int i = 0; i < number; i++) {
			Project project = new Project();
			project.setName("project" + i);
			Project clone = callCreate(UrlHelpers.PROJECT, project);
		}
		// Try with all default values
		PaginatedResults<Project> result = callAllGetObject(UrlHelpers.PROJECT,
				Project.class, null, null, null, null);
		assertNotNull(result);
		assertEquals(number, result.getTotalNumberOfResults());
		assertNotNull(result.getResults());
		assertEquals(number, result.getResults().size());

		// Try with a value in each slot
		result = callAllGetObject(UrlHelpers.PROJECT, Project.class, 2, 1,
				"name", true);
		assertNotNull(result);
		assertEquals(number, result.getTotalNumberOfResults());
		assertNotNull(result.getResults());
		assertEquals(1, result.getResults().size());
		assertNotNull(result.getResults().get(0));
	}

	@Test(expected = NotFoundException.class)
	public void testDelete() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = callCreate(UrlHelpers.PROJECT, project);
		callDeleteObject(UrlHelpers.PROJECT, clone.getId());
		// This should throw an exception
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		entityController.getEntity(AuthUtilConstants.ANONYMOUS_USER_ID,
				clone.getId(), mockRequest, Project.class);
	}
	
	@Test
	public void testGetSchema() throws Exception{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = callCreate(UrlHelpers.PROJECT, project);
		// Get the schema
		String schema = callGetSchema(UrlHelpers.PROJECT, clone.getId());
		assertNotNull(schema);
		// Try without the id
		schema = callGetSchema(UrlHelpers.PROJECT, null);
		assertNotNull(schema);
		log.info("Project schema: "+schema);
	}

	/**
	 * Create an object
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param entity
	 * @return
	 * @throws Exception
	 */
	private <T extends Base> T callCreate(String requestUrl, T entity)
			throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		request.setParameter(AuthUtilConstants.USER_ID_PARAM,
				AuthUtilConstants.ANONYMOUS_USER_ID);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, entity);
		String body = out.toString();
		request.setContent(body.getBytes("UTF-8"));
		log.info("About to send: " + body);
		dispatchServlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("Reason: " + response.getErrorMessage(),
				HttpStatus.CREATED.value(), response.getStatus());
		@SuppressWarnings("unchecked")
		T returnedEntity = (T) objectMapper.readValue(
				response.getContentAsString(), entity.getClass());
		if (returnedEntity != null) {
			// Make sure this gets deleted
			toDelete.add(returnedEntity.getId());
		}
		return returnedEntity;
	}

	/**
	 * Get an object using an id.
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private <T extends Base> T callGetObject(String requestUrl,
			Class<? extends T> clazz, String id) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl + "/" + id);
		request.setParameter(AuthUtilConstants.USER_ID_PARAM,
				AuthUtilConstants.ANONYMOUS_USER_ID);
		dispatchServlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("Reason: " + response.getErrorMessage(),
				HttpStatus.OK.value(), response.getStatus());
		return (T) objectMapper.readValue(response.getContentAsString(), clazz);
	}

	/**
	 * Get all objects of type.
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private <T extends Base> PaginatedResults<T> callAllGetObject(
			String requestUrl, Class<? extends T> clazz, Integer offset,
			Integer limit, String sort, Boolean ascending) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		if (offset != null) {
			request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM,
					offset.toString());
		}
		if (limit != null) {
			request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM,
					limit.toString());
		}
		if (sort != null) {
			request.setParameter(ServiceConstants.SORT_BY_PARAM, sort);
		}
		if (ascending != null) {
			request.setParameter(ServiceConstants.ASCENDING_PARAM,
					ascending.toString());
		}
		request.setRequestURI(requestUrl);
		request.setParameter(AuthUtilConstants.USER_ID_PARAM,
				AuthUtilConstants.ANONYMOUS_USER_ID);
		dispatchServlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("Reason: " + response.getErrorMessage(),
				HttpStatus.OK.value(), response.getStatus());
		return objectMapper.readValue(response.getContentAsString(),
				PaginatedResults.class);
	}

	/**
	 * Get an object using an id.
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private void callDeleteObject(String requestUrl, String id)
			throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl + "/" + id);
		request.setParameter(AuthUtilConstants.USER_ID_PARAM,
				AuthUtilConstants.ANONYMOUS_USER_ID);
		dispatchServlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("Reason: " + response.getErrorMessage(),
				HttpStatus.NO_CONTENT.value(), response.getStatus());
	}

	/**
	 * Get the schema
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private String callGetSchema(String requestUrl, String id) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		if (id != null) {
			request.setRequestURI(requestUrl + "/" + id + UrlHelpers.SCHEMA);
		} else {
			request.setRequestURI(requestUrl + UrlHelpers.SCHEMA);
		}
		request.setParameter(AuthUtilConstants.USER_ID_PARAM,
				AuthUtilConstants.ANONYMOUS_USER_ID);
		dispatchServlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("Reason: " + response.getErrorMessage(),
				HttpStatus.OK.value(), response.getStatus());
		return response.getContentAsString();
	}

}
