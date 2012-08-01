package org.sagebionetworks.tool.migration.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.job.Job.Type;

public class CreationJobBuilderTest {
	
	List<MigratableObjectData> source;
	Queue<Job> jobQueue;
	
	@Before
	public void before(){
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

		// Start with an empty queue
		jobQueue = new LinkedList<Job>();
	}
	
	@Test
	public void testDestinationEmpty() throws Exception{
		// The destination is empty
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		Map<MigratableObjectDescriptor, MigratableObjectData> destMap = JobUtil.buildMigratableMapFromList(dest);
		// now run the job
		int batchSize = 5;
		CreationJobBuilder builder = new CreationJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Just the root should have been submitted
		int expectedSubmited = 1;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(source.size()-expectedSubmited, response.pendingDependencies);
		assertEquals(1, jobQueue.size());
		
		// Check the jobQue
		Job job = jobQueue.poll();
		assertNotNull(job);
		assertNotNull(job.getObjectIds());
		assertEquals(1, job.getObjectIds().size());
		assertTrue(job.getObjectIds().contains("1"));
		
	}

	@Test
	public void testDestinationWithRoot() throws Exception{
		// This time add root to the destination.
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		dest.add(source.get(0));
		Map<MigratableObjectDescriptor, MigratableObjectData> destMap = JobUtil.buildMigratableMapFromList(dest);
		// now run the job
		int batchSize = 2;
		CreationJobBuilder builder = new CreationJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Just the root's children should have been submitted
		int expectedSubmitted = 3;
		assertEquals((int)Math.ceil((double)expectedSubmitted/(double)batchSize), jobQueue.size());
		// this test removes a job from the queue
		assertEquals("actual: "+jobQueue.poll().getObjectIds(), expectedSubmitted, response.getSubmittedToQueue());
		assertEquals(source.size()-dest.size()-expectedSubmitted, response.pendingDependencies);
		
		// Check the jobQue
		Job job = null;
		while((job = jobQueue.poll()) != null){
			assertNotNull(job);
			assertEquals(Type.CREATE, job.getJobType());
			assertNotNull(job.getObjectIds());
			assertTrue("job size: "+job.getObjectIds().size()+" batchsize: "+batchSize, job.getObjectIds().size() <= batchSize);
		}
		
	}
	
	@Test
	public void testDestinationWithPartFirstLevel() throws Exception{
		// This time add root the the destination.
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		dest.add(source.get(0));
		dest.add(source.get(1));
		dest.add(source.get(2));
		Map<MigratableObjectDescriptor, MigratableObjectData> destMap = JobUtil.buildMigratableMapFromList(dest);
		// now run the job
		int batchSize = 2;
		CreationJobBuilder builder = new CreationJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Just the root should have been submitted
		int expectedSubmited = 9;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(source.size()-dest.size()-expectedSubmited, response.pendingDependencies);
		assertEquals(expectedSubmited/batchSize+1, jobQueue.size());
		
		// Check the jobQue
		Job job = null;
		while((job = jobQueue.poll()) != null){
			assertNotNull(job);
			assertEquals(Type.CREATE, job.getJobType());
			assertNotNull(job.getObjectIds());
			assertTrue(job.getObjectIds().size() <= batchSize);
		}
		
	}
}
