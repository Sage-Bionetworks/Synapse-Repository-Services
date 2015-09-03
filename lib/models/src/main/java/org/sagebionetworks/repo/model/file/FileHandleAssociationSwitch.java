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
public interface FileHandleAssociationSwitch {

	/**
	 * Get the distinct FileHandleAssociation for the given set of
	 * FileHandleIds. The resulting set should only include FileHandleIds that
	 * are actually associated with the given objectId.
	 * 
	 * @param fileHandleIds
	 *            List of FileHandleIds to test.
	 * @param objectId
	 *            The associated object id.
	 * @return
	 */
	public Set<String> getDistinctAssociationsForFileHandleIds(
			List<String> fileHandleIds, String objectId,
			FileHandleAssociationType associationType);
	
	/**
	 * What ObjectType matches the FileHandleAssociationType?
	 * @param associationType
	 * @return
	 */
	public ObjectType getObjectTypeForAssociationType(FileHandleAssociationType associationType);

}
