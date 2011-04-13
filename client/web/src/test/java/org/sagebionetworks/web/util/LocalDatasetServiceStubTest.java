package org.sagebionetworks.web.util;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.PaginatedDatasets;

/**
 * Test for the local dataset service stub.
 * @author John
 *
 */
public class LocalDatasetServiceStubTest {
	
	@Test
	public void testSubList(){
		LocalDatasetServiceStub stub = new LocalDatasetServiceStub();
		stub.generateRandomDatasets(50);
		// The stub should start with 50 datasets
		List<Dataset> subList = ListUtils.getSubList(1, 10, stub.getAllForTests());
		assertNotNull(subList);
		// There should be 10 datasets in the list
		assertEquals(10, subList.size());
		
		// Now give too small of an offest
		subList = ListUtils.getSubList(0, 10, stub.getAllForTests() );
		assertNotNull(subList);
		// There should be 10 datasets in the list
		assertEquals(10, subList.size());
		
		// same size
		subList = ListUtils.getSubList(10, 10, stub.getAllForTests());
		assertNotNull(subList);
		// There should be 10 datasets in the list
		assertEquals(10, subList.size());
		
		// request more than is there
		subList = ListUtils.getSubList(45, 10, stub.getAllForTests());
		assertNotNull(subList);
		// There should be 10 datasets in the list
		assertEquals(6, subList.size());
		
		// Request past the limit
		subList = ListUtils.getSubList(51, 10, stub.getAllForTests());
		assertNotNull(subList);
		// There should be 10 datasets in the list
		assertEquals(0, subList.size());
	}
	
	@Test
	public void testGetAllDatasts(){
		LocalDatasetServiceStub stub = new LocalDatasetServiceStub();
		stub.clearAll();
		stub.generateRandomDatasets(50);
		PaginatedDatasets results = stub.getAllDatasets(1, 15, null, true);
		assertNotNull(results);
		assertEquals(50, results.getTotalNumberOfResults());
		List<Dataset> subList = results.getResults();
		assertEquals(15, subList.size());
	}
	
	@Test
	public void testGetDatset(){
		LocalDatasetServiceStub stub = new LocalDatasetServiceStub();
		stub.clearAll();
		stub.generateRandomDatasets(50);
		
		// Null and empty Ids should return null
		Dataset result = stub.getDataset(null);
		assertNull(result);
		result = stub.getDataset("");
		assertNull(result);
		result = stub.getDataset("badId");
		assertNull(result);
		// Pick some datsets in the middle of the set.
		PaginatedDatasets results = stub.getAllDatasets(25, 15, null, true);
		assertNotNull(results);
		// We should be able to find all datasets on this list
		List<Dataset> subList = results.getResults();
		assertEquals(15, subList.size());
		for(Dataset dataset: subList){
			Dataset found = stub.getDataset(dataset.getId());
			assertNotNull(found);
			// The should be equal
			assertTrue(dataset.equals(found));
		}
		
		// Make sure we can find a datast even with white space around the id.
		Dataset last = subList.get(0);
		assertNotNull(last);
		Dataset found = stub.getDataset("\n"+last.getId()+" ");
		assertNotNull(found);
		assertTrue(last.equals(found));
		
	}

}
