package org.sagebionetworks.tool.migration.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

public class DeleteJobBuilderTest {

	
	List<MigratableObjectData> source;
	Map<MigratableObjectDescriptor, MigratableObjectData> sourceMap;
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
		sourceMap = JobUtil.buildMigratableMapFromList(source);
	}
	
	@Test
	public void testDestinationEmpty() throws Exception{
		// The destination is empty
		List<MigratableObjectData> destList = new ArrayList<MigratableObjectData>();
		// now run the job
		int batchSize = 5;
		DeleteJobBuilder builder = new DeleteJobBuilder(sourceMap, destList, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 0;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(0, response.pendingDependencies);
		assertEquals(0, jobQueue.size());
	}
	
	@Test
	public void testDestinationSharedParentDelete() throws Exception{
		// The destination is empty
		List<MigratableObjectData> destList = new ArrayList<MigratableObjectData>();
		// This is the only one that should be deleted.
		destList.add(XestUtil.createMigratableObjectData("20", "0", null));
		// All of these will be deleted due to the cascade delete.
		destList.add(XestUtil.createMigratableObjectData("21", "0", "20"));
		destList.add(XestUtil.createMigratableObjectData("22", "0", "21"));
		destList.add(XestUtil.createMigratableObjectData("23", "0", "22"));
	
		// now run the job
		int batchSize = 5;
		DeleteJobBuilder builder = new DeleteJobBuilder(sourceMap, destList, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// This used to be the logic...
		// Even though all of these entities do not exist in the source only the 
		// root parent should be deleted because of the cascade delete.
		// ... but we no longer support the optimization:
		int expectedSubmited = 4; // was 1
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(0, response.pendingDependencies);
		assertEquals(1, jobQueue.size());
		Set<String> toDelete = jobQueue.peek().getObjectIds();
		assertNotNull(toDelete);
		assertNotNull(toDelete.contains(destList.get(0).getId().getId()));
	}
	
	@Test
	public void testDestinationParentsRoot() throws Exception{
		// The destination is empty
		List<MigratableObjectData> destList = new ArrayList<MigratableObjectData>();
		// For this case the parent will not be deleted, so each should be deleted.
		destList.add(XestUtil.createMigratableObjectData("20", "0", "1"));
		destList.add(XestUtil.createMigratableObjectData("21", "0", "1"));
		destList.add(XestUtil.createMigratableObjectData("22", "0", "1"));
		destList.add(XestUtil.createMigratableObjectData("23", "0", "1"));
	
		// now run the job
		int batchSize = 2;
		DeleteJobBuilder builder = new DeleteJobBuilder(sourceMap, destList, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// All 4 should be delted.
		int expectedSubmited = 4;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(0, response.pendingDependencies);
		assertEquals(2, jobQueue.size());
		Set<String> toDelete = jobQueue.peek().getObjectIds();
		assertNotNull(toDelete);
		assertNotNull(toDelete.contains(destList.get(0).getId().getId()));
	}
	
	@Test
	public void testDestinationSameAsSource() throws Exception{
		// make sure the destination is the same as the source
		List<MigratableObjectData> dest = new ArrayList<MigratableObjectData>();
		for(MigratableObjectData fromSource: source){
			dest.add(fromSource);
		}
		assertEquals(source, dest);
		// now run the job
		int batchSize = 5;
		DeleteJobBuilder builder = new DeleteJobBuilder(sourceMap, dest, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 0;
		assertEquals(expectedSubmited, response.getSubmittedToQueue());
		assertEquals(0, response.pendingDependencies);
		assertEquals(0, jobQueue.size());
	}
	

	
}
