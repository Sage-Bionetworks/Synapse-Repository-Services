package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.HttpMediaTypeNotAcceptableException;

/**
 * Unit tests for the Dataset CRUD operations exposed by the DatasetController
 * with XML request and response encoding.
 * <p>
 * 
 * This is just a smattering of tests to check that we can do XML with no code
 * changes other than @XmlRootElement at the top of the model class and adding
 * the model class to the JAXB bean configuration.
 * <p>
 * 
 * TODO parse the XML to a map using XStream
 * http://xstream.codehaus.org/tutorial.html if we get more serious about
 * supporting XML encoding as a first class citizen. Alternatively
 * http://xmlunit.sourceforge.net/ could help.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DatasetControllerXMLTest {

	private static final Logger log = Logger
			.getLogger(DatasetControllerXMLTest.class.getName());

	@Autowired
	private Helpers helper;
	private HttpServlet servlet = null;

	// Dev Note: these ids cannot just be any string, they have to be paresable
	// by our key utility
	private static final String NON_EXISTENT_DATASET_ID = "agR0ZXN0chMLEg1HQUVKRE9EYXRhc2V0GAEM";

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

	/*****************************************************************************************
	 * Happy Case Tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateDataset() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset");
		request.addHeader("Content-Type", "application/xml; charset=UTF-8");
		request
				.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dataset><name>dataset from a unit test</name></dataset>"
						.getBytes("UTF-8"));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
		assertTrue(response
				.getContentAsString()
				.startsWith(
						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dataset>"));
		assertTrue(response.getContentAsString().endsWith("</dataset>"));

		// Clean up from this test 
		String locationHeader = (String) response
				.getHeader(ServiceConstants.LOCATION_HEADER);
		helper.testDeleteJsonEntity(locationHeader);
		
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDataset() throws Exception {
		// Load up a few datasets
		createDatasetHelper();
		String newDatasetId = createDatasetHelper();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset/"
				+ newDatasetId);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		String results = response.getContentAsString();
		// The response should be something like:
		// <?xml version="1.0" encoding="UTF-8"
		// standalone="yes"?><dataset><id>1</id>
		// <name>dataset from a unit test</name></dataset>
		assertTrue(results
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dataset>"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteDataset() throws Exception {
		// Load up a few datasets
		createDatasetHelper();
		String newDatasetId = createDatasetHelper();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset/"
				+ newDatasetId);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
		assertEquals("", response.getContentAsString());
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDatasets() throws Exception {
		// Load up a few datasets
		createDatasetHelper();
		createDatasetHelper();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		String results = response.getContentAsString();
		// The response should be something like:
		// <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
		// <result><paging>
		// <entry><key>previous</key><value>/dataset?offset=1&amp;limit=10</value></entry>
		// <entry><key>next</key><value>/dataset?offset=11&amp;limit=10</value></entry></paging>
		// <results xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		// xsi:type="dataset">
		// <id>1</id><name>dataset from a unit test</name></results>
		// <results xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		// xsi:type="dataset"><id>2</id><name>dataset from a unit
		// test</name></results>
		// <totalNumberOfResults>42</totalNumberOfResults>
		// </result>
		assertTrue(results
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><result>"));
		assertTrue(results.endsWith("</totalNumberOfResults></result>"));
	}

	/*****************************************************************************************
	 * Not Found Tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetNonExistentDataset() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset/"
				+ NON_EXISTENT_DATASET_ID);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		// TODO the authorization piece changes the behavior
		// assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
		// assertTrue(response
		// .getContentAsString()
		// .equals(
		// "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
		// +
		// "<errorResponse><reason>The resource you are attempting to access cannot be found</reason></errorResponse>"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteNonExistentDataset() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("DELETE");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset/"
				+ NON_EXISTENT_DATASET_ID);
		servlet.service(request, response);
		// TODO The authorization piece changes the behavior here
		log.info("Results: " + response.getContentAsString());
		// assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
		// assertTrue(response
		// .getContentAsString()
		// .equals(
		// "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
		// +
		// "<errorResponse><reason>The resource you are attempting to access cannot be found</reason></errorResponse>"));
	}

	/*****************************************************************************************
	 * Miscellaneous Error Test Cases
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#sanityCheck(org.springframework.ui.ModelMap)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSanityCheck() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset/test");
		try {
			servlet.service(request, response);
		} catch (ServletException e) {
			// Unfortunately this exception is uncaught via the
			// DispatcherServlet so that the service does not
			// have an opportunity to handle it. It would be better if were able
			// to handle it like the others
			// and return HttpStatus.BAD_REQUEST
			log.log(Level.INFO,
					"servlet failed to handle exception, but we expected this",
					e);
			// So this test passes because it did what we expected
			// javax.servlet.ServletException: Could not resolve view with name
			// '' in servlet with name 'repository'
			return;
		}
		if (helper.isIntegrationTest()) {
			log.info("Results: " + response.getContentAsString());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response
					.getStatus());
			return;
		}
		fail("something changed!");
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIncorrectContentTypeCreateDataset() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request
				.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dataset><name>dataset from a unit test</name></dataset>"
						.getBytes("UTF-8"));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertTrue(response.getContentAsString().startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
						+ "<errorResponse><reason>"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidXMLCreateDataset() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset");
		request.addHeader("Content-Type", "application/xml; charset=UTF-8");
		// Notice the malformed XML
		request
				.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dataset>dataset from a unit test</name></dataset>"
						.getBytes("UTF-8"));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertTrue(response.getContentAsString().startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
						+ "<errorResponse><reason>"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMissingBodyCreateDataset() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset");
		request.addHeader("Content-Type", "application/xml; charset=UTF-8");
		// No call to request.setContent()
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertTrue(response.getContentAsString().startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
						+ "<errorResponse><reason>"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUnsupportedEncoding() throws Exception {
		// Load up a few datasets
		createDatasetHelper();
		createDatasetHelper();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/javascript");
		request.setRequestURI(helper.getServletPrefix() + "/dataset");
		try {
			servlet.service(request, response);
		} catch (HttpMediaTypeNotAcceptableException e) {
			// Unfortunately this exception is uncaught via the
			// DispatcherServlet so that the service does not
			// have an opportunity to handle it. It is handled by an error page
			// configured via web.xml
			log.log(Level.INFO,
					"servlet failed to handle exception, but we expected this",
					e);
			// So this test passes because it did what we expected
			// org.springframework.web.HttpMediaTypeNotAcceptableException:
			// Could not find acceptable representation
			return;
		}
		if (helper.isIntegrationTest()) {
			log.info("Results: " + response.getContentAsString());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response
					.getStatus());
			return;
		}
		fail("something changed!");
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNoHandler() throws Exception {
		// Load up a few datasets
		createDatasetHelper();
		createDatasetHelper();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/dataset/1/foo");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
		if (!helper.isIntegrationTest()) {
			// Note that an error page configured via web.xml will actually be
			// served
			assertEquals("", response.getContentAsString());
		}
	}

	private String createDatasetHelper() throws Exception {
		JSONObject dataset = helper.testCreateJsonEntity(helper.getServletPrefix() + "/dataset",
				"{\"name\":\"dataset from a unit test\"}");
		return dataset.getString("id");
	}

}
