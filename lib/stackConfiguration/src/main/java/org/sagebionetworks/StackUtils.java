package org.sagebionetworks;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

public class StackUtils {
	


	
	/**
	 * Validate that that toTest properties has non-null values for each of the required properties.
	 * @param required
	 * @param toTest
	 */
	public static void validateRequiredProperties(Properties required, Properties toTest, String stack, String instance){
		if(required == null) throw new IllegalArgumentException("The required properties cannot be null");
		if(toTest == null) throw new IllegalArgumentException("The toTest properties cannot be null");
		if(stack == null) throw new IllegalArgumentException("Stack cannot be null");
		if(instance == null) throw new IllegalArgumentException("The stack instance cannot be null");
		if(toTest.size() < required.size()) throw new IllegalArgumentException("Expected at least: "+required.size()+" properties");
		// Make we have a non-null value for each property
		Iterator<Object> it = required.keySet().iterator();
		while(it.hasNext()){
			String key = (String) it.next();
			String value = toTest.getProperty(key);
			if(value == null) throw new IllegalArgumentException("Missing a required property: "+key);
			value = value.trim();
			if("".equals(value)) throw new IllegalArgumentException("A required property: "+key+" has no value");
			// Does this property require a prefix?
			String requiredValue = (String) required.get(key);
			if(requiredValue != null){
				StringBuilder prefix = new StringBuilder();
				if(requiredValue.indexOf(StackConstants.REQUIRES_STACK_PREFIX) >= 0){
					prefix.append(stack);
				}
				if(requiredValue.indexOf(StackConstants.REQUIRES_STACK_INTANCE_PREFIX) >= 0){
					prefix.append(instance);
				}
				// Validate the property.
				validateStackProperty(prefix.toString(), key, value);
			}
		}
	}
	
	/**
	 * Validate a statck property.
	 * @param stack
	 * @param value
	 */
	public static void validateStackProperty(String prefix, String key, String value){
		String valueToTest = value;
		try {
			// Is it a database url:
			if(key.indexOf(StackConstants.DATABASE_URL_PROPERTY) >= 0){
				int index = value.lastIndexOf("/");
				valueToTest = value.substring(index+1, value.length());
			}else{
				// Is the value a url?
				try{
					URL url = new URL(value);
					String path = url.getPath();
					path = path.replaceAll("\\\\","/");
					int index = path.lastIndexOf("/");
					if(index < 0){
						index = 0;
					}
					if (path.length()>0) valueToTest = path.substring(index+1, path.length());
				}catch(MalformedURLException e){
				}
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("The property: "+key+" must start with stack prefix: "+prefix+". Actual value: "+value+". Modified value: "+valueToTest, e);
		}
		// Now that we know what to test do the work
		validateValue(prefix, key, valueToTest);
	}
	

	/**
	 * Validate a single value.
	 * @param stack
	 * @param key
	 * @param value
	 */
	public static void validateValue(String prefix, String key, String value) {
		if(!value.startsWith(prefix)){
			throw new IllegalArgumentException("The property: "+key+" must start with stack prefix: "+prefix+". Actual value: "+value);
		}
	}

}
