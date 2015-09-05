package org.sagebionetworks.repo.model.file;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * Abstraction for all of the different type of FileHandleAssociationsProviders.
 * This switch will simply defer all calls to the correct provider for each
 * type.
 *
 */
public interface FileHandleAssociationManager {

	/**
	 * Given a set of FileHandleIds and an associatedObjectId, get the sub-set of
	 * FileHandleIds that are actually associated with the requested object.
	 * 
	 * @param fileHandleIds
	 *            List of FileHandleIds to test.
	 * @param objectId
	 *            The associated object id.
	 * @return
	 */
	public Set<String> getFileHandleIdsAssociatedWithObject(
			List<String> fileHandleIds, String objectId,
			FileHandleAssociateType associationType);
	
	/**
	 * What ObjectType matches the FileHandleAssociationType?
	 * @param associationType
	 * @return
	 */
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType associationType);

}
