package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * 
 * @author John
 *
 */
public class PaginatedResultsTest {
	
	@Test
	public void testRoundJSONRoundTrip() throws JSONObjectAdapterException{
		// Create a list of projects
		List<Project> list = new ArrayList<Project>();
		for(int i=0; i<4; i++){
			Project project = new Project();
			project.setParentId("1"+i);
			project.setId(""+i);
			project.setName("Project"+i);
			list.add(project);
		}
		
		PaginatedResults<Project> pr = new PaginatedResults<Project>(list, 101);
		// now write it to JSON
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		pr.writeToJSONObject(adapter);
		// Now to go json string
		String json = adapter.toJSONString();
		System.out.println(json);
		// Now create a clone from the JSON
		adapter = new JSONObjectAdapterImpl(json);
		PaginatedResults<Project> clone = new PaginatedResults<Project>(Project.class);
		clone.initializeFromJSONObject(adapter);
		assertEquals(pr, clone);
	}
	
	@Test
	public void testCreateWithLimitUnderLimt(){
		long limit = 100;
		int pageSize = (int)limit-1;
		List<Project> page = createListOfSize(pageSize, Project.class);
		PaginatedResults<Project> result = PaginatedResults.createWithLimit(page, limit);
		assertNotNull(result);
		assertEquals(page, result.getResults());
		assertEquals(pageSize, result.getTotalNumberOfResults());
	}
	
	@Test
	public void testCreateWithLimitAtLimit(){
		long limit = 100;
		int pageSize = (int)limit;
		List<Project> page = createListOfSize(pageSize, Project.class);
		PaginatedResults<Project> result = PaginatedResults.createWithLimit(page, limit);
		assertNotNull(result);
		assertEquals(page, result.getResults());
		assertTrue(result.getTotalNumberOfResults() > limit);
	}
	
	@Test
	public void testCreateWithLimitOverLimit(){
		long limit = 100;
		int pageSize = (int)limit+1;
		List<Project> page = createListOfSize(pageSize, Project.class);
		PaginatedResults<Project> result = PaginatedResults.createWithLimit(page, limit);
		assertNotNull(result);
		assertEquals(page, result.getResults());
		assertTrue(result.getTotalNumberOfResults() > pageSize);
	}
	
	/**
	 * Helper to create a list of a given size.
	 * @param size
	 * @param clazz
	 * @return
	 */
	public static <T> List<T> createListOfSize(int size, Class<? extends T> clazz){
		List<T> result = new LinkedList<T>();
		for(int i=0; i<size; i++){
			try {
				result.add(clazz.newInstance());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

}
