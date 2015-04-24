package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides basic validation that applies to all object types.
 * 
 * @author jmhill
 *
 */
public class AllTypesValidatorImpl implements AllTypesValidator{
	
	private static final String PARENT_RETRIEVAL_ERROR = "Parent entity could not be resolved";
	@Autowired
	NodeDAO nodeDAO;

	@Override
	public void validateEntity(Entity entity, EntityEvent event)throws InvalidModelException {
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(event == null) throw new IllegalArgumentException("Event cannot be null");
		// What is the type of the object
		EntityType objectType = EntityType.getEntityTypeForClass(entity.getClass());
		// Determine the parent type
		EntityType parentType = null;
		List<EntityHeader> parentPath = event.getNewParentPath();
		if(parentPath != null && parentPath.size() > 0){
			// The last header is the direct parent
			EntityHeader parentHeader = parentPath.get(parentPath.size()-1);
			// Get the type for this parent.
			parentType = EntityType.getEntityTypeForClassName(parentHeader.getType());
		}
		
		// Does entity have a parent?
		if (entity.getParentId() != null) {
			// Check if the parent entity is root
			boolean isParentRoot;			
			try {
				isParentRoot = nodeDAO.isNodeRoot(entity.getParentId());
			} catch (NotFoundException e) {
				throw new InvalidModelException(PARENT_RETRIEVAL_ERROR);
			} catch (DatastoreException e) {
				throw new InvalidModelException(PARENT_RETRIEVAL_ERROR);
			}				
			
			// If entity has a parent other than the root entity, validate the parent type
			if (!isParentRoot) {		
				// Note: Null parent type is valid for some object types.
				if(!objectType.isValidParentType(parentType)){
					throw new IllegalArgumentException("Entity type: "+objectType.getEntityTypeClassName()+" cannot have a parent of type: "+parentType.getEntityTypeClassName());
				}
			}
		}
		
		// Is this a create or update?
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType()){
			// Verify that path is acyclic
			if (parentPath != null){
				for (EntityHeader eh : parentPath){
					if (entity.getId().equals(eh.getId())){
						throw new IllegalArgumentException("Invalid hierarchy: an entity cannot be an ancestor of itself");
					}
				}
			}
		}
	}
}
