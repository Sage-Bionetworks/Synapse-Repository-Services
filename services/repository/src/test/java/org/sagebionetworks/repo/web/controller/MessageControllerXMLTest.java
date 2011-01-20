/**
 *
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * Unit tests for the Message CRUD operations exposed by the MessageController with
 * XML request and response encoding.
 * <p>
 * This unit test suite for the repository Google App Engine service
 * uses a local instance of DataStoreService.
 * <p>
 * The following unit tests are written at the servlet layer to test
 * bugs in our URL mapping, request format, response format, and
 * response status code.
 * <p>
 * Dev Note: we are not spending much effort on the XML representation at this time.
 * A deeper test into the structure of the XML will be needed if/when we have anyone
 * consuming xml.  The purpose of configuring any XML at all right now is to just confirm
 * that the basic mechanism for varying the response encoding depending upon the Accept header
 * is working.
 * <p>
 * TODO parse the XML to a map using XStream http://xstream.codehaus.org/tutorial.html if we
 * get more serious about supporting XML encoding as a first class citizen
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
public class MessageControllerXMLTest {

    private static final Logger log = Logger
    .getLogger(MessageControllerXMLTest.class.getName());

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
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message/test");
        try {
            servlet.service(request, response);
        }
        catch (Exception e) {
            // Unfortunately this exception is uncaught via the DispatcherServlet so that the service does not
            // have an opportunity to handle it.  It would be better if were able to handle it like the others
            // and return HttpStatus.BAD_REQUEST
            log.log(Level.INFO, "servlet failed to handle exception, but we expected this", e);
            // So this test passes because it did what we expected
            // javax.servlet.ServletException: Could not resolve view with name '' in servlet with name 'repository'
            return;
        }
        fail("something changed!");
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getEntity(java.lang.Long)}.
     * @throws Exception
     */
    @Test
    public void testGetNonExistentMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("GET");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message/22");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertTrue(response.getContentAsString().equals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<errorResponse><reason>no message with id 22 exists</reason></errorResponse>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getEntity(java.lang.Long)}.
     * @throws Exception
     */
    @Test
    public void testGetMessageBadId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("GET");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message/thisShouldBeANumber");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertTrue(response.getContentAsString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><errorResponse>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#createEntity(Message)}.
     * @throws Exception
     */
    @Test
    public void testCreateMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message");
        request.addHeader("Content-Type", "application/xml; charset=UTF-8");
        request.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><message><text>message from a unit test</text></message>".getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        assertTrue(response.getContentAsString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><message><id>"));
        assertTrue(response.getContentAsString().endsWith("</id><text>message from a unit test</text></message>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#createEntity(Message)}.
     * @throws Exception
     */
    @Test
    public void testInvalidModelCreateMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message");
        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><message><text>message from a unit test</text></message>".getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertTrue(response.getContentAsString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<errorResponse><reason>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#createEntity(Message)}.
     * @throws Exception
     */
    @Test
    public void testInvalidXMLCreateMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message");
        request.addHeader("Content-Type", "application/xml; charset=UTF-8");
        // Notice the malformed XML
        request.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><message>message from a unit test</text></message>".getBytes("UTF-8"));
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertTrue(response.getContentAsString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<errorResponse><reason>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#createEntity(Message)}.
     * @throws Exception
     */
    @Test
    public void testMissingBodyCreateMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message");
        request.addHeader("Content-Type", "application/xml; charset=UTF-8");
        // No call to request.setContent()
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertTrue(response.getContentAsString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<errorResponse><reason>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#deleteEntity(java.lang.Long)}.
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
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message/2");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
        assertEquals("", response.getContentAsString());
    }


    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#deleteEntity(java.lang.Long)}.
     * @throws Exception
     */
    @Test
    public void testDeleteNonExistentMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setMethod("DELETE");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message/2");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertTrue(response.getContentAsString().equals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<errorResponse><reason>no message with id 2 exists</reason></errorResponse>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getEntity(java.lang.Long)}.
     *
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
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message/1");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        String results = response.getContentAsString();
        // The response should be something like:
        // <?xml version="1.0" encoding="UTF-8" standalone="yes"?><message><id>1</id>
        // <text>message from a unit test</text></message>
        assertTrue(results.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><message><id>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getEntities(Integer, Integer, HttpServletRequest)}.
     *
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
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        String results = response.getContentAsString();
        // The response should be something like:
        // <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        // <result><paging>
        // <entry><key>previous</key><value>/message?offset=1&amp;limit=10</value></entry>
        // <entry><key>next</key><value>/message?offset=11&amp;limit=10</value></entry></paging>
        // <results xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="message">
        // <id>1</id><text>message from a unit test</text></results>
        // <results xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="message"><id>2</id><text>message from a unit test</text></results>
        // <totalNumberOfResults>42</totalNumberOfResults>
        // </result>
        assertTrue(results.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><result>"));
        assertTrue(results.endsWith("</totalNumberOfResults></result>"));
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getEntities(Integer, Integer, HttpServletRequest)}.
     * @throws Exception
     */
    @Test
    public void testUnsupportedEncoding() throws Exception {
        // Load up a few messages
        testCreateMessage();
        testCreateMessage();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.addHeader("Accept", "application/javascript");
        request.setRequestURI("/message");
        try {
            servlet.service(request, response);
        }
        catch (HttpMediaTypeNotAcceptableException  e) {
            // Unfortunately this exception is uncaught via the DispatcherServlet so that the service does not
            // have an opportunity to handle it.  It is handled by an error page configured via web.xml
            log.log(Level.INFO, "servlet failed to handle exception, but we expected this", e);
            // So this test passes because it did what we expected
            // org.springframework.web.HttpMediaTypeNotAcceptableException: Could not find acceptable representation
            return;
        }
        fail("something changed!");
    }

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.MessageController#getEntities(Integer, Integer, HttpServletRequest)}.
     * @throws Exception
     */
    @Test
    public void testNoHandler() throws Exception {
        // Load up a few messages
        testCreateMessage();
        testCreateMessage();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.addHeader("Accept", "application/xml");
        request.setRequestURI("/message/1/foo");
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        // Note that an error page configured via web.xml will actually be served
        assertEquals("", response.getContentAsString());
    }
}
