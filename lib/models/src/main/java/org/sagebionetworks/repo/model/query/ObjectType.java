package org.sagebionetworks.repo.model.query;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InputDataLayer;

/**
 * The types of objects that can queried.
 * 
 * @author jmhill
 *
 */
public enum ObjectType {
	dataset,
	layer;
	
	/**
	 * Get the object type for a given class
	 * @param clazz
	 * @return
	 */
	public static ObjectType getNodeTypeForClass(Class<? extends Base> clazz){
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		if(clazz == Dataset.class){
			return dataset;
		}else if(clazz == InputDataLayer.class){
			return layer;
		}else{
			throw new IllegalArgumentException("Unkown Object type: "+clazz.getName());
		}
	}
}
