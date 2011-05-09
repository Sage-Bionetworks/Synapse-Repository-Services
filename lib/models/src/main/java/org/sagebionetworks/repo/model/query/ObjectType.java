package org.sagebionetworks.repo.model.query;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.LayerLocation;

/**
 * The types of objects that can queried.
 * 
 * @author jmhill
 *
 */
public enum ObjectType {
	dataset(Dataset.class),
	layer(InputDataLayer.class),
	layerlocation(LayerLocation.class);
	
	private Class<? extends Base> clazz;
	
	ObjectType(Class<? extends Base> clazz){
		this.clazz = clazz;
	}
	
	/**
	 * What is the class that goes with this type?
	 * @return
	 */
	public Class<? extends Base> getClassForType(){
		return this.clazz;
	}
	
	
	/**
	 * Get the object type for a given class
	 * @param clazz
	 * @return
	 */
	public static ObjectType getNodeTypeForClass(Class<? extends Base> clazz){
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		ObjectType[] array  = ObjectType.values();
		for(ObjectType type: array){
			if(type.getClassForType() == clazz) return type;
		}
		throw new IllegalArgumentException("Unkown Object type: "+clazz.getName());
	}
	
}
