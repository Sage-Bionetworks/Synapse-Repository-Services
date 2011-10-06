package profiler.org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Queue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.controller.DefaultController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test for cloud watch profiler
 * @author ntiedema
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:controllerProfilerAutowired-spb.xml" })
public class ControllerProfilerAutowiredTest {
	//will need a defaultController to make calls so MetricDatum objects get
	//created
	@Autowired
	DefaultController defaultController;
	
	//need a consumer to add watchers to, so we can track if "put" to cloudWatch
	//was successful or not
	@Autowired
	Consumer consumer;

	private static final long NANOSECOND_PER_MILLISECOND = 1000000L;
	private static final long TOOK_TO_LONG = 7000;

	/**
	 * Tests if cloudWatch put worked, and success message worked it's
	 * way into watcher's list.  If the stackConstant has the profiler set to 
	 * off/false then the test will exit and will still pass.
	 */
	@Test
	public void testWatchersSuccess() throws Exception {
		assertNotNull(defaultController);
		assertNotNull(consumer);
		
		//test uses xml configuration that ensures profiler will always be on
		
		WatcherImpl testWatcher = new WatcherImpl();
		consumer.registerWatcher(testWatcher);

		//verify our watcher is in the consumer
		assertTrue(consumer.getWatcherList().size() == 1);
		
		defaultController.getAclSchema();
		defaultController.getAnnotationsSchema();
		
		Queue<String> ourWatchersList = testWatcher.getUpdates();
		assertNotNull(ourWatchersList);
		
		//need to wait  amount of ms until the cloud watch call will have been made and then verify the
		//success message was put in our watcher's queue
		
		//tracking in milliseconds, but will use nanoTime (conversion is 1,000,000 nanoseconds
		//for every 1 millisecond)
		
		long start = System.nanoTime();	//collect start time
		
		//want to wait until either ms have passed (as a time out) or 
		//our watcher's queue olds a message
		while(ourWatchersList.size() <= 0){
			Thread.sleep(1000);
			long end = System.nanoTime();
			long latency = (end - start)/NANOSECOND_PER_MILLISECOND;
			
			if (latency > TOOK_TO_LONG){
				break;
			}
			ourWatchersList = testWatcher.getUpdates();
		}
		
		assertEquals('S', ourWatchersList.peek().charAt(0));
		
		//remove watcher as we no longer need it
		consumer.removeWatcher(testWatcher);
	}
}