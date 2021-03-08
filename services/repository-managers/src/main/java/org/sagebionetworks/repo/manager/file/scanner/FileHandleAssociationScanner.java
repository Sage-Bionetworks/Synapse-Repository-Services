package org.sagebionetworks.repo.manager.file.scanner;

import org.sagebionetworks.repo.model.file.IdRange;

/**
 * Abstraction for scanning the file handles associated with objects
 * 
 * @author Marco Marasca
 *
 */
public interface FileHandleAssociationScanner {
	
	long MAX_ID_RANGE_SIZE = 100000;
	
	/**
	 * @return Hint for the maximium range of id to scan in one call 
	 */
	default long getMaxIdRangeSize() {
		return MAX_ID_RANGE_SIZE;
	}
	
	/** 
	 * @return The minimum and maximum id of the association object table
	 */
	IdRange getIdRange();
	
	/**
	 * Scan all the file handles that fall in the given id range and returns an iterable
	 * 
	 * @param range The range of ids to scan, inclusive
	 * @return An iterable over all the file handles found in the given range
	 */
	Iterable<ScannedFileHandleAssociation> scanRange(IdRange range);

}
