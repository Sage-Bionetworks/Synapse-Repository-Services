package org.sagebionetworks.tool.migration.job;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.job.Job.Type;

public class CreationJobBuilderTest {
	
	List<EntityData> source;
	Queue<Job> jobQueue;
	
	@Before
	public void before(){
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

		// Start with an empty queue
		jobQueue = new LinkedList<Job>();
	}
	
	@Test
	public void testDestinationEmpty() throws Exception{
		// The destination is empty
		List<EntityData> dest = new ArrayList<EntityData>();
		Map<String, EntityData> destMap = JobUtil.buildMapFromList(dest);
		// now run the job
		int batchSize = 5;
		CreationJobBuilder builder = new CreationJobBuilder(source, destMap, jobQueue, batchSize);
		CreatorResponse response = builder.call();
		assertNotNull(response);
		// Just the root should have been submitted
		int expectedSubmited = 1;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(source.size()-expectedSubmited, response.pendingDependancies);
		assertEquals(1, jobQueue.size());
		
		// Check the jobQue
		Job job = jobQueue.poll();
		assertNotNull(job);
		assertNotNull(job.getEntityIds());
		assertEquals(1, job.getEntityIds().size());
		assertTrue(job.getEntityIds().contains("1"));
		
	}

	@Test
	public void testDestinationWithRoot() throws Exception{
		// This time add root the the destination.
		List<EntityData> dest = new ArrayList<EntityData>();
		dest.add(source.get(0));
		Map<String, EntityData> destMap = JobUtil.buildMapFromList(dest);
		// now run the job
		int batchSize = 2;
		CreationJobBuilder builder = new CreationJobBuilder(source, destMap, jobQueue, batchSize);
		CreatorResponse response = builder.call();
		assertNotNull(response);
		// Just the root should have been submitted
		int expectedSubmited = 3;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(source.size()-dest.size()-expectedSubmited, response.pendingDependancies);
		assertEquals(expectedSubmited/batchSize+1, jobQueue.size());
		
		// Check the jobQue
		Job job = null;
		while((job = jobQueue.poll()) != null){
			assertNotNull(job);
			assertEquals(Type.CREATE, job.getJobType());
			assertNotNull(job.getEntityIds());
			assertTrue(job.getEntityIds().size() <= batchSize);
		}
		
	}
	
	@Test
	public void testDestinationWithPartFirstLevel() throws Exception{
		// This time add root the the destination.
		List<EntityData> dest = new ArrayList<EntityData>();
		dest.add(source.get(0));
		dest.add(source.get(1));
		dest.add(source.get(2));
		Map<String, EntityData> destMap = JobUtil.buildMapFromList(dest);
		// now run the job
		int batchSize = 2;
		CreationJobBuilder builder = new CreationJobBuilder(source, destMap, jobQueue, batchSize);
		CreatorResponse response = builder.call();
		assertNotNull(response);
		// Just the root should have been submitted
		int expectedSubmited = 9;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(source.size()-dest.size()-expectedSubmited, response.pendingDependancies);
		assertEquals(expectedSubmited/batchSize+1, jobQueue.size());
		
		// Check the jobQue
		Job job = null;
		while((job = jobQueue.poll()) != null){
			assertNotNull(job);
			assertEquals(Type.CREATE, job.getJobType());
			assertNotNull(job.getEntityIds());
			assertTrue(job.getEntityIds().size() <= batchSize);
		}
		
	}
}
