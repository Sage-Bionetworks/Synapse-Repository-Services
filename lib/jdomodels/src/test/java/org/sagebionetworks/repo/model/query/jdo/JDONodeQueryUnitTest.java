package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.sagebionetworks.repo.model.query.FieldType;

/**
 * 
 * @author jmhill
 *
 */
public class JDONodeQueryUnitTest {

	Logger log = LogManager.getLogger(JDONodeQueryUnitTest.class);

	
	@Test
	public void testDetermineTypeFromValueNull(){
		assertEquals(null, QueryUtils.determineTypeFromValue(null));
	}
	
	@Test
	public void testDetermineTypeFromValueString(){
		assertEquals(FieldType.STRING_ATTRIBUTE, QueryUtils.determineTypeFromValue("StringValue"));
	}
	
	@Test
	public void testDetermineTypeFromValueLong(){
		assertEquals(FieldType.LONG_ATTRIBUTE, QueryUtils.determineTypeFromValue(new Long(123)));
	}
	
	@Test
	public void testDetermineTypeFromValueDouble(){
		assertEquals(FieldType.DOUBLE_ATTRIBUTE, QueryUtils.determineTypeFromValue(new Double(123.99)));
	}
	@Test
	public void testDetermineTypeFromValueLongAsString(){
		assertEquals(FieldType.LONG_ATTRIBUTE, QueryUtils.determineTypeFromValue("435"));
	}
	@Test
	public void testDetermineTypeFromValueDoubleAsString(){
		assertEquals(FieldType.DOUBLE_ATTRIBUTE, QueryUtils.determineTypeFromValue("435.99"));
	}
	
	@Test
	public void testAddNewOnly(){
		Map<String, Object> map = new HashMap<String, Object>();
		// Start with a non null vlaue
		map.put("id", "123");
		String annoKey = "annoKey";
		map.put(annoKey, null);
		Map<String, String> toAdd = new HashMap<String, String>();
		toAdd.put(annoKey, "one");
		toAdd.put("id", "should not get added");
		List<String> select = new ArrayList<String>();
		select.add("id");
		select.add(annoKey);
		QueryUtils.addNewOnly(map, toAdd, select);
		assertEquals("one", map.get(annoKey));
		assertEquals("123", map.get("id"));
	}
}
