package profiler.org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit test for WatcherImpl
 * @author ntiedema
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class WatcherTest {
	@Autowired
	WatcherImpl watcher;
	
	String frontOfQueue = "string one";
	String middleOfQueue = "string two";
	String backOfQueue = "string three";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/**
	 * Tests that constructor that takes a queue of strings, correctly
	 * sets queue.
	 */
	@Test
	public void testConstructorWithQueueParameter() throws Exception {
		Queue<String> testQueueParam = new LinkedList<String>();
		testQueueParam.add(frontOfQueue);
		testQueueParam.add(middleOfQueue);
		testQueueParam.add(backOfQueue);
		
		WatcherImpl newTestWatcher = new WatcherImpl(testQueueParam);
		assertEquals(testQueueParam, newTestWatcher.getUpdates());
	}
	
	/**
	 * Tests update method and verifies string gets added to queue.
	 */ 
	@Test
	public void testWatcherUpdateMethod() throws Exception {
		assertTrue(watcher.getUpdates().size() == 0);
		watcher.update(frontOfQueue);
		assertTrue(watcher.getUpdates().size() == 1);
		
		//clean up
		watcher.removeQueueHead();
	}
	
	/**
	 * Tests update method for when string parameter is invalid.
	 */
	@Test (expected = IllegalArgumentException.class)
	public void testWatcherUpdateforInvalidParameter() throws Exception {
		String badString = null;
		
		assertTrue(watcher.getUpdates().size() == 0);
		watcher.update(badString);
		assertTrue(watcher.getUpdates().size() == 0);		
	}
	
	/**
	 * Tests update method and verifies it behaves correctly when queue
	 * is not empty.
	 */
	@Test
	public void testWatcherAddMethodForQueueWithTwoItemsInIt() throws Exception {
		assertTrue(watcher.getUpdates().size() == 0);
		watcher.update(frontOfQueue);
		watcher.update(middleOfQueue);
		watcher.update(backOfQueue);
		assertTrue(watcher.getUpdates().size() == 3);
		
		//cleanup
		watcher.removeQueueHead();
		watcher.removeQueueHead();
		watcher.removeQueueHead();
	}
	
	/**
	 * Tests getter for queue.
	 */
	@Test
	public void testWatcherGetUpdates() throws Exception {
		//make a watcher
		//add 3 strings
		//get the list and verify the size of list and each individual string
		assertTrue(watcher.getUpdates().size() == 0);
		watcher.update(frontOfQueue);
		watcher.update(middleOfQueue);
		watcher.update(backOfQueue);
		Queue<String> testResultsForGetUpdates = watcher.getUpdates(); 
		
		assertNotNull(testResultsForGetUpdates);
		assertEquals(3, testResultsForGetUpdates.size());
		assertEquals(frontOfQueue, testResultsForGetUpdates.poll());
		assertEquals(middleOfQueue, testResultsForGetUpdates.poll());
		assertEquals(backOfQueue, testResultsForGetUpdates.poll());
	}
	
	/**
	 * Tests removeQueueHead method.
	 */
	@Test
	public void testRemoveQueueHead() throws Exception {
		assertTrue(watcher.getUpdates().size() == 0);
		watcher.update(frontOfQueue);
		watcher.update(middleOfQueue);
		watcher.update(backOfQueue);
		String next = watcher.removeQueueHead();
		assertEquals(frontOfQueue, next);
		next = watcher.removeQueueHead();
		assertEquals(middleOfQueue, next);
		next = watcher.removeQueueHead();
		assertEquals(backOfQueue, next);		
	}
	
	/**
	 * tests removeQueueHead method when queue is empty.
	 */
	@Test
	public void testRemoveQueueHeadFromEmptyQueue() throws Exception {
		assertEquals(0, watcher.getUpdates().size());
		String resultsOfRemove = watcher.removeQueueHead();
		assertEquals("queue was empty", resultsOfRemove);
	}
}
