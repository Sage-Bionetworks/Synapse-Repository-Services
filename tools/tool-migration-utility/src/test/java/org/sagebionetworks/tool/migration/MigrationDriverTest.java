package org.sagebionetworks.tool.migration;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.job.Job;
import org.sagebionetworks.tool.migration.job.Job.Type;

public class MigrationDriverTest {
	
	ExecutorService threadPool = Executors.newFixedThreadPool(1);
	// The JOB queue
	Queue<Job> jobQueue = new ConcurrentLinkedQueue<Job>();
	List<EntityData> source;
	
	@Before
	public void before(){
		// Start with a clean queue
		jobQueue = new ConcurrentLinkedQueue<Job>();
		// Setup each test the same
		source = new ArrayList<EntityData>();
		// Create a root
		source.add(new EntityData("1", "0", null));
		// Add a few children of root
		source.add(new EntityData("2", "0", "1"));
		source.add(new EntityData("3", "0", "1"));
		source.add(new EntityData("4", "0", "1"));
		
		// Add some children to 2
		source.add(new EntityData("5", "0", "2"));
		source.add(new EntityData("6", "0", "2"));
		source.add(new EntityData("7", "0", "2"));
		source.add(new EntityData("8", "0", "2"));
		
		// Add some children to 3
		source.add(new EntityData("9", "0", "3"));
		source.add(new EntityData("10", "0", "3"));
		source.add(new EntityData("11", "0", "3"));
		source.add(new EntityData("12", "0", "3"));
		
		// Add some children to 4
		source.add(new EntityData("13", "0", "4"));
		source.add(new EntityData("14", "0", "4"));
		source.add(new EntityData("15", "0", "4"));
		source.add(new EntityData("16", "0", "4"));
	}
	
	/**
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * 
	 */
	@Test
	public void testPopulateQueueStaringData() throws InterruptedException, ExecutionException{
		// this is how most new stacks will start with three bootstrapped entities.
		List<EntityData> dest = new ArrayList<EntityData>();
		dest.add(new EntityData("99", "0", null));
		dest.add(new EntityData("101", "0", "99"));
		dest.add(new EntityData("102", "0", "99"));
		// Populate the queue for this setup
		RepositoryMigrationDriver.populateQueue(threadPool, jobQueue, source, dest, 1);
		// Only the first entity can be created since it is root.
		int expectedCreate = 1;
		int expectedUpdate = 0;
		int expectedDelete = 1;
		assertEquals(expectedCreate+expectedUpdate+expectedDelete, jobQueue.size());
		
		int createCount = 0;
		int deleteCount = 0;
		int updateCount = 0;
		for(Job job: jobQueue){
			if(job.getJobType() == Type.CREATE){
				assertTrue(job.getEntityIds().contains(source.get(0).getEntityId()));
				createCount++;
			}else if(job.getJobType() == Type.DELETE){
				assertTrue(job.getEntityIds().contains(dest.get(0).getEntityId()));
				deleteCount++;
			}else if(job.getJobType() == Type.UPDATE){
				updateCount++;
			}
		}
		// Check the final counts.
		assertEquals(expectedCreate, createCount);
		assertEquals(expectedUpdate, updateCount);
		assertEquals(expectedDelete, deleteCount);
	}
	
	/**
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * 
	 */
	@Test
	public void testSourceAndDestInSynch() throws InterruptedException, ExecutionException{
		// this is how most new stacks will start with three bootstrapped entities.
		List<EntityData> dest = new ArrayList<EntityData>();
		for(EntityData sourceD: source){
			dest.add(new EntityData(sourceD));
		}
		// Populate the queue for this setup
		RepositoryMigrationDriver.populateQueue(threadPool, jobQueue, source, dest, 1);
		// Only the first entity can be created since it is root.
		int expectedCreate = 0;
		int expectedUpdate = 0;
		int expectedDelete = 0;
		assertEquals(expectedCreate+expectedUpdate+expectedDelete, jobQueue.size());
		
		int createCount = 0;
		int deleteCount = 0;
		int updateCount = 0;
		for(Job job: jobQueue){
			if(job.getJobType() == Type.CREATE){
				createCount++;
			}else if(job.getJobType() == Type.DELETE){
				deleteCount++;
			}else if(job.getJobType() == Type.UPDATE){
				updateCount++;
			}
		}
		// Check the final counts.
		assertEquals(expectedCreate, createCount);
		assertEquals(expectedUpdate, updateCount);
		assertEquals(expectedDelete, deleteCount);
	}
	
	@Test
	public void testOutOfSych() throws InterruptedException, ExecutionException{
		// this is how most new stacks will start with three bootstrapped entities.
		List<EntityData> dest = new ArrayList<EntityData>();
		for(EntityData sourceD: source){
			dest.add(new EntityData(sourceD));
		}
		// Add one entity to the destination that is not in the source
		// this should get deleted.
		dest.add(new EntityData("99","0",null));
		// Update one in source.  This should get updated.
		source.get(3).seteTag("2");
		// Add one to source.  This should get created.
		source.add(new EntityData("17", "0", "16"));
		
	
		// Populate the queue for this setup
		RepositoryMigrationDriver.populateQueue(threadPool, jobQueue, source, dest, 1);
		// Only the first entity can be created since it is root.
		int expectedCreate = 1;
		int expectedUpdate = 1;
		int expectedDelete = 1;
		assertEquals(expectedCreate+expectedUpdate+expectedDelete, jobQueue.size());
		
		int createCount = 0;
		int deleteCount = 0;
		int updateCount = 0;
		// Tally what is in the queue.
		for(Job job: jobQueue){
			if(job.getJobType() == Type.CREATE){
				assertTrue(job.getEntityIds().contains("17"));
				createCount++;
			}else if(job.getJobType() == Type.DELETE){
				assertTrue(job.getEntityIds().contains("99"));
				deleteCount++;
			}else if(job.getJobType() == Type.UPDATE){
				assertTrue(job.getEntityIds().contains(source.get(3).getEntityId()));
				updateCount++;
			}
		}
		// Check the final counts.
		assertEquals(expectedCreate, createCount);
		assertEquals(expectedUpdate, updateCount);
		assertEquals(expectedDelete, deleteCount);
	}

}
