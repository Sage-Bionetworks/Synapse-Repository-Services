package org.sagebionetworks.logging;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.profiler.Frame;
import org.sagebionetworks.profiler.LoggingProfiler;
import org.sagebionetworks.repo.web.AccessInterceptor;

/**
 * Test that logging is setup as expected.
 * @author John
 *
 */
public class LoggingTest {

	private static Logger log = LogManager.getLogger(LoggingTest.class);
	
	@Before
	public void before(){
		// setup a session id.
		String sessionId = UUID.randomUUID().toString();
		ThreadContext.put(AccessInterceptor.SESSION_ID, sessionId);
	}
	
	@After
	public void after(){
		// Clear the stack
		ThreadContext.clear();
	}
	
	@Test
	public void testTrace(){
		log.debug("This is a trace message");
	}
	
	@Test
	public void testDebug(){
		log.debug("This is a debug message");
	}
	
	@Test
	public void testInfo(){
		log.info("This is an info message");
	}
	
	@Test
	public void testException(){
		log.error("An Error!!!", new RuntimeException(new IllegalArgumentException("Bad mojo!")));
	}
	
	@Test
	public void testProfileTrace(){
		LoggingProfiler profiler = new LoggingProfiler();
		Frame frame = new Frame();
		frame.setElapse(1010);
		frame.setName("this is a trace frame");
		profiler.fireProfile(frame);
	}
	
	@Test
	public void testProfileDebug(){
		LoggingProfiler profiler = new LoggingProfiler();
		Frame frame = new Frame();
		frame.setElapse(5001);
		frame.setName("this is a debug frame");
		profiler.fireProfile(frame);
	}
	
	@Test
	public void testRollover() throws InterruptedException{
		log.info("Log dir: "+System.getProperty("java.io.tmpdir"));
		int count = 70;
		for(int i=0; i<count; i++){
			log.debug("Test rollover "+i);
			Thread.sleep(1000);
		}
	}
	
	
}
