package org.sagebionetworks.ids;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
@ContextConfiguration(locations = { "classpath:id-generator.spb.xml" })
public class IdGeneratorConcurrencyTest {
	
	@Autowired
	IdGenerator idGenerator;
	
	// The number of ms to allow this test to run before failing.
	private int TIME_OUT = 1000*10; // ten seconds
	private int numberOfThreads = 8;
	private volatile boolean[] finished = new boolean[numberOfThreads];
	
	@Test
	public void testConcurrentThreads() throws InterruptedException{
		// All threads will store the returned ids here.
		final Set<Long> sharedIdSet = Collections.synchronizedSet(new HashSet<Long>());
		final int numberIds = 100;
		// Setup each thread
		Thread[] threads = new Thread[numberOfThreads];
		for(int i=0; i< numberOfThreads; i++){
			// Threads start off as not finished
			finished[i] = false;
			final int index = i;
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					// Generate 100 ids, yielding between each
					for(int j=0; j<numberIds; j++){
						Long id = idGenerator.generateNewId();
						if(j % 25 == 0){
							System.out.println("Thread.id="+Thread.currentThread().getId()+" generated id: "+id);							
						}
						assertTrue("Duplicate ID found!", sharedIdSet.add(id));
						// Make sure other threads can go
						Thread.yield();
					}
					// This thread is done
					System.out.println("Thread.id="+Thread.currentThread().getId()+" finished");
					finished[index] = true;
				}
			});
		}
		// start them up
		for(int i=0; i< numberOfThreads; i++){
			threads[i].start();
		}
		// Wait for all thread to finish
		long start = System.currentTimeMillis();
		for(int i=0; i< numberOfThreads; i++){
			// Wait for each thread
			while(!finished[i]){
				long current = System.currentTimeMillis();
				if(current - start > TIME_OUT){
					fail("Timout waiting for the threads to finish");
				}
				Thread.sleep(100);
			}
		}
		// Make sure we have the expected number of IDs
		assertEquals("Did not get the expceted count of unique IDs", numberOfThreads*numberIds, sharedIdSet.size());
	}

}
