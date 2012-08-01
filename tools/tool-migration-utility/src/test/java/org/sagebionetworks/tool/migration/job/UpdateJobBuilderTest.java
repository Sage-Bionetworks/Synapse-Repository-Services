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
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.job.Job.Type;

public class UpdateJobBuilderTest {

	
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
		UpdateJobBuilder builder = new UpdateJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 0;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(0, response.pendingDependencies);
		assertEquals(0, jobQueue.size());
	}
	
	@Test
	public void testDestinationSameAsSource() throws Exception{
		// make sure the destination is the same as the source
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		for(MigratableObjectData fromSource: source){
			dest.add(fromSource);
		}
		assertEquals(source, dest);
		Map<MigratableObjectDescriptor, MigratableObjectData> destMap = JobUtil.buildMigratableMapFromList(dest);
		// now run the job
		int batchSize = 5;
		UpdateJobBuilder builder = new UpdateJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 0;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(0, response.pendingDependencies);
		assertEquals(0, jobQueue.size());
	}
	
	@Test
	public void testSourceWithUpdates() throws Exception{
		// make sure the destination is the same as the source
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		for(MigratableObjectData fromSource: source) {
			dest.add(XestUtil.cloneMigratableObjectData(fromSource));
		}
		assertEquals(source, dest);
		// Change some eTags on the source
		source.get(2).setEtag("33");
		source.get(5).setEtag("33");
		source.get(7).setEtag("33");
		
		Map<MigratableObjectDescriptor, MigratableObjectData> destMap = JobUtil.buildMigratableMapFromList(dest);
		// now run the job
		int batchSize = 2;
		UpdateJobBuilder builder = new UpdateJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 3;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(0, response.pendingDependencies);
		assertEquals(expectedSubmited/batchSize+1, jobQueue.size());
		
		// Check the jobQue
		Job job = null;
		while((job = jobQueue.poll()) != null){
			assertNotNull(job);
			assertEquals(Type.UPDATE, job.getJobType());
			assertNotNull(job.getObjectIds());
			assertTrue(job.getObjectIds().size() <= batchSize);
		}
	}
	
	@Test
	public void testSourceWithUpdatesSmallUpdates() throws Exception{
		// make sure the destination is the same as the source
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		for(MigratableObjectData fromSource: source){
			dest.add(XestUtil.cloneMigratableObjectData(fromSource));
		}
		assertEquals(source, dest);
		// Change some eTags on the source
		MigratableObjectData toUpdate = source.get(2);
		toUpdate.setEtag("33");
		
		Map<MigratableObjectDescriptor, MigratableObjectData> destMap = JobUtil.buildMigratableMapFromList(dest);
		// now run the job
		int batchSize = 10;
		UpdateJobBuilder builder = new UpdateJobBuilder(source, destMap, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 1;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(0, response.pendingDependencies);
		assertEquals(1, jobQueue.size());
		
		Set<String> set = jobQueue.peek().getObjectIds();
		assertNotNull(set);
		// Does it contain the one expected id.
		assertTrue(set.contains(toUpdate.getId().getId()));
		
	}
}
