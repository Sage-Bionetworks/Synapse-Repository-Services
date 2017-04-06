package org.sagebionetworks.ids;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test will ensure that multiple threads can create IDs concurrently without
 * overlap.
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:id-generator.spb.xml", "classpath:id-generator-test-context.xml" })
public class IdGeneratorConcurrencyTest {
	
	@Autowired
	IdGenerator idGenerator;
	
	// The number of ms to allow this test to run before failing.
	private int TIME_OUT = 1000*10; // ten seconds
	private int numberOfThreads = 8;
	
	@Test
	public void testConcurrentThreads() throws InterruptedException{
		// All threads will store the returned ids here.
		final Set<Long> sharedIdSet = Collections.synchronizedSet(new HashSet<Long>());
		final int numberIds = 100;
		final AtomicInteger count = new AtomicInteger(0);
		// Setup each thread
		Thread[] threads = new Thread[numberOfThreads];
		for(int i=0; i< numberOfThreads; i++){
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Generate 100 ids, yielding between each
						for(int j=0; j<numberIds; j++){
							Long id = idGenerator.generateNewId(IdType.ENTITY_ID);
							if(j % 25 == 0){
								System.out.println("Thread.id="+Thread.currentThread().getId()+" generated id: "+id);							
							}
							assertTrue("Duplicate ID found!", sharedIdSet.add(id));
							count.incrementAndGet();
							// Make sure other threads can go
							Thread.yield();
						}
						// This thread is done
						System.out.println("Thread.id="+Thread.currentThread().getId()+" finished");
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			});
		}
		// start them up
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join(TIME_OUT);
		}
		System.out.println("Expected: "+numberOfThreads*numberIds);
		System.out.println("Count: "+count.get());
		// Make sure we have the expected number of IDs
		assertEquals("Did not get the expceted count of insertions", numberOfThreads*numberIds, count.get());
		assertEquals("Did not get the expceted count of unique IDs", numberOfThreads*numberIds, sharedIdSet.size());
	}

	@Test
	public void testConcurrentThreadsBatch() throws InterruptedException{
		// All threads will store the returned ids here.
		final Set<Long> sharedIdSet = Collections.synchronizedSet(new HashSet<Long>());
		final int numberIds = 100;
		final int batchSize = 50;
		final AtomicInteger count = new AtomicInteger(0);
		// Setup each thread
		Thread[] threads = new Thread[numberOfThreads];
		for(int i=0; i< numberOfThreads; i++){
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Generate 100 ids, yielding between each
						for(int j=0; j<numberIds; j++){
							BatchOfIds range = idGenerator.generateBatchNewIds(IdType.ENTITY_ID, batchSize);
							if(j % 25 == 0){
								System.out.println("Thread.id="+Thread.currentThread().getId()+" generated id: "+range.getFirstId());							
							}
							// attempt to add each ID from the range into the shared set
							for(long id=range.getFirstId(); id <= range.getLastId(); id++){
								assertTrue("Duplicate ID found!", sharedIdSet.add(id));
								count.incrementAndGet();
							}
							// Make sure other threads can go
							Thread.yield();
						}
						// This thread is done
						System.out.println("Thread.id="+Thread.currentThread().getId()+" finished");
					} catch (Throwable t) {
						t.printStackTrace();
						fail(t.getMessage());
					}
				}
			});
		}
		// start them up
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join(TIME_OUT);
		}
		System.out.println("Expected: "+numberOfThreads*numberIds*batchSize);
		System.out.println("Count: "+count.get());
		// Make sure we have the expected number of IDs
		assertEquals("Did not get the expceted count of insertions", numberOfThreads*numberIds*batchSize, count.get());
		assertEquals("Did not get the expceted count of unique IDs", numberOfThreads*numberIds*batchSize, sharedIdSet.size());
	}

}
