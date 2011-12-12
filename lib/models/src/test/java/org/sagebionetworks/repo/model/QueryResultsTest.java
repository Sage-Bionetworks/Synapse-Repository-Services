package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class QueryResultsTest {
	
	@Ignore
	@Test
	public void testRoundTrip() throws JSONObjectAdapterException{
		// Create a query result with all types of data

//		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
//		int count = 4;
//		for(int i=0; i<count; i++){
//			Map<String, Object> row = new HashMap<String, Object>();
//			row.put("string key", "String value:"+i);
//			row.put("long key", new Long(2*i));
//			row.put("double key", new Double(4*i+1.33));
//			row.put("int key", new Integer(i));
//			row.put("null key", null);
//			row.put("boolean key", (i % 2) >0);
////			row.put("key array", new String[]{"0ne", "two", "three"});
//			List<String> list = new ArrayList<String>();
//			list.add("a");
//			list.add("b");
//			list.add("c");
//			row.put("key List", list);
//			rows.add(row);
//		}
//		QueryResults result = new QueryResults(rows, 100);
//		// Make the round trip
//		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
//		result.writeToJSONObject(adapter);
//		QueryResults clone = new QueryResults();
//		clone.initializeFromJSONObject(adapter);
//		assertEquals(result, clone);
	}

}
