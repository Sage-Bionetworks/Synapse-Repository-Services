package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
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
	static final int MAX_DESCRIPTION_CHARS  = 1000;
	static final int MAX_NAME_CHARS  = 256;
	private static final String PARENT_RETRIEVAL_ERROR = "Parent entity could not be resolved";

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Override
	public void validateEntity(Entity entity, EntityEvent event) throws InvalidModelException {
		ValidateArgument.required(entity, "Entity");
		ValidateArgument.required(event, "Event");

		// Validate name.
		if (entity.getName() != null) {
			if (entity.getName().length() > MAX_NAME_CHARS) {
				throw new IllegalArgumentException("Name must be " + MAX_NAME_CHARS + " characters or less");
			}
		}

		// Validate description.
		if (entity.getDescription() != null) {
			if (entity.getDescription().length() > MAX_DESCRIPTION_CHARS) {
				throw new IllegalArgumentException("Description must be " + MAX_DESCRIPTION_CHARS +
						" characters or less");
			}
		}

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

		if (EventType.CREATE == event.getType()) {
			// If the parent lives inside an STS-enabled folder, only Files and Folders are allowed.
			if (!(entity instanceof FileEntity) && !(entity instanceof Folder)) {
				String parentId = entity.getParentId();
				if (parentId != null) {
					Optional<UploadDestinationListSetting> projectSetting = projectSettingsManager
							.getProjectSettingForNode(event.getUserInfo(), parentId, ProjectSettingsType.upload,
									UploadDestinationListSetting.class);
					if (projectSetting.isPresent() && projectSettingsManager.isStsStorageLocationSetting(
							projectSetting.get())) {
						throw new IllegalArgumentException("Can only create Files and Folders inside STS-enabled folders");
					}
				}
			}
		}

		if (EventType.UPDATE == event.getType()) {
			// If we're updating, the old version of the entity must exist.
			// Note that entity.getId() is validated by EntityServiceImpl.updateEntity().
			if (!nodeDAO.doesNodeExist(KeyFactory.stringToKey(entity.getId()))) {
				throw new NotFoundException("Entity " + entity.getId() + " does not exist");
			}

			// for update check for cycles
			if (parentPath != null) {
				for (EntityHeader eh : parentPath) {
					if (entity.getId().equals(eh.getId())) {
						throw new IllegalArgumentException("Invalid hierarchy: an entity cannot be an ancestor of itself");
					}
				}
			}
		}
	}
}
