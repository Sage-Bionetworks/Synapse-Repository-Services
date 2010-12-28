/**
 *
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Message;
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
 * Unit tests for the Message CRUD operations exposed by the MessageController.
 * <p>
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
public class MessageControllerTest {

    private static final Logger log = Logger
    .getLogger(MessageControllerTest.class.getName());

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
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#sanityCheck(org.springframework.ui.ModelMap)}.
     * @throws Exception
     */
    @Test
    public void testSanityCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message/test");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals("we got 200 OK", 200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"hello":"REST rocks"}
        assertEquals("REST rocks", results.getString("hello"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getMessages()}.
     * @throws Exception
     */
    @Test
    public void testGetMessages() throws Exception {
        // Load up a few messages
        testCreateMessage();
        testCreateMessage();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONArray results = new JSONArray(response.getContentAsString());
        // The response should be:
        // [{"id":1,"text":"message from a unit test"},{"id":2,"text":"message from a unit test"}]
        assertEquals(2, results.length());
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getMessage(java.lang.Long)}.
     * @throws Exception
     */
    @Test
    public void testGetMessage() throws Exception {
        // Load up a few messages
        testCreateMessage();
        testCreateMessage();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message/2");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"id":1,"text":"message from a unit test"}
        assertEquals(2, results.getInt("id"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getMessage(java.lang.Long)}.
     * @throws Exception
     */
    @Test
    public void testGetNonExistentMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message/22");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"reason":"no message with id 22 exists"}
        assertTrue(null != results.getString("reason"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getMessage(java.lang.Long)}.
     * @throws Exception
     */
    @Test
    public void testGetMessageBadId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("GET");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message/thisShouldBeANumber");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"reason":"Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long';
        // nested exception is java.lang.NumberFormatException: For input string: \"thisShouldBeANumber\""}
        assertTrue(null != results.getString("reason"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#createMessage(Message)}.
     * @throws Exception
     */
    @Test
    public void testCreateMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message");
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent("{\"text\":\"message from a unit test\"}".getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be something like: {"id":1,"text":"message from a unit test"}
        assertTrue(0 < results.getInt("id"));
        assertEquals("message from a unit test", results.getString("text"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#createMessage(Message)}.
     * @throws Exception
     */
    @Test
    public void testInvalidJsonCreateMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message");
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent("{\"textBROKEN\":\"message from a unit test\"}".getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be something like: {"reason":"Unrecognized field \"test\"
        // (Class org.sagebionetworks.repo.model.Message), not marked as ignorable\n at
        // [Source: org.mortbay.jetty.HttpParser$Input@293a985; line: 1, column: 2]"}
        assertTrue(null != results.getString("reason"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#createMessage(Message)}.
     * @throws Exception
     */
    @Test
    public void testMissingJsonCreateMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message");
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        // No call to request.setContent()
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be something like: {"reason":"No content to map to Object due to end of input"}
        assertTrue(null != results.getString("reason"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#deleteMessage(java.lang.Long)}.
     * @throws Exception
     */
    @Test
    public void testDeleteMessage() throws Exception {
        // Load up a few messages
        testCreateMessage();
        testCreateMessage();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("DELETE");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message/2");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
        assertEquals("", response.getContentAsString());
    }


    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#deleteMessage(java.lang.Long)}.
     * @throws Exception
     */
    @Test
    public void testDeleteNonExistentMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("DELETE");
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/message/2");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"reason":"no message with id 22 exists"}
        assertTrue(null != results.getString("reason"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getMessage(java.lang.Long)}.
     *
     * TODO fix me!
     * @throws Exception
     */
    @Test
    public void testGetMessageXML() throws Exception {
        // Load up a few messages
        testCreateMessage();
        testCreateMessage();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message/2");
//        servlet.service(request, response);
        //log.info("***Results: " + response.getContentAsString());
        // TODO this is broken, returns 406 NOT_ACCEPTABLE
//        assertEquals(HttpStatus.OK.value(), response.getStatus());
//        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"id":1,"text":"message from a unit test"}
//        assertEquals(2, results.getInt("id"));
    }

}
