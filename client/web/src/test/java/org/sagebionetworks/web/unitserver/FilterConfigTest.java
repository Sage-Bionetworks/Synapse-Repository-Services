package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.server.FilterConfig;
import org.sagebionetworks.web.shared.DisplayableValue;
import org.sagebionetworks.web.shared.FilterEnumeration;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;

public class FilterConfigTest {
	
	List<DisplayableValue> valuesOne;
	List<DisplayableValue> valuesTwo;
	
	List<FilterEnumeration> filterList;
	
	FilterConfig config;
	
	@Before
	public void setup(){
		// One
		valuesOne = new ArrayList<DisplayableValue>();
		valuesOne.add(new DisplayableValue("displayOne", "valueOne"));
		valuesOne.add(new DisplayableValue("displayTwo", "valueTwo"));
		valuesOne.add(new DisplayableValue("someXml:<>+='\"\\", "??/.,m{}[]1234567890-=!@#$%^&*()_+"));
		
		// Two
		valuesTwo = new ArrayList<DisplayableValue>();
		valuesTwo.add(new DisplayableValue(">displayTwoOne", "valueTwoOne"));
		valuesTwo.add(new DisplayableValue(null, "valueTwoTwo"));
		
		filterList = new ArrayList<FilterEnumeration>();
		filterList.add(new FilterEnumeration("column.id.one", "All One Values", WhereOperator.EQUALS, valuesOne));
		filterList.add(new FilterEnumeration("column.id.two", "All Two Values", WhereOperator.GREATER_THAN, valuesTwo));
		
		// Setup the config
		config = new FilterConfig();
		config.setFilters(filterList);
	}
	
	@Test
	public void testRoundTrip(){
		// First send it to a string
		StringWriter writer = new StringWriter();
		FilterConfig.toXml(config, writer);
		String xml = writer.toString();
		System.out.println(xml);
		// Make a copy with the string
		FilterConfig copy = FilterConfig.fromXml(new StringReader(xml));
		assertNotNull(copy);
		assertEquals(config, copy);
		
	}

}
