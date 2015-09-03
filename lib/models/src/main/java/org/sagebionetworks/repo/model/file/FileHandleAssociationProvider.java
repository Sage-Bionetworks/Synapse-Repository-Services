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
	 * Get the distinct FileHandleAssociation for the given set of FileHandleIds.  The resulting set
	 * should only include FileHandleIds that are actually associated with the given objectId.
	 * @param fileHandleIds List of FileHandleIds to test.
	 * @param objectId The associated object id.
	 * @return
	 */
	public Set<String> getDistinctAssociationsForFileHandleIds(List<String> fileHandleIds, String objectId);

	/**
	 * Get the ObjectType for this FileHandleAssociationType.
	 * @return
	 */
	public ObjectType getObjectTypeForAssociationType(); 

}
