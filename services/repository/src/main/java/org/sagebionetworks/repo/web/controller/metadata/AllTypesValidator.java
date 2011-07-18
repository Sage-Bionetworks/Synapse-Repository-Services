package org.sagebionetworks.repo.web.controller.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;

/**
 * This class provides basic validation that applies to all object types.
 * 
 * @author jmhill
 *
 */
public class AllTypesValidator implements EntityValidator<Nodeable>{

	@Override
	public void validateEntity(Nodeable entity, EntityEvent event)throws InvalidModelException {
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(event == null) throw new IllegalArgumentException("Event cannot be null");
		// What is the type of the object
		ObjectType objectType = ObjectType.getNodeTypeForClass(entity.getClass());
		// Determine the parent type
		ObjectType parentType = null;
		List<EntityHeader> parentPath = event.getNewParentPath();
		if(parentPath != null && parentPath.size() > 0){
			// The last header is the direct parent
			EntityHeader parentHeader = parentPath.get(parentPath.size()-1);
			// Get the type for this parent.
			parentType = ObjectType.getFirstTypeInUrl(parentHeader.getType());
		}
		// Note: Null parent type is valid for some object types.
		if(!objectType.isValidParentType(parentType)){
			throw new IllegalArgumentException("Entity type: "+objectType+" cannot have a parent of type: "+parentType);
		}
	}
}
