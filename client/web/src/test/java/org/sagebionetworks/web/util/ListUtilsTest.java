package org.sagebionetworks.web.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.web.shared.Dataset;

/**
 * 
 * @author jmhill
 *
 */
public class ListUtilsTest {
	
	@Test
	public void testDatasetSort(){
		
		// Create a few datset
		Dataset one = new Dataset();
		one.setId("0");
		one.setName("beta");
		one.setCreationDate(new Date(99));
		Dataset two = new Dataset();
		two.setId("1");
		two.setName("alpha");
		two.setCreationDate(new Date(98));
		Dataset allNull = new Dataset();
		// Add them to the list
		
		List<Dataset> list = new ArrayList<Dataset>();
		list.add(allNull);
		list.add(one);
		list.add(two);
		
		// Now sort the list on name
		List<Dataset> sorted = ListUtils.getSortedCopy("name", false, list);
		assertNotNull(sorted);
		System.out.println(sorted);
		
		
	}

}
