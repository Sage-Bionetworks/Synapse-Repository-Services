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
	 * Given a set of FileHandleIds and an associatedObjectId, get the sub-set of
	 * FileHandleIds that are actually associated with the requested object.
	 * @param fileHandleIds
	 * @param objectId
	 * @return The sub-set of FileHandleIds that are associated with the given objectId.
	 */
	public Set<String> getFileHandleIdsAssociatedWithObject(List<String> fileHandleIds, String objectId);

	/**
	 * Get the ObjectType for this FileHandleAssociationType.
	 * @return
	 */
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType(); 

}
