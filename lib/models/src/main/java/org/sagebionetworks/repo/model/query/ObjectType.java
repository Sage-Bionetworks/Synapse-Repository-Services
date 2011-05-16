package org.sagebionetworks.repo.model.query;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.Project;

/**
 * The types of objects that can queried.
 * 
 * @author jmhill
 *
 */
public enum ObjectType {
	dataset(Dataset.class, (short)0),
	layer(InputDataLayer.class, (short)1),
	layerlocation(LayerLocation.class, (short)2),
	project(Project.class, (short)3);
	
	private Class<? extends Base> clazz;
	private short id;
	ObjectType(Class<? extends Base> clazz, short id){
		this.clazz = clazz;
		this.id = id;
	}
	
	/**
	 * What is the class that goes with this type?
	 * @return
	 */
	public Class<? extends Base> getClassForType(){
		return this.clazz;
	}
	
	public short getId(){
		return id;
	}
	
	public static ObjectType getTypeForId(short id){
		ObjectType[] array  = ObjectType.values();
		for(ObjectType type: array){
			if(type.getId() == id) return type;
		}
		throw new IllegalArgumentException("Unkown id for ObjectType: "+id);
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
