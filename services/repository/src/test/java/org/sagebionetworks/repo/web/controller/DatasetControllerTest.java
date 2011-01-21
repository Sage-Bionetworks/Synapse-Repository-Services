/**
 *
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

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

    private static final Logger log = Logger.getLogger(DatasetControllerTest.class.getName());
    private Helpers helper = new Helpers();
    private DispatcherServlet servlet;
    
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
    
    /*************************************************************************************************************************
     * Happy case tests
     */

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

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}.
     * @throws Exception
     */
    @Test
    public void testCreateDataset() throws Exception {
        JSONObject results = helper.testCreateJsonEntity("/dataset", "{\"name\":\"DeLiver\"}");
        // Check required properties
        assertEquals("DeLiver", results.getString("name"));
        // Check immutable system-defined properties
        assertNotNull(results.get("creationDate"));
        
        // Check that optional properties that receive default values
        assertNotNull(results.get("version"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntity}.
     * @throws Exception
     */
    @Test
    public void testGetDataset() throws Exception {
        // Load up a few datasets
        helper.testCreateJsonEntity("/dataset", "{\"name\":\"DeLiver\"}");
        helper.testCreateJsonEntity("/dataset", "{\"name\":\"Harvard Brain\"}");
        JSONObject newDataset = helper.testCreateJsonEntity("/dataset", "{\"name\":\"MouseCross\"}");

        JSONObject dataset = helper.testGetJsonEntity(newDataset.getString("uri"));
        
        assertEquals(newDataset.getString("id"), dataset.getString("id"));
        assertEquals("MouseCross", dataset.getString("name"));
    }


    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}.
     * 
     * TODO un-ignore this
     * @throws Exception
     */
    @Test
    @Ignore
    public void testGetDatasets() throws Exception {
        int totalNumDatasets = 12;
        // Load up a few datasets
        for(int i = 0; i < totalNumDatasets; i++) {
            helper.testCreateJsonEntity("/dataset", "{\"name\":\"DeLiver" + i + "\"}");
        }
        
        JSONObject results = helper.testGetJsonEntities("/dataset");
        assertEquals(12, results.getInt("totalNumberOfResults"));
        assertEquals(10, results.getJSONArray("results").length());
        assertEquals("/dataset?offset=1&limit=10", results.getJSONObject("paging").getString(PaginatedResults.PREVIOUS_PAGE_FIELD));
        assertEquals("/dataset?offset=11&limit=10", results.getJSONObject("paging").getString(PaginatedResults.NEXT_PAGE_FIELD));
        
        results = helper.testGetJsonEntities(results.getJSONObject("paging").getString(PaginatedResults.NEXT_PAGE_FIELD));
        assertEquals(12, results.getInt("totalNumberOfResults"));
        assertEquals(2, results.getJSONArray("results").length());
        assertEquals("/dataset?offset=1&limit=10", results.getJSONObject("paging").getString(PaginatedResults.PREVIOUS_PAGE_FIELD));
        assertEquals("/dataset?offset=11&limit=10", results.getJSONObject("paging").getString(PaginatedResults.NEXT_PAGE_FIELD));
        
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}.
     * @throws Exception
     */
    @Test
    public void testUpdateDataset() throws Exception {
        // Load up a few datasets
        helper.testCreateJsonEntity("/dataset", "{\"name\":\"DeLiver\"}");
        helper.testCreateJsonEntity("/dataset", "{\"name\":\"Harvard Brain\"}");
        JSONObject newDataset = helper.testCreateJsonEntity("/dataset", "{\"name\":\"MouseCross\"}");

        // Get one dataset
        JSONObject dataset = helper.testGetJsonEntity(newDataset.getString("uri"));
        assertEquals(newDataset.getString("id"), dataset.getString("id"));
        assertEquals("MouseCross", dataset.getString("name"));

        // Modify that dataset
        dataset.put("name", "MouseX");
        JSONObject updatedDataset = helper.testUpdateJsonEntity(dataset);
        
        assertEquals("MouseX", updatedDataset.getString("name"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity(java.lang.String)}.
     * @throws Exception
     */
    @Test
    public void testDeleteDataset() throws Exception {
        // Load up a few datasets
        helper.testCreateJsonEntity("/dataset", "{\"name\":\"DeLiver\"}");
        helper.testCreateJsonEntity("/dataset", "{\"name\":\"Harvard Brain\"}");
        JSONObject newDataset = helper.testCreateJsonEntity("/dataset", "{\"name\":\"MouseCross\"}");
        helper.testDeleteJsonEntity(newDataset.getString("uri"));
    }

    /*****************************************************************************************************
     * Bad parameters tests
     */
    
    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}.
     * @throws Exception
     */
    @Test
    public void testInvalidModelCreateDataset() throws Exception {
        
        JSONObject results = helper.testCreateJsonEntityShouldFail("/dataset", 
                "{\"name\": \"DeLiver\", \"BOGUS\":\"this does not match our model object\"}",
                HttpStatus.BAD_REQUEST);

        // The response should be something like: {"reason":"Unrecognized field \"BOGUS\" 
        // (Class org.sagebionetworks.repo.model.Dataset), not marked as ignorable\n at 
        // [Source: org.springframework.mock.web.DelegatingServletInputStream@2501e081; line: 1, column: 19]"}

        String reason = results.getString("reason");
        assertTrue(reason.matches("(?s).*\"BOGUS\".*"));
        assertTrue(reason.matches("(?s).*not marked as ignorable.*"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}.
     * @throws Exception
     */
    @Test
    public void testMissingRequiredFieldCreateDataset() throws Exception {

        JSONObject results = helper.testCreateJsonEntityShouldFail("/dataset", 
                "{\"version\": \"1.0.0\"}",
                HttpStatus.BAD_REQUEST);

        // The response should be something like: {"reason":"'name' is a required property for Dataset"}
        assertEquals("'name' is a required property for Dataset", results.getString("reason"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}.
     * @throws Exception
     */
    @Test
    public void testUpdateDatasetConflict() throws Exception {
        // Create a dataset
        JSONObject newDataset = helper.testCreateJsonEntity("/dataset", "{\"name\":\"MouseCross\"}");
        // Get that dataset
        JSONObject dataset = helper.testGetJsonEntity(newDataset.getString("uri"));
        assertEquals(newDataset.getString("id"), dataset.getString("id"));
        assertEquals("MouseCross", dataset.getString("name"));
        // Modify that dataset
        dataset.put("name", "MouseX");
        JSONObject updatedDataset = helper.testUpdateJsonEntity(dataset);
        assertEquals("MouseX", updatedDataset.getString("name"));
        
        // Modify the dataset we got earlier a second time
        dataset.put("name", "CONFLICT MouseX");
        JSONObject error = helper.testUpdateJsonEntityShouldFail(dataset, HttpStatus.PRECONDITION_FAILED);
        
        // TODO matches
    }

    
    //
//  /**
//   * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities(Integer, Integer, HttpServletRequest)}.
//   * @throws Exception
//   */
//  @Test
//  public void testGetDatasetsBadLimit() throws Exception {
//      // Load up a few datasets
//      testCreateDataset();
//      testCreateDataset();
//
//      MockHttpServletRequest request = new MockHttpServletRequest();
//      MockHttpServletResponse response = new MockHttpServletResponse();
//      request.setMethod("GET");
//      request.addHeader("Accept", "application/json");
//      request.setRequestURI("/dataset");
//      request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, "1");
//      request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, "0");
//      servlet.service(request, response);
//      log.info("Results: " + response.getContentAsString());
//      assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
//      JSONObject results = new JSONObject(response.getContentAsString());
//      // The response should be:
//      assertNotNull(results.getString("reason"));
//  }
//  
//  /**
//   * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities(Integer, Integer, HttpServletRequest)}.
//   * @throws Exception
//   */
//  @Test
//  public void testGetDatasetsBadOffset() throws Exception {
//      // Load up a few datasets
//      testCreateDataset();
//      testCreateDataset();
//
//      MockHttpServletRequest request = new MockHttpServletRequest();
//      MockHttpServletResponse response = new MockHttpServletResponse();
//      request.setMethod("GET");
//      request.addHeader("Accept", "application/json");
//      request.setRequestURI("/dataset");
//      request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, "-5");
//      request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, "0");
//      servlet.service(request, response);
//      log.info("Results: " + response.getContentAsString());
//      assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
//      JSONObject results = new JSONObject(response.getContentAsString());
//      // The response should be:
//      assertNotNull(results.getString("reason"));
//  }
//  

    /*****************************************************************************************************
     * Not Found Tests
     */
    
    /**
     * Test method for {@link org.sagebionetworks.repo.web.DAOControllerImp#getEntity(java.lang.String, javax.servlet.http.HttpServletRequest)}.
     * @throws Exception 
     */
    @Test
    public void testGetNonExistentDataset() throws Exception {
        JSONObject results = helper.testCreateJsonEntity("/dataset", "{\"name\":\"DeLiver\"}");

        helper.testDeleteJsonEntity(results.getString("uri"));
        
        JSONObject error = helper.testGetJsonEntityShouldFail(results.getString("uri"), HttpStatus.NOT_FOUND);

        // The response should be something like: {"reason":"no dataset with id 100 exists"}
        // assertTrue(results.getString("reason"));
    }
    
    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}.
     * @throws Exception
     */
    @Test
    public void testUpdateNonExistentDataset() throws Exception {
        JSONObject results = helper.testCreateJsonEntity("/dataset", "{\"name\":\"DeLiver\"}");

        helper.testDeleteJsonEntity(results.getString("uri"));
        
        JSONObject error = helper.testUpdateJsonEntityShouldFail(results, HttpStatus.NOT_FOUND);

        // The response should be something like: {"reason":"no dataset with id 100 exists"}
        // assertTrue(results.getString("reason"));
    }

  /**
  * Test method for {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity(java.lang.String)}.
  * @throws Exception
  */
 @Test
 public void testDeleteNonExistentDataset() throws Exception {
     JSONObject results = helper.testCreateJsonEntity("/dataset", "{\"name\":\"DeLiver\"}");

     helper.testDeleteJsonEntity(results.getString("uri"));
     
     JSONObject error = helper.testDeleteJsonEntityShouldFail(results.getString("uri"), HttpStatus.NOT_FOUND);

     // The response should be something like: {"reason":"no dataset with id 100 exists"}
     // assertTrue(results.getString("reason"));
 }


}
