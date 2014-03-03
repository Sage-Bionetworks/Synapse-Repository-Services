package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

public class ResourceTrackerTest {
	
	@Test
	public void testAttemptCheckoutExceedsMax() throws TemporarilyUnavailableException{
		ResourceTracker tracker = new ResourceTracker(100);
		try {
			// attempt to checkout more than the max.
			tracker.attemptCheckout(101);
			fail("This should have failed!");
		} catch (ExceedsMaximumResources e) {
			// This is expected
			System.out.println(e.getMessage());
			// Check the message
			assertEquals("Cannot allocate 101 resource because it exceed the maximum of 100", e.getMessage());
		}
	}
	
	@Test
	public void testAttemptCheckoutResourceTempoarryUnavailable() throws TemporarilyUnavailableException, ExceedsMaximumResources{
		ResourceTracker tracker = new ResourceTracker(100);
		// The first we should be able to allocate 
		Long result = tracker.attemptCheckout(80);
		assertEquals(new Long(80), result);
		// Now allocate more
		result = tracker.attemptCheckout(20);
		assertEquals(new Long(20), result);
		// At this point there are no more resources available;
		try{
			tracker.attemptCheckout(1);
			fail("This should have failed as there are not more resources.");
		}catch(TemporarilyUnavailableException e){
			// This is expected.
		}
		// Check back in some of the resources
		tracker.checkin(result);
		// We should not be able to checkout 21.
		try{
			tracker.attemptCheckout(21);
			fail("This should have failed as there are not more resources.");
		}catch(TemporarilyUnavailableException e){
			// This is expected.
		}
		// However we should be able to checkout 20
		result = tracker.attemptCheckout(20);
		assertEquals(new Long(20), result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBadCheckin() throws TemporarilyUnavailableException, ExceedsMaximumResources{
		ResourceTracker tracker = new ResourceTracker(100);
		tracker.attemptCheckout(50);
		// Try to check-in more than we checked out.
		tracker.checkin(51);
	}
	
	/**
	 * Note: This is an important test!
	 * 
	 *  It validates that ResourceTracker.allocateAndUseResources() is not blocking.
	 * @throws Exception 
	 */
	@Test
	public void testForBlocking() throws Exception{
		// Start with a tracker with 100 resources.
		final ResourceTracker tracker = new ResourceTracker(100);
		// These runners will block until told to stop.
		final BlockingRunner runnerA = new BlockingRunner();
		final BlockingRunner runnerB = new BlockingRunner();
		final BlockingRunner runnerC = new BlockingRunner();

		// setup the threads
		Thread threadA = new Thread(new Runnable(){
			@Override
			public void run() {
				// For this thread start runner a
				try {
					tracker.allocateAndUseResources(runnerA, 20l);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
		}});
		// 
		Thread threadB = new Thread(new Runnable(){
			@Override
			public void run() {
				// For this thread start runner b
				try {
					tracker.allocateAndUseResources(runnerB, 20l);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
		}});
		// Before we start, no resources should be allocated
		assertEquals(0, tracker.getAllocated());
		// We are all setup so start both threads.
		threadA.start();
		// give the thread a chance to start
		Thread.sleep(100);
		assertEquals("Twenty resources should be allocated with one thread running", 20, tracker.getAllocated());
		// Start the next thread
		threadB.start();
		// give the thread a chance to start
		Thread.sleep(100);
		assertFalse("ResourceTracker.allocateAndUseResources() is blocking if only twenty resources have been allocated at this point! ResourceTracker.allocateAndUseResources() must not be synchronized!!!!!!!!!", tracker.getAllocated() == 20);
		assertEquals("Forty resources should be allocated with two threads running", 40, tracker.getAllocated());
		// Now try to allocate more than is available.
		try{
			tracker.allocateAndUseResources(runnerC, 61l);
			fail("This should have failed as 61 resources are not available");
		}catch(TemporarilyUnavailableException e){
			// This is expected.
		}
		// Now stop the fist runner
		runnerA.setBlocking(false);
		// give the thread a chance to stop.
		Thread.sleep(BlockingRunner.SLEEP_MS*2);
		assertFalse(threadA.isAlive());
		assertEquals("Twenty resources should be allocated with one thread running", 20, tracker.getAllocated());
		// Now stop the second worker
		runnerB.setBlocking(false);
		// give the thread a chance to stop.
		Thread.sleep(BlockingRunner.SLEEP_MS*2);
		assertFalse(threadB.isAlive());
		assertEquals("Zero resources should be allocated with no threads running", 0, tracker.getAllocated());
	}

}
