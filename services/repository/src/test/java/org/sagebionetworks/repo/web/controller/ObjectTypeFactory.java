package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;

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
	public static Nodeable createObjectForTest(String name, ObjectType type, String parentId) throws InstantiationException, IllegalAccessException, InvalidModelException{
		Nodeable object = type.getClassForType().newInstance();
		object.setName(name);
		// Handle layers
		if(object instanceof Layer){
			Layer layer = (Layer) object;
			layer.setType(Layer.LayerTypeNames.C.name());
		} else if(object instanceof Location){
			Location location = (Location) object;
			location.setType(Location.LocationTypeNames.sage.name());
			location.setPath("/somePath");
			location.setMd5sum("9ca4d9623b655ba970e7b8173066b58f");
		} else if(object instanceof Eula){
			Eula eula = (Eula) object;
			eula.setAgreement("this is a fake agreement");
		} 
		// Any object that needs  parent
		if(object instanceof Nodeable){
			Nodeable child = (Nodeable) object;
			child.setParentId(parentId);
		}
		return object;
	}
	
	

}
