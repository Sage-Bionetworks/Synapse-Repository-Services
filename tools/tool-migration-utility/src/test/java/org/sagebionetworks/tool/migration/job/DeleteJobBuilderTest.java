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
import org.sagebionetworks.tool.migration.dao.EntityData;

public class DeleteJobBuilderTest {

	
	List<EntityData> source;
	Map<String, EntityData> sourceMap;
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
		sourceMap = JobUtil.buildMapFromList(source);
	}
	
	@Test
	public void testDestinationEmpty() throws Exception{
		// The destination is empty
		List<EntityData> destList = new ArrayList<EntityData>();
		// now run the job
		int batchSize = 5;
		DeleteJobBuilder builder = new DeleteJobBuilder(sourceMap, destList, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 0;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(0, response.pendingDependancies);
		assertEquals(0, jobQueue.size());
	}
	
	@Test
	public void testDestinationSharedParentDelete() throws Exception{
		// The destination is empty
		List<EntityData> destList = new ArrayList<EntityData>();
		// This is the only one that should be deleted.
		destList.add(new EntityData("20", "0", null));
		// All of these will be deleted due to the cascade delete.
		destList.add(new EntityData("21", "0", "20"));
		destList.add(new EntityData("22", "0", "21"));
		destList.add(new EntityData("23", "0", "22"));
	
		// now run the job
		int batchSize = 5;
		DeleteJobBuilder builder = new DeleteJobBuilder(sourceMap, destList, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Even though all of these entities do not exist in the source only the 
		// root parent should be deleted because of the cascade delete.
		int expectedSubmited = 1;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(0, response.pendingDependancies);
		assertEquals(1, jobQueue.size());
		Set<String> toDelete = jobQueue.peek().getEntityIds();
		assertNotNull(toDelete);
		assertNotNull(toDelete.contains(destList.get(0).getEntityId()));
	}
	
	@Test
	public void testDestinationParentsRoot() throws Exception{
		// The destination is empty
		List<EntityData> destList = new ArrayList<EntityData>();
		// For this case the parent will not be deleted, so each should be deleted.
		destList.add(new EntityData("20", "0", "1"));
		destList.add(new EntityData("21", "0", "1"));
		destList.add(new EntityData("22", "0", "1"));
		destList.add(new EntityData("23", "0", "1"));
	
		// now run the job
		int batchSize = 2;
		DeleteJobBuilder builder = new DeleteJobBuilder(sourceMap, destList, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// All 4 should be delted.
		int expectedSubmited = 4;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(0, response.pendingDependancies);
		assertEquals(2, jobQueue.size());
		Set<String> toDelete = jobQueue.peek().getEntityIds();
		assertNotNull(toDelete);
		assertNotNull(toDelete.contains(destList.get(0).getEntityId()));
	}
	
	@Test
	public void testDestinationSameAsSource() throws Exception{
		// make sure the destination is the same as the source
		List<EntityData> dest = new ArrayList<EntityData>();
		for(EntityData fromSource: source){
			dest.add(new EntityData(fromSource));
		}
		assertEquals(source, dest);
		// now run the job
		int batchSize = 5;
		DeleteJobBuilder builder = new DeleteJobBuilder(sourceMap, dest, jobQueue, batchSize);
		BuilderResponse response = builder.call();
		assertNotNull(response);
		// Nothing should have been submitted.
		int expectedSubmited = 0;
		assertEquals(expectedSubmited, response.getSubmitedToQueue());
		assertEquals(0, response.pendingDependancies);
		assertEquals(0, jobQueue.size());
	}
	

	
}
