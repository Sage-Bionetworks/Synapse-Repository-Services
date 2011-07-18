package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
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
		if(object instanceof InputDataLayer){
			InputDataLayer layer = (InputDataLayer) object;
			layer.setType(InputDataLayer.LayerTypeNames.C.name());
		} else if(object instanceof LayerLocation){
			LayerLocation location = (LayerLocation) object;
			location.setType(LayerLocation.LocationTypeNames.sage.name());
			location.setPath("/somePath");
			location.setMd5sum("md5sum");
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
