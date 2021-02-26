package org.sagebionetworks.repo.manager.file;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

/**
 * Abstraction for all of the different type of FileHandleAssociationsProviders.
 * This switch will simply defer all calls to the correct provider for each
 * type.
 *
 */
public interface FileHandleAssociationManager {

	/**
	 * Given a set of file handle ids and an associated object, get the sub-set of
	 * ids that are associated with the requested object including file handles ids that reference 
	 * a preview and that are linked to the object.
	 * 
	 * @param fileHandleIds List of FileHandleIds to test.
	 * @param objectId The associated object id.
	 * @param associationType The associated object type
	 * @return
	 */
	Set<String> getFileHandleIdsAssociatedWithObject(List<String> fileHandleIds, String objectId, FileHandleAssociateType associationType);
	
	/**
	 * What ObjectType matches the FileHandleAssociationType?
	 * 
	 * @param associationType
	 * @return
	 */
	ObjectType getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType associationType);
	
	/**
	 * @param associateType The type of association
	 * @return An instance of a scanner that can be used to iterate over all the file handles associated with the given type
	 */
	FileHandleAssociationScanner getFileHandleAssociationScanner(FileHandleAssociateType associateType);

}
