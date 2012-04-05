package org.sagebionetworks.repo.web.controller.metadata;

import java.util.List;

import org.sagebionetworks.repo.manager.AttachmentManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides basic validation that applies to all object types.
 * 
 * @author jmhill
 *
 */
public class AllTypesValidatorImpl implements AllTypesValidator{
	
	@Autowired
	AttachmentManager attachmentManager;

	@Override
	public void validateEntity(Entity entity, EntityEvent event)throws InvalidModelException {
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(event == null) throw new IllegalArgumentException("Event cannot be null");
		// What is the type of the object
		EntityType objectType = EntityType.getNodeTypeForClass(entity.getClass());
		// Determine the parent type
		EntityType parentType = null;
		List<EntityHeader> parentPath = event.getNewParentPath();
		if(parentPath != null && parentPath.size() > 0){
			// The last header is the direct parent
			EntityHeader parentHeader = parentPath.get(parentPath.size()-1);
			// Get the type for this parent.
			parentType = EntityType.getFirstTypeInUrl(parentHeader.getType());
		}
		// Note: Null parent type is valid for some object types.
		if(!objectType.isValidParentType(parentType)){
			throw new IllegalArgumentException("Entity type: "+objectType+" cannot have a parent of type: "+parentType);
		}
		// Is this a create or update?
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType()){
			if(entity.getAttachments() != null){
				// This step will create any previews as needed.
				try {
					attachmentManager.checkAttachmentsForPreviews(entity);
				} catch (NotFoundException e) {
					throw new InvalidModelException(e);
				} catch (DatastoreException e) {
					throw new InvalidModelException(e);
				} catch (UnauthorizedException e) {
					throw new InvalidModelException(e);
				}
			}
		}
	}
}
