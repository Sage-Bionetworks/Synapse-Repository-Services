package org.sagebionetworks.logging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.logging.s3.LogDAO;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.sagebionetworks.repo.web.controller.AccessInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test that logging is setup as expected.  This includes writing sessionIds to
 * every log entry and logs are sweept to S3. 
 * @author John
 *
 */
public class LoggingIntegrationTest extends AbstractAutowiredControllerTestBase {

	private static Logger log = LogManager.getLogger(LoggingIntegrationTest.class);
	
	/**
	 * This test has a long wait time as log rolling only occurs once a minute.
	 */
	public static final int MAX_WAIT_MS = 1000*60*6;
	
	
	@Autowired
	LogDAO logDAO;
	
	@Before
	public void before(){
		// clear all s3 log data before we start
		logDAO.deleteAllStackInstanceLogs();

	}
	
	@After
	public void after(){
		// Clear the stack
		ThreadContext.clear();
	}
	
	@Ignore
	@Test
	public void testRoundTrip() throws InterruptedException, IOException{
		// This test writes data to the log and then attempts to find that data S3 after the logs have been swept.
		// This session ID should be written to all log entries
		String sessionId = UUID.randomUUID().toString();
		ThreadContext.put(AccessInterceptor.SESSION_ID, sessionId);
		log.trace("This is a trace message");
		log.debug("This is a debug message");
		log.info("This is an info message");
		log.error("An Error!!!", new RuntimeException(new IllegalArgumentException("Bad mojo!")));
		// Now we need to keep writing to the log to trigger a rolloever into a GZip.
		// Now we need to wait for the logs to appear in S3
		long start = System.currentTimeMillis();
		String key = null;
		do{
			key = logDAO.findLogContainingUUID(sessionId);
			if(key == null){
				log.info("Waiting for logs to get swept to S3...");
				Thread.sleep(1000);
			}
			long elapse = System.currentTimeMillis()-start;
			assertTrue("Timed out waiting for logs to be swept to S3",elapse < MAX_WAIT_MS);
		}while(key == null);
		assertNotNull(key);
		log.info("Found the log data in :"+key);
	}
		
}
