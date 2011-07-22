package org.sagebionetworks;

import java.util.Iterator;
import java.util.Properties;

public class StackUtils {
	
	/**
	 * Validate that that toTest properties has non-null values for each of the required properties.
	 * @param required
	 * @param toTest
	 */
	public static void validateRequiredProperties(Properties required, Properties toTest){
		if(required == null) throw new IllegalArgumentException("The required properties cannot be null");
		if(toTest == null) throw new IllegalArgumentException("The toTest properties cannot be null");
		if(toTest.size() < required.size()) throw new IllegalArgumentException("Expected at least: "+required.size()+" properties");
		// Make we have a non-null value for each property
		Iterator<Object> it = required.keySet().iterator();
		while(it.hasNext()){
			String key = (String) it.next();
			String value = toTest.getProperty(key);
			if(value == null) throw new IllegalArgumentException("Missing a required property: "+key);
			value = value.trim();
			if("".equals(value)) throw new IllegalArgumentException("A required property: "+key+" has no value");
		}
	}

}
