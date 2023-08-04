package org.sagebionetworks.repo.model;

import java.lang.reflect.Field;

/**
 * Extract the ID from objects.
 * @author John
 *
 */
public class ResponseDataExtractorUtil {
	
	/**
	 * Exact the ID from an object
	 * @param object
	 * @return
	 */
	public static String getObject(Object object, String fieldName){
		if(object == null) return null;
		try {
			Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			Object ob = field.get(object);
			if(ob instanceof String){
				return (String)ob;
			}else if(ob instanceof Long){
				return ((Long)ob).toString();
			}else return null;
		} catch (Exception e) {
			return null;
		} 
	}

	public static ResponseData getResponseData(Object object){
		String objectId = getObject(object, "id");
		String concreteType = getObject(object, "concreteType");
		return new ResponseData(objectId, concreteType);
	}
}
