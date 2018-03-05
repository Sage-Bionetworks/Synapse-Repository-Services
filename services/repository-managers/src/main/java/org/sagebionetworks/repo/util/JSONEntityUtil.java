package org.sagebionetworks.repo.util;

import org.sagebionetworks.schema.adapter.JSONEntity;

/**
 * Helpers for working with JSONEntity classes.
 * 
 * @author jmhill
 *
 */
public class JSONEntityUtil {
	
	/**
	 * Is the given class a JSONEntity
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isJSONEntity(Class<?> clazz){
		Class[] interfaces = clazz.getInterfaces();
		if(interfaces == null) return false;
		if(interfaces.length < 1) return false;
		for(Class c: interfaces){
			if(JSONEntity.class.equals(c)) return true;
		}
		return false;
	}

}
