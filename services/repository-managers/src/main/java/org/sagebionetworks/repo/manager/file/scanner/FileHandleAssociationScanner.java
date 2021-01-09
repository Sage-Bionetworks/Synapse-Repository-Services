package org.sagebionetworks.repo.manager.file.scanner;

/**
 * Abstraction for scanning the file handles associated with objects
 * 
 * @author Marco Marasca
 *
 */
public interface FileHandleAssociationScanner {
	
	/** 
	 * @return The minimum and maximum id of the association object table
	 */
	IdRange getIdRange();
	
	/**
	 * Scan all the file handles that fall in the given id range and returns an iterable
	 * 
	 * @param range The range of ids to scan
	 * @param batchSize The maximum number of elements to inspect in a single query
	 * @return An iterable over all the file handles found in the given range
	 */
	Iterable<ScannedFileHandle> scanRange(IdRange range, long batchSize);

}
