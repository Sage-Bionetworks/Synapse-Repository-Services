package org.sagebionetworks.repo.manager.file;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;

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
	 * Get the range of ids for the given association type
	 * 
	 * @param associationType The association type
	 * @return The {@link IdRange} for the table storing the given association type
	 */
	IdRange getIdRange(FileHandleAssociateType associationType);
	
	/**
	 * Returns the max size for scanning a range of ids for the given type
	 * 
	 * @param associationType The association type
	 * @return The max suggested range size for a scan request for the given association type
	 */
	long getMaxIdRangeSize(FileHandleAssociateType associationType);
	
	/**
	 * Return an iterable for all the file handles associatied with the given asociation type
	 * 
	 * @param associationType The association type
	 * @param range The range of ids to scan, inclusive
	 * @return An iterable over all the file handles found in the given range
	 */
	Iterable<ScannedFileHandleAssociation> scanRange(FileHandleAssociateType associationType, IdRange range);

}
