package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.URLEncoder;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.json.JSONObject;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * Test helper class to consolidate some generic testing code
 * 
 * @author deflaux
 *
 */
public class Helpers {

    private static final Logger log = Logger
    .getLogger(Helpers.class.getName());

    private static final LocalServiceTestHelper datastoreHelper =
        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    private DispatcherServlet servlet = null;
    
    /**
     * Setup up our mock datastore and servlet
     * 
     * @return the servlet
     * @throws ServletException
     */
    public DispatcherServlet setUp() throws ServletException {
        datastoreHelper.setUp();

        // Create a Spring MVC DispatcherServlet so that we can test our URL
        // mapping, request format, response format, and response status code.
        MockServletConfig servletConfig = new MockServletConfig("repository");
        servletConfig.addInitParameter("contextConfigLocation",
                                       "classpath:repository-context.xml,classpath:repository-servlet.xml");
        servlet = new DispatcherServlet();
        servlet.init(servletConfig);
        
        return servlet;
    }
    
    /**
     * Teardown our datastore, deleting all items
     */
    public void tearDown() {
        datastoreHelper.tearDown();
    }
    
    /**
     * Creation of JSON entities
     * 
     * @param requestUrl
     * @param jsonRequestContent
     * @return the entity created
     * @throws Exception 
     */
    public JSONObject testCreateJsonEntity(String requestUrl, String jsonRequestContent) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI(requestUrl);
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent(jsonRequestContent.getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        
        // Check default properties
        assertNotNull(results.getString("id"));
        assertNotNull(results.getString("uri"));
        assertNotNull(results.getString("etag"));
        
        // Check our response headers
        String etagHeader = (String) response.getHeader(ServiceConstants.ETAG_HEADER);
        assertNotNull(etagHeader);
        assertEquals(etagHeader, results.getString("etag"));
        String locationHeader = (String) response.getHeader(ServiceConstants.LOCATION_HEADER);
        assertEquals(locationHeader, requestUrl + "/" + URLEncoder.encode(results.getString("id"), "UTF-8"));
        assertEquals(locationHeader, results.getString("uri"));
        
        return results;
    }
    
    /**
     * @param requestUrl
     * @return the jsonRequestContent object holding the entity
     * @throws Exception
     */
    public JSONObject testGetJsonEntity(String requestUrl) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI(requestUrl);
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());

        // Check default properties
        assertNotNull(results.getString("id"));
        assertNotNull(results.getString("uri"));
        assertNotNull(results.getString("etag"));
        
        // Check our response headers
        String etagHeader = (String) response.getHeader(ServiceConstants.ETAG_HEADER);
        assertNotNull(etagHeader);
        assertEquals(etagHeader, results.getString("etag"));
        
        return results;
    }

    /**
     * @param jsonEntity
     * @return the json object holding the updated entity
     * @throws Exception 
     */
    public JSONObject testUpdateJsonEntity(JSONObject jsonEntity) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("PUT");
        request.addHeader("Accept", "application/json");
        request.addHeader(ServiceConstants.ETAG_HEADER, jsonEntity.getString("etag"));
        request.setRequestURI(jsonEntity.getString("uri"));
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent(jsonEntity.toString().getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());

        // Check default properties
        assertNotNull(results.getString("id"));
        assertNotNull(results.getString("uri"));
        assertNotNull(results.getString("etag"));

        // Check our response headers
        String etagHeader = (String) response.getHeader(ServiceConstants.ETAG_HEADER);
        assertNotNull(etagHeader);
        assertEquals(etagHeader, results.getString("etag"));

        // Make sure we got an updated etag
        assertFalse(etagHeader.equals(jsonEntity.getString("etag")));

        return results;
    }
    
    /**
     * @param requestUrl
     * @throws Exception
     */
    public void testDeleteJsonEntity(String requestUrl) throws Exception {
    
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("DELETE");
        request.addHeader("Accept", "application/json");
        request.setRequestURI(requestUrl);
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
        assertEquals("", response.getContentAsString());
        
        testGetJsonEntityShouldFail(requestUrl, HttpStatus.NOT_FOUND);
    }

    /**
     * @param requestUrl
     * @return the response Json entity
     * @throws Exception
     */
    public JSONObject testGetJsonEntities(String requestUrl) throws Exception {
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI(requestUrl);
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        
        // Check that the response has the correct structure
        assertNotNull(results.getInt("totalNumberOfResults"));
        assertNotNull(results.getJSONArray("results"));
        assertNotNull(results.getJSONObject("paging"));
        assertNotNull(results.getJSONObject("paging").getString(PaginatedResults.PREVIOUS_PAGE_FIELD));
        assertNotNull(results.getJSONObject("paging").getString(PaginatedResults.NEXT_PAGE_FIELD));

        return results;
    }    
    /**
     * @param requestUrl
     * @param jsonRequestContent
     * @param status 
     * @return the error entity
     * @throws Exception
     */
    public JSONObject testCreateJsonEntityShouldFail(String requestUrl, String jsonRequestContent, HttpStatus status) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI(requestUrl);
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent(jsonRequestContent.getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertFalse(HttpStatus.OK.equals(response.getStatus()));
        assertEquals(status.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        assertNotNull(results.getString("reason"));
        
        return results;
    }
    
    /**
     * @param requestUrl
     * @param status
     * @return the error entity
     * @throws Exception
     */
    public JSONObject testGetJsonEntityShouldFail(String requestUrl, HttpStatus status) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI(requestUrl); 
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertFalse(HttpStatus.OK.equals(response.getStatus()));
        assertEquals(status.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        assertNotNull(results.getString("reason"));        

        return results;
    }

    /**
     * @param jsonEntity
     * @param status
     * @return the error entity
     * @throws Exception
     */
    public JSONObject testUpdateJsonEntityShouldFail(JSONObject jsonEntity, HttpStatus status) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("PUT");
        request.addHeader("Accept", "application/json");
        request.addHeader(ServiceConstants.ETAG_HEADER, jsonEntity.getString("etag"));
        request.setRequestURI(jsonEntity.getString("uri"));
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent(jsonEntity.toString().getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertFalse(HttpStatus.OK.equals(response.getStatus()));
        assertEquals(status.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        assertNotNull(results.getString("reason"));        

        return results;
    }
 
        /**
     * @param requestUrl
     * @param status
     * @return the error entity
     * @throws Exception
     */
    public JSONObject testDeleteJsonEntityShouldFail(String requestUrl, HttpStatus status) throws Exception {
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("DELETE");
        request.addHeader("Accept", "application/json");
        request.setRequestURI(requestUrl);
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertFalse(HttpStatus.OK.equals(response.getStatus()));
        assertEquals(status.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        assertNotNull(results.getString("reason"));
        
        return results;
    }    
    
    
}
