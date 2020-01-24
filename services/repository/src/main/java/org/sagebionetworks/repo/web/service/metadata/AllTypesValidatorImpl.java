package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides basic validation that applies to all object types.
 * 
 * @author jmhill
 *
 */
public class AllTypesValidatorImpl implements AllTypesValidator {

	private static final String PARENT_RETRIEVAL_ERROR = "Parent entity could not be resolved";
	
	@Autowired
	private NodeDAO nodeDAO;

	@Override
	public void validateEntity(Entity entity, EntityEvent event) throws InvalidModelException {
		ValidateArgument.required(entity, "Entity");
		ValidateArgument.required(event, "Event");

		// What is the type of the object
		EntityType objectType = EntityTypeUtils.getEntityTypeForClass(entity.getClass());
		// Determine the parent type
		EntityType parentType = null;
		List<EntityHeader> parentPath = event.getNewParentPath();
		if (parentPath != null && parentPath.size() > 0) {
			// The last header is the direct parent
			EntityHeader parentHeader = parentPath.get(parentPath.size() - 1);
			// Get the type for this parent.
			parentType = EntityTypeUtils.getEntityTypeForClassName(parentHeader.getType());
		}

		// Does entity have a parent?
		if (entity.getParentId() != null) {
			// Check if the parent entity is root
			try {
				if (nodeDAO.isNodeRoot(entity.getParentId())) {
					parentType = null;
				}
			} catch (NotFoundException | DatastoreException e) {
				throw new InvalidModelException(PARENT_RETRIEVAL_ERROR);
			}
		}

		// validate the parent type
		// Note: Null parent type is valid for some object types.
		if (!EntityTypeUtils.isValidParentType(objectType, parentType)) {
			throw new IllegalArgumentException("Entity type: "
					+ (objectType == null ? "null" : EntityTypeUtils.getEntityTypeClassName(objectType)) + " cannot have a parent of type: "
					+ (parentType == null ? "null" : EntityTypeUtils.getEntityTypeClassName(parentType)));
		}

		// for update check for cycles
		if (EventType.UPDATE == event.getType() && parentPath != null) {
			for (EntityHeader eh : parentPath) {
				if (entity.getId().equals(eh.getId())) {
					throw new IllegalArgumentException("Invalid hierarchy: an entity cannot be an ancestor of itself");
				}
			}
		}
	}
}
