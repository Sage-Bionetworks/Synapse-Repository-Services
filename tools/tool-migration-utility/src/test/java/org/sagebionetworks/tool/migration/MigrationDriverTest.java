package org.sagebionetworks.tool.migration;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.job.Job;
import org.sagebionetworks.tool.migration.job.Job.Type;
import org.sagebionetworks.tool.migration.job.XestUtil;

public class MigrationDriverTest {
	
	ExecutorService threadPool = Executors.newFixedThreadPool(1);
	// The JOB queue
	Queue<Job> jobQueue = new ConcurrentLinkedQueue<Job>();
	List<MigratableObjectData> source;
	
	@Before
	public void before(){
		// Start with a clean queue
		jobQueue = new ConcurrentLinkedQueue<Job>();
		// Setup each test the same
		source = new ArrayList<MigratableObjectData>();
		// Create a root
		source.add(XestUtil.createMigratableObjectData("1", "0", null));
		// Add a few children of root
		source.add(XestUtil.createMigratableObjectData("2", "0", "1"));
		source.add(XestUtil.createMigratableObjectData("3", "0", "1"));
		source.add(XestUtil.createMigratableObjectData("4", "0", "1"));
		
		// Add some children to 2
		source.add(XestUtil.createMigratableObjectData("5", "0", "2"));
		source.add(XestUtil.createMigratableObjectData("6", "0", "2"));
		source.add(XestUtil.createMigratableObjectData("7", "0", "2"));
		source.add(XestUtil.createMigratableObjectData("8", "0", "2"));
		
		// Add some children to 3
		source.add(XestUtil.createMigratableObjectData("9", "0", "3"));
		source.add(XestUtil.createMigratableObjectData("10", "0", "3"));
		source.add(XestUtil.createMigratableObjectData("11", "0", "3"));
		source.add(XestUtil.createMigratableObjectData("12", "0", "3"));
		
		// Add some children to 4
		source.add(XestUtil.createMigratableObjectData("13", "0", "4"));
		source.add(XestUtil.createMigratableObjectData("14", "0", "4"));
		source.add(XestUtil.createMigratableObjectData("15", "0", "4"));
		source.add(XestUtil.createMigratableObjectData("16", "0", "4"));
	}
	
	/**
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * 
	 */
	@Test
	public void testPopulateQueueStartingData() throws InterruptedException, ExecutionException{
		// this is how most new stacks will start with three bootstrapped entities.
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		dest.add(XestUtil.createMigratableObjectData("99", "0", null));
		dest.add(XestUtil.createMigratableObjectData("101", "0", "99"));
		dest.add(XestUtil.createMigratableObjectData("102", "0", "99"));
		// Populate the queue for this setup
		int MAX_BATCH_SIZE = 1;
		RepositoryMigrationDriver.populateQueue(threadPool, jobQueue, source, dest, MAX_BATCH_SIZE);
		// Only the first entity can be created since it is root.
		int expectedCreate = 1;
		// since there are no common objects bet src and dst, there's nothing to update
		int expectedUpdate = 0;
		// we will delete *everything* in dest
		int expectedDelete = 3;
		Job head = jobQueue.peek();
		assertEquals("First job: "+head.getJobType()+" "+head.getObjectIds(), expectedCreate+expectedUpdate+expectedDelete, jobQueue.size());
		
		int createCount = 0;
		int deleteCount = 0;
		int updateCount = 0;
		Set<String> sourceIds = new HashSet<String>();
		for (MigratableObjectData mod : source) sourceIds.add(mod.getId().getId());
		Set<String> destIds = new HashSet<String>();
		for (MigratableObjectData mod : dest) destIds.add(mod.getId().getId());
		
		for(Job job: jobQueue){
			if(job.getJobType() == Type.CREATE){
				assertEquals(1, job.getObjectIds().size()); // this is because we set MAX_BATCH_SIZE = 1
				assertTrue(sourceIds.contains(job.getObjectIds().iterator().next()));
				createCount++;
			}else if(job.getJobType() == Type.DELETE){
				assertEquals(1, job.getObjectIds().size()); // this is because we set MAX_BATCH_SIZE = 1
				assertTrue(destIds.contains(job.getObjectIds().iterator().next()));
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
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		for(MigratableObjectData sourceD: source){
			dest.add(sourceD);
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
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		for(MigratableObjectData sourceD: source){
			dest.add(XestUtil.cloneMigratableObjectData(sourceD));
		}
		// Add one entity to the destination that is not in the source
		// this should get deleted.
		dest.add(XestUtil.createMigratableObjectData("99","0",null));
		// Update one in source.  This should get updated.
		source.get(3).setEtag("2");
		// Add one to source.  This should get created.
		source.add(XestUtil.createMigratableObjectData("17", "0", "16"));
		
	
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
				assertTrue(job.getObjectIds().contains("17"));
				createCount++;
			}else if(job.getJobType() == Type.DELETE){
				assertTrue(job.getObjectIds().contains("99"));
				deleteCount++;
			}else if(job.getJobType() == Type.UPDATE){
				assertTrue(job.getObjectIds().contains(source.get(3).getId().getId()));
				updateCount++;
			}
		}
		// Check the final counts.
		assertEquals(expectedCreate, createCount);
		assertEquals(expectedUpdate, updateCount);
		assertEquals(expectedDelete, deleteCount);
	}

}
