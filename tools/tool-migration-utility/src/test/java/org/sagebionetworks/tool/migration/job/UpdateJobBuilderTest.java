package org.sagebionetworks.tool.migration.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.job.Job.Type;

public class UpdateJobBuilderTest {

	
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
		UpdateJobBuilder builder = new UpdateJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 0;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(0, response.pendingDependancies);
		assertEquals(0, jobQueue.size());
	}
	
	@Test
	public void testDestinationSameAsSource() throws Exception{
		// make sure the destination is the same as the source
		List<EntityData> dest = new ArrayList<EntityData>();
		for(EntityData fromSource: source){
			dest.add(new EntityData(fromSource));
		}
		assertEquals(source, dest);
		Map<String, EntityData> destMap = JobUtil.buildMapFromList(dest);
		// now run the job
		int batchSize = 5;
		UpdateJobBuilder builder = new UpdateJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 0;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(0, response.pendingDependancies);
		assertEquals(0, jobQueue.size());
	}
	
	@Test
	public void testSourceWithUpdates() throws Exception{
		// make sure the destination is the same as the source
		List<EntityData> dest = new ArrayList<EntityData>();
		for(EntityData fromSource: source){
			dest.add(new EntityData(fromSource));
		}
		assertEquals(source, dest);
		// Change some eTags on the source
		source.get(2).seteTag("33");
		source.get(5).seteTag("33");
		source.get(7).seteTag("33");
		
		Map<String, EntityData> destMap = JobUtil.buildMapFromList(dest);
		// now run the job
		int batchSize = 2;
		UpdateJobBuilder builder = new UpdateJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 3;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(0, response.pendingDependancies);
		assertEquals(expectedSubmited/batchSize+1, jobQueue.size());
		
		// Check the jobQue
		Job job = null;
		while((job = jobQueue.poll()) != null){
			assertNotNull(job);
			assertEquals(Type.UPDATE, job.getJobType());
			assertNotNull(job.getEntityIds());
			assertTrue(job.getEntityIds().size() <= batchSize);
		}
	}
	
	@Test
	public void testSourceWithUpdatesSmallUpdates() throws Exception{
		// make sure the destination is the same as the source
		List<EntityData> dest = new ArrayList<EntityData>();
		for(EntityData fromSource: source){
			dest.add(new EntityData(fromSource));
		}
		assertEquals(source, dest);
		// Change some eTags on the source
		EntityData toUpdate = source.get(2);
		toUpdate.seteTag("33");
		
		Map<String, EntityData> destMap = JobUtil.buildMapFromList(dest);
		// now run the job
		int batchSize = 10;
		UpdateJobBuilder builder = new UpdateJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 1;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(0, response.pendingDependancies);
		assertEquals(1, jobQueue.size());
		
		Set<String> set = jobQueue.peek().getEntityIds();
		assertNotNull(set);
		// Does it contain the one expected id.
		assertTrue(set.contains(toUpdate.getEntityId()));
		
	}
}
