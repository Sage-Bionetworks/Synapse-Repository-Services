package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Suite for tests not specific to any particular model.
 * <p>
 * 
 * Most of the conditions they test occur either before we enter the controller
 * code or after we leave it. Therefore they do not need to be implemented for
 * each specific controller because the tests don't enter code paths that differ
 * for each.
 * 
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ControllerTest {

	private static final Logger log = Logger
			.getLogger(DatasetControllerTest.class.getName());

	@Autowired
	private Helpers helper;
	private HttpServlet servlet;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		servlet = helper.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.EntityController#getEntitySchema} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSchemas() throws Exception {

		// TODO autogenerate these urls instead of listing them here
		String urls[] = { "/dataset/schema", "/dataset/123/schema",
				"/dataset/123/annotations/schema", "/dataset/123/layer/schema",
				"/dataset/123/layer/456/schema",
				"/dataset/123/layer/456/annotations/schema",
				"/dataset/123/layer/456/locations/schema",
				"/dataset/123/layer/456/preview/schema",
				"/dataset/123/layer/456/previewAsMap/schema",
				"/dataset/123/layer/456/awsS3Location/schema",
				"/dataset/123/layer/456/awsEBSLocation/schema",
				"/dataset/123/layer/456/sageLocation/schema", "/query/schema", };

		List<String> testCases = Arrays.asList(urls); // UrlHelpers.getAllEntityUrlPrefixes();
		for (String url : testCases) {
			JSONObject schema = helper.testGetJsonObject(helper
					.getServletPrefix()
					+ url);
			assertNotNull(schema);
			assertNotNull(schema.getString("type"));
			assertNotNull(schema.getJSONObject("properties"));
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.EntityController#createEntity} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidJsonCreateEntity() throws Exception {

		Collection<String> urls = UrlHelpers.getAllEntityUrlPrefixes();
		for (String url : urls) {

			url = (null == UrlHelpers.getParentForChildUrl(url)) ? helper
					.getServletPrefix()
					+ url : helper.getServletPrefix()
					+ UrlHelpers.getParentForChildUrl(url) + "/123" + url;

			// Notice the missing quotes around the key
			JSONObject results = helper.testCreateJsonEntityShouldFail(url,
					"{name:\"bad json from a unit test\"}",
					HttpStatus.BAD_REQUEST);

			// The response should be something like: {"reason":"Could not read
			// JSON: Unexpected character
			// ('t' (code 116)): was expecting double-quote to start field
			// name\n at [Source:
			// org.springframework.mock.web.DelegatingServletInputStream@11e3c2c6;
			// line: 1, column: 3];
			// nested exception is org.codehaus.jackson.JsonParseException:
			// Unexpected character
			// ('n' (code 116)): was expecting double-quote to start field
			// name\n at [Source:
			// org.springframework.mock.web.DelegatingServletInputStream@11e3c2c6;
			// line: 1, column: 3]"}
			int index = results.getString("reason").indexOf("Could not read JSON: Unexpected character");
			assertTrue("Testing " + url, index >= 0);
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.EntityController#createEntity} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMissingBodyCreateEntity() throws Exception {

		Collection<String> urls = UrlHelpers.getAllEntityUrlPrefixes();
		for (String url : urls) {

			url = (null == UrlHelpers.getParentForChildUrl(url)) ? helper
					.getServletPrefix()
					+ url : helper.getServletPrefix()
					+ UrlHelpers.getParentForChildUrl(url) + "/123" + url;

			MockHttpServletRequest request = new MockHttpServletRequest();
			MockHttpServletResponse response = new MockHttpServletResponse();
			request.setMethod("POST");
			request.addHeader("Accept", "application/json");
			request.setRequestURI(url);
			request
					.addHeader("Content-Type",
							"application/json; charset=UTF-8");
			// No call to request.setContent()
			servlet.service(request, response);
			log.info("Results: " + response.getContentAsString());
			assertEquals("Testing " + url, HttpStatus.BAD_REQUEST.value(),
					response.getStatus());
			JSONObject results = new JSONObject(response.getContentAsString());
			// The response should be something like:
			// {"reason":"No content to map to Object due to end of input"}
			int index = results.getString("reason").indexOf("No content to map to Object due to end of input");
			assertTrue("Testing " + url, index >= 0);
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.EntityController#createEntity} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMissingBodyUpdateEntity() throws Exception {

		Collection<String> urls = UrlHelpers.getAllEntityUrlPrefixes();
		for (String url : urls) {

			url = (null == UrlHelpers.getParentForChildUrl(url)) ? helper
					.getServletPrefix()
					+ url : helper.getServletPrefix()
					+ UrlHelpers.getParentForChildUrl(url) + "/123" + url;

			MockHttpServletRequest request = new MockHttpServletRequest();
			MockHttpServletResponse response = new MockHttpServletResponse();
			request.setMethod("PUT");
			request.addHeader("Accept", "application/json");
			request.setRequestURI(url + "/1");
			request
					.addHeader("Content-Type",
							"application/json; charset=UTF-8");
			request.addHeader(ServiceConstants.ETAG_HEADER, "123");
			// No call to request.setContent()
			servlet.service(request, response);
			log.info("Results: " + response.getContentAsString());
			assertEquals("Testing " + url, HttpStatus.BAD_REQUEST.value(),
					response.getStatus());
			JSONObject results = new JSONObject(response.getContentAsString());
			// The response should be something like:
			// {"reason":"No content to map to Object due to end of input"}
			int index = results.getString("reason").indexOf("No content to map to Object due to end of input");
			assertTrue("Testing " + url, index >= 0);
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.EntityController#updateEntity} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateEntityMissingEtag() throws Exception {
		Collection<String> urls = UrlHelpers.getAllEntityUrlPrefixes();
		for (String url : urls) {

			url = (null == UrlHelpers.getParentForChildUrl(url)) ? helper
					.getServletPrefix()
					+ url : helper.getServletPrefix()
					+ UrlHelpers.getParentForChildUrl(url) + "/123" + url;

			MockHttpServletRequest request = new MockHttpServletRequest();
			MockHttpServletResponse response = new MockHttpServletResponse();
			request.setMethod("PUT");
			request.addHeader("Accept", "application/json");
			request.setRequestURI(url + "/1");
			request
					.addHeader("Content-Type",
							"application/json; charset=UTF-8");
			request
					.setContent("{\"id\": 1, \"text\":\"updated dataset from a unit test\"}"
							.getBytes("UTF-8"));
			servlet.service(request, response);
			log.info("Results: " + response.getContentAsString());
			assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
			JSONObject results = new JSONObject(response.getContentAsString());
			// The response should be something like: {"reason":"Failed to
			// invoke handler method [public
			// org.sagebionetworks.repo.model.Dataset
			// org.sagebionetworks.repo.web.controller.DatasetController.updateDataset
			// (java.lang.Long,java.lang.String,org.sagebionetworks.repo.model.Dataset)
			// throws org.sagebionetworks.repo.web.NotFoundException]; nested
			// exception is java.lang.IllegalStateException:
			// Missing header 'Etag' of type [java.lang.String]"}
			assertTrue("Testing " + url, results.getString("reason").matches(
					"(?s).*Missing header 'ETag'.*"));
		}
	}
}
