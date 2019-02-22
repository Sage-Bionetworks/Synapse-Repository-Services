package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
	
	@Mock
	List<Project> mockPage;
	Long limit;
	Long offset;

	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		limit = 100L;
		offset = limit*2;
	}
	
	@Test
	public void testRoundJSONRoundTrip() throws JSONObjectAdapterException, JSONException{
		// Create a list of projects
		List<Project> list = new ArrayList<Project>();
		for(int i=0; i<4; i++){
			Project project = new Project();
			project.setParentId("1"+i);
			project.setId(""+i);
			project.setName("Project"+i);
			list.add(project);
		}
		
		PaginatedResults<Project> pr = PaginatedResults.createWithLimitAndOffset(list, 100L, 0L);
		// now write it to JSON
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		pr.writeToJSONObject(adapter);
		// Now to go json string
		String json = adapter.toJSONString();
		System.out.println(json);
		// Now create a clone from the JSON
		adapter = new JSONObjectAdapterImpl(json);
		PaginatedResults<Project> clone = PaginatedResults.createFromJSONObjectAdapter(adapter, Project.class);
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
		when(mockPage.size()).thenReturn(pageSize);
		PaginatedResults<Project> result = PaginatedResults.createWithLimitAndOffset(mockPage, limit, offset);
		assertNotNull(result);
		assertEquals(mockPage, result.getResults());
		assertEquals(offset+pageSize, result.getTotalNumberOfResults());
	}
	
	@Test
	public void testCreateWithLimitAndOffsetAtLimit(){
		int pageSize = (int)limit.intValue();
		when(mockPage.size()).thenReturn(pageSize);
		PaginatedResults<Project> result = PaginatedResults.createWithLimitAndOffset(mockPage, limit, offset);
		assertNotNull(result);
		assertEquals(mockPage, result.getResults());
		assertEquals(offset+pageSize+1, result.getTotalNumberOfResults());
	}
	
	@Test
	public void testCreateWithLimitAndOffsetOverLimit(){
		int pageSize = limit.intValue()+1;
		when(mockPage.size()).thenReturn(pageSize);
		PaginatedResults<Project> result = PaginatedResults.createWithLimitAndOffset(mockPage, limit, offset);
		assertNotNull(result);
		assertEquals(mockPage, result.getResults());
		assertEquals(offset+pageSize+1, result.getTotalNumberOfResults());
	}
	
	@Test
	public void testCalculateTotalWithLimitAndOffsetUnderLimt(){
		int pageSize = limit.intValue()-1;
		when(mockPage.size()).thenReturn(pageSize);
		long total = PaginatedResults.calculateTotalWithLimitAndOffset(pageSize, limit, offset);
		assertEquals(offset+pageSize, total);
	}
	
	@Test
	public void testcalculateTotalWithLimitAndOffsetAtLimit(){
		int pageSize = limit.intValue();
		when(mockPage.size()).thenReturn(pageSize);
		long total = PaginatedResults.calculateTotalWithLimitAndOffset(pageSize, limit, offset);
		assertEquals(offset+pageSize+1, total);
	}
	
	@Test
	public void testCalculateTotalWithLimitAndOffsetOverLimit(){
		int pageSize = limit.intValue()+1;
		when(mockPage.size()).thenReturn(pageSize);
		long total = PaginatedResults.calculateTotalWithLimitAndOffset(pageSize, limit, offset);
		assertEquals(offset+pageSize+1, total);
	}
	
}
