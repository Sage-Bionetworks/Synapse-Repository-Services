package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerTypeNames;

/**
 * A utility for creating various object types with all of the required fields set.
 * @author jmhill
 *
 */
public class ObjectTypeFactory {
	
	
	/**
	 * A factory method used by tests to create an object with all of the required fields filled in.
	 * @param name
	 * @param type
	 * @param parentId 
	 * @return entity with a few required fields filled in
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvalidModelException 
	 */
	public static Entity createObjectForTest(String name, EntityType type, String parentId) throws InstantiationException, IllegalAccessException, InvalidModelException{
		Entity object = type.getClassForType().newInstance();
		object.setName(name);
		// Handle layers
		if(object instanceof Data){
			Data layer = (Data) object;
			layer.setType(LayerTypeNames.C);
		} else if(object instanceof Analysis){
			Analysis analysis = (Analysis) object;
			analysis.setDescription("this is a fake description");
		} 
		// Any object that needs  parent
		if(object instanceof Entity){
			Entity child = (Entity) object;
			child.setParentId(parentId);
		}
		return object;
	}
	
	

}
