package org.sagebionetworks.repo.model.file;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
/**
 * FileHandles can be associated with many different object types.  Each object type records FileHandles associations differently.
 * This interface  
 * 
 * @author jhill
 *
 */
public interface FileHandleAssociationProvider {

	/**
	 * Given a set of FileHandleIds and an associatedObjectId, get the sub-set of file handle ids that are directly associated with the given object (excluding the previews).
	 * 
	 * @param fileHandleIds
	 * @param objectId
	 * @return The sub-set of file handle ids that are directly associated with the given association.
	 */
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId);

	/**
	 * Get the ObjectType for this FileHandleAssociationType.
	 * @return
	 */
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType(); 

}
