/**
 *
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Message;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * Unit tests for the Dataset CRUD operations exposed by the DatasetController with JSON
 * request and response encoding.<p>
 * 
 * TODO this patient is lying open on the surgery table, don't bother CR-ing this yet
 * 
 * This unit test suite for the repository Google App Engine service
 * uses a local instance of DataStoreService.
 * <p>
 * The following unit tests are written at the servlet layer to test
 * bugs in our URL mapping, request format, response format, and
 * response status code.
 * <p>
 * See src/main/webapp/WEB-INF/repository-servlet.xml for servlet configuration
 * <p>
 * See src/main/webapp/WEB-INF/repository-context.xml for application
 * configuration.  Later on we may wish to have a test-specific version
 * of this file.
 *
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:repository-context.xml","classpath:repository-servlet.xml"})
public class DatasetControllerTest {

    private static final Logger log = Logger
    .getLogger(DatasetControllerTest.class.getName());

    private static final LocalServiceTestHelper datastoreHelper =
        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    private DispatcherServlet servlet = null;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        datastoreHelper.setUp();

        // Create a Spring MVC DispatcherServlet so that we can test our URL
        // mapping, request format, response format, and response status code.
        MockServletConfig servletConfig = new MockServletConfig("repository");
        servletConfig.addInitParameter("contextConfigLocation",
                                       "classpath:repository-context.xml,classpath:repository-servlet.xml");
        servlet = new DispatcherServlet();
        servlet.init(servletConfig);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        datastoreHelper.tearDown();
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#sanityCheck(org.springframework.ui.ModelMap)}.
     * @throws Exception
     */
    @Test
    public void testSanityCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/dataset/test");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals("we got 200 OK", 200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"hello":"REST for Datasets rocks"}
        assertEquals("REST for Datasets rocks", results.getString("hello"));
    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities(Integer, Integer, HttpServletRequest)}.
