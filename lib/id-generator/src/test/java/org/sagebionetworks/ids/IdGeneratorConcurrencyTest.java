package org.sagebionetworks.ids;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * This test will ensure that multiple threads can create IDs concurrently without
 * overlap.
 * @author jmhill
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:id-generator.spb.xml" })
public class IdGeneratorConcurrencyTest {
	
	@Autowired
	IdGenerator idGenerator;
	
	// The number of ms to allow this test to run before failing.
	private int TIME_OUT = 1000*10; // ten seconds
	private int numberOfThreads = 2;
	
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
							assertTrue(sharedIdSet.add(id),"Duplicate ID found!");
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
		assertEquals(numberOfThreads*numberIds, count.get(), "Did not get the expceted count of insertions");
		assertEquals(numberOfThreads*numberIds, sharedIdSet.size(), "Did not get the expceted count of unique IDs");
	}
}
