package org.sagebionetworks.web.util;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		List<Dataset> sorted = ListUtils.getSortedCopy("name", false, list, Dataset.class);
		assertNotNull(sorted);
		System.out.println(sorted);
		
	}
	
	@Test
	public void testMapSort(){
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		String[] columns = new String[]{"col1", "col2", "col3"};
		int numberRows = 3;
		for(int i=0; i<numberRows; i++){
			Map<String, Object> row = new HashMap<String, Object>();
			// Add a column for each column
			for(int col=0; col<columns.length; col++){
				row.put(columns[col], ""+i+"#"+col);
			}
			rows.add(row);
		}
		// Now get a sorted copy
		List<Map<String, Object>> sorted = ListUtils.getSortedCopy(columns[0], false, rows, Map.class);
		assertNotNull(sorted);
		System.out.println(sorted);
	}

}