//     * @throws Exception
//     */
//    @Test
//    public void testGetDatasets() throws Exception {
//        // Load up a few datasets
//        testCreateDataset();
//        testCreateDataset();
//
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("GET");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset");
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.OK.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be:
//        //  {"results":[{"id":1,"text":"dataset from a unit test"},{"id":2,
//        //   "text":"dataset from a unit test"}],"totalNumberOfResults":42,
//        //   "paging":{"previous":"/dataset?offset=1&limit=10","next":"/dataset?offset=11&limit=10"}}
//        assertNotNull(results.getInt("totalNumberOfResults"));
//        assertEquals(2, results.getJSONArray("results").length());
//        assertEquals("/dataset?offset=1&limit=10", results.getJSONObject("paging").getString(PaginatedResults.PREVIOUS_PAGE_FIELD));
//        assertEquals("/dataset?offset=11&limit=10", results.getJSONObject("paging").getString(PaginatedResults.NEXT_PAGE_FIELD));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities(Integer, Integer, HttpServletRequest)}.
//     * @throws Exception
//     */
//    @Test
//    public void testGetDatasetsBadLimit() throws Exception {
//        // Load up a few datasets
//        testCreateDataset();
//        testCreateDataset();
//
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("GET");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset");
//        request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, "1");
//        request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, "0");
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be:
//        assertNotNull(results.getString("reason"));
//    }
//    
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities(Integer, Integer, HttpServletRequest)}.
//     * @throws Exception
//     */
//    @Test
//    public void testGetDatasetsBadOffset() throws Exception {
//        // Load up a few datasets
//        testCreateDataset();
//        testCreateDataset();
//
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("GET");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset");
//        request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, "-5");
//        request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, "0");
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be:
//        assertNotNull(results.getString("reason"));
//    }
//    
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntity(java.lang.Long)}.
//     * @throws Exception
//     */
//    @Test
//    public void testGetDataset() throws Exception {
//        // Load up a few datasets
//        testCreateDataset();
//        testCreateDataset();
//
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("GET");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset/2");
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.OK.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be: {"id":1,"text":"dataset from a unit test"}
//        assertEquals(2, results.getInt("id"));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntity(java.lang.Long)}.
//     * @throws Exception
//     */
//    @Test
//    public void testGetNonExistentDataset() throws Exception {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//
//        request.setMethod("GET");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset/22");
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be: {"reason":"no dataset with id 22 exists"}
//        assertNotNull(results.getString("reason"));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntity(java.lang.Long)}.
//     * @throws Exception
//     */
//    @Test
//    public void testGetDatasetBadId() throws Exception {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//
//        request.setMethod("GET");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset/thisShouldBeANumber");
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be: {"reason":"Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long';
//        // nested exception is java.lang.NumberFormatException: For input string: \"thisShouldBeANumber\""}
//        assertNotNull(results.getString("reason"));
//    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity(Dataset)}.
     * @throws Exception
     */
    @Test
    public void testCreateDataset() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        String requestURI = "/dataset";
        request.setRequestURI(requestURI);
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent("{\"name\":\"DeLiver\"}".getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be something like: {"id":1,"text":"dataset from a unit test"}

        // Check required properties
        assertEquals("DeLiver", results.getString("name"));

        // Check immutable system-defined properties
        assertNotNull(results.get("id"));
        assertNotNull(results.get("creationDate"));
        
        // Check that optional properties that receive default values
        assertNotNull(results.get("version"));
        
        // Check our response headers
        assertNotNull(response.getHeader(ServiceConstants.ETAG_HEADER));
        assertEquals(requestURI + "/" + results.get("id"), response.getHeader(ServiceConstants.LOCATION_HEADER));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity(Dataset)}.
     * @throws Exception
     */
    @Test
    public void testInvalidModelCreateDataset() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/dataset");
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent("{\"name\": \"DeLiver\", \"BOGUS\":\"this does not match our model object\"}".getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be something like: {"reason":"Unrecognized field \"BOGUS\" 
        // (Class org.sagebionetworks.repo.model.Dataset), not marked as ignorable\n at 
        // [Source: org.springframework.mock.web.DelegatingServletInputStream@2501e081; line: 1, column: 19]"}
        assertNotNull(results.getString("reason"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity(Dataset)}.
     * @throws Exception
     */
    @Test
    public void testMissingRequireFieldCreateDataset() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/dataset");
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent("{\"version\": \"1.0.0\"}".getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be something like: {"reason":"'name' is a require property for Dataset"}
        assertNotNull(results.getString("reason"));
    }
    
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity(Dataset)}.
//     * @throws Exception
//     */
//    @Test
//    public void testInvalidJsonCreateDataset() throws Exception {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("POST");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset");
//        request.addHeader("Content-Type", "application/json; charset=UTF-8");
//        // Notice the missing quotes around the key
//        request.setContent("{text:\"dataset from a unit test\"}".getBytes("UTF-8"));
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be something like:  {"reason":"Could not read JSON: Unexpected character
//        // ('t' (code 116)): was expecting double-quote to start field name\n at [Source:
//        // org.springframework.mock.web.DelegatingServletInputStream@11e3c2c6; line: 1, column: 3];
//        // nested exception is org.codehaus.jackson.JsonParseException: Unexpected character
//        // ('t' (code 116)): was expecting double-quote to start field name\n at [Source:
//        // org.springframework.mock.web.DelegatingServletInputStream@11e3c2c6; line: 1, column: 3]"}
//        assertNotNull(results.getString("reason"));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity(Dataset)}.
//     * @throws Exception
//     */
//    @Test
//    public void testMissingBodyCreateDataset() throws Exception {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("POST");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset");
//        request.addHeader("Content-Type", "application/json; charset=UTF-8");
//        // No call to request.setContent()
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be something like: {"reason":"No content to map to Object due to end of input"}
//        assertNotNull(results.getString("reason"));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity(java.lang.Long)}.
//     * @throws Exception
//     */
//    @Test
//    public void testDeleteDataset() throws Exception {
//        // Load up a few datasets
//        testCreateDataset();
//        testCreateDataset();
//
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("DELETE");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset/2");
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
//        assertEquals("", response.getContentAsString());
//    }
//
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity(java.lang.Long)}.
//     * @throws Exception
//     */
//    @Test
//    public void testDeleteNonExistentDataset() throws Exception {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//
//        request.setMethod("DELETE");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset/2");
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be: {"reason":"no dataset with id 22 exists"}
//        assertNotNull(results.getString("reason"));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity(Long, Integer, Dataset)}.
//     * @throws Exception
//     */
//    @Test
//    public void testUpdateDataset() throws Exception {
//        // Make one new dataset
//        testCreateDataset();
//
//        // Get that dataset
//        MockHttpServletRequest getMethodRequest = new MockHttpServletRequest();
//        MockHttpServletResponse getMethodResponse = new MockHttpServletResponse();
//        getMethodRequest.setMethod("GET");
//        getMethodRequest.addHeader("Accept", "application/json");
//        getMethodRequest.setRequestURI("/dataset/1");
//        servlet.service(getMethodRequest, getMethodResponse);
//        log.info("GET Results: " + getMethodResponse.getContentAsString());
//        JSONObject getResults = new JSONObject(getMethodResponse.getContentAsString());
//        Integer etag = (Integer) getMethodResponse.getHeader(ServiceConstants.ETAG_HEADER);
//        assertNotNull(etag);
//
//        // Modify that dataset
//        getResults.put("text", "updated dataset from a unit test");
//
//        MockHttpServletRequest putMethodRequest = new MockHttpServletRequest();
//        MockHttpServletResponse putMethodResponse = new MockHttpServletResponse();
//        putMethodRequest.setMethod("PUT");
//        putMethodRequest.addHeader("Accept", "application/json");
//        putMethodRequest.addHeader(ServiceConstants.ETAG_HEADER, etag);
//        putMethodRequest.setRequestURI("/dataset/1");
//        putMethodRequest.addHeader("Content-Type", "application/json; charset=UTF-8");
//        putMethodRequest.setContent(getResults.toString().getBytes("UTF-8"));
//        servlet.service(putMethodRequest, putMethodResponse);
//        log.info("Results: " + putMethodResponse.getContentAsString());
//        assertEquals(HttpStatus.OK.value(), putMethodResponse.getStatus());
//        JSONObject results = new JSONObject(putMethodResponse.getContentAsString());
//        // The response should be something like: {"id":1,"text":"updated dataset from a unit test"}
//        assertTrue(0 < results.getInt("id"));
//        assertEquals("updated dataset from a unit test", results.getString("text"));
//        Integer updatedEtag = (Integer) putMethodResponse.getHeader(ServiceConstants.ETAG_HEADER);
//        assertNotNull(updatedEtag);
//
//        // Make sure we got an updated etag
//        assertFalse(etag.equals(updatedEtag));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity(Long, Integer, Dataset)}.
//     * @throws Exception
//     */
//    @Test
//    public void testUpdateDatasetConflict() throws Exception {
//        // Make one new dataset
//        testCreateDataset();
//
//        // Get that dataset
//        MockHttpServletRequest getMethodRequest = new MockHttpServletRequest();
//        MockHttpServletResponse getMethodResponse = new MockHttpServletResponse();
//        getMethodRequest.setMethod("GET");
//        getMethodRequest.addHeader("Accept", "application/json");
//        getMethodRequest.setRequestURI("/dataset/1");
//        servlet.service(getMethodRequest, getMethodResponse);
//        log.info("GET Results: " + getMethodResponse.getContentAsString());
//        JSONObject getResults = new JSONObject(getMethodResponse.getContentAsString());
//        Integer etag = (Integer) getMethodResponse.getHeader(ServiceConstants.ETAG_HEADER);
//
//        // Someone else updates it
//        testUpdateDataset();
//
//        // Modify the dataset we got earlier
//        getResults.put("text", "conflicting dataset from a unit test");
//
//        MockHttpServletRequest putMethodRequest = new MockHttpServletRequest();
//        MockHttpServletResponse putMethodResponse = new MockHttpServletResponse();
//        putMethodRequest.setMethod("PUT");
//        putMethodRequest.addHeader("Accept", "application/json");
//        putMethodRequest.addHeader(ServiceConstants.ETAG_HEADER, etag);
//        putMethodRequest.setRequestURI("/dataset/1");
//        putMethodRequest.addHeader("Content-Type", "application/json; charset=UTF-8");
//        putMethodRequest.setContent(getResults.toString().getBytes("UTF-8"));
//        servlet.service(putMethodRequest, putMethodResponse);
//        log.info("Results: " + putMethodResponse.getContentAsString());
//        assertEquals(HttpStatus.PRECONDITION_FAILED.value(), putMethodResponse.getStatus());
//        JSONObject results = new JSONObject(putMethodResponse.getContentAsString());
//        // The response should be something like:
//        assertNotNull(results.getString("reason"));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity(Long, Integer, Dataset)}.
//     * @throws Exception
//     */
//    @Test
//    public void testUpdateDatasetMissingEtag() throws Exception {
//        // Make one new dataset
//        testCreateDataset();
//
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("PUT");
//        request.addHeader("Accept", "application/json");
//        request.setRequestURI("/dataset/1");
//        request.addHeader("Content-Type", "application/json; charset=UTF-8");
//        request.setContent("{\"id\": 1, \"text\":\"updated dataset from a unit test\"}".getBytes("UTF-8"));
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be something like:  {"reason":"Failed to invoke handler method [public
//        // org.sagebionetworks.repo.model.Dataset org.sagebionetworks.repo.web.controller.DatasetController.updateDataset
//        // (java.lang.Long,java.lang.String,org.sagebionetworks.repo.model.Dataset)
//        // throws org.sagebionetworks.repo.web.NotFoundException]; nested exception is java.lang.IllegalStateException:
//        // Missing header 'Etag' of type [java.lang.String]"}
//        assertNotNull(results.getString("reason"));
//    }
//
//    /**
//     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity(Long, Integer, Dataset)}.
//     * @throws Exception
//     */
//    @Test
//    public void testUpdateNonExistentDataset() throws Exception {
//        // Make one new dataset
//        testCreateDataset();
//
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        request.setMethod("PUT");
//        request.addHeader("Accept", "application/json");
//        request.addHeader("Etag", 123);
//        request.setRequestURI("/dataset/100");
//        request.addHeader("Content-Type", "application/json; charset=UTF-8");
//        request.setContent("{\"id\": 100, \"text\":\"updated dataset from a unit test\"}".getBytes("UTF-8"));
//        servlet.service(request, response);
//        log.info("Results: " + response.getContentAsString());
//        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
//        // The response should be something like: {"reason":"no dataset with id 100 exists"}
//        assertNotNull(results.getString("reason"));
//    }
}
