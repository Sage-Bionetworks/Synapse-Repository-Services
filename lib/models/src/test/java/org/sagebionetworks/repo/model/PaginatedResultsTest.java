package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
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
	
	Long limit;
	Long offset;
	
	@Before
	public void before(){
		limit = 100L;
		offset = limit*2;
	}
	
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWithLimitAndOffsetNullPage(){
		List<Project> page = null;
		PaginatedResults.createWithLimitAndOffset(page, limit, offset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWithLimitAndOffsetNullLimit(){
		List<Project> page = new LinkedList<>();
		limit = null;
		PaginatedResults.createWithLimitAndOffset(page, limit, offset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWithLimitAndOffsetNullOffset(){
		List<Project> page = new LinkedList<>();
		offset = null;
		PaginatedResults.createWithLimitAndOffset(page, limit, offset);
	}
	
	
	@Test
	public void testCreateWithLimitAndOffsetUnderLimt(){
		int pageSize = limit.intValue()-1;
		List<Project> page = createListOfSize(pageSize, Project.class);
		PaginatedResults<Project> result = PaginatedResults.createWithLimitAndOffset(page, limit, offset);
		assertNotNull(result);
		assertEquals(page, result.getResults());
		assertEquals(offset+pageSize, result.getTotalNumberOfResults());
	}
	
	@Test
	public void testCreateWithLimitAndOffsetAtLimit(){
		int pageSize = (int)limit.intValue();
		List<Project> page = createListOfSize(pageSize, Project.class);
		PaginatedResults<Project> result = PaginatedResults.createWithLimitAndOffset(page, limit, offset);
		assertNotNull(result);
		assertEquals(page, result.getResults());
		assertEquals(offset+pageSize+1, result.getTotalNumberOfResults());
	}
	
	@Test
	public void testCreateWithLimitAndOffsetOverLimit(){
		int pageSize = limit.intValue()+1;
		List<Project> page = createListOfSize(pageSize, Project.class);
		PaginatedResults<Project> result = PaginatedResults.createWithLimitAndOffset(page, limit, offset);
		assertNotNull(result);
		assertEquals(page, result.getResults());
		assertEquals(offset+pageSize+1, result.getTotalNumberOfResults());
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
