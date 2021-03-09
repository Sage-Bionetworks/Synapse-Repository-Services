package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;

/**
 * Manager for the file handle association scanner jobs, takes care of starting the driver job and processing the association ranges
 */
public interface FileHandleAssociationScannerJobManager {
	
	/**
	 * Process a range of ids for a given type of association according to the given request specification.
	 * 
	 * @param request The scan range request
	 * @throws RecoverableException If the request failed but can be retried on a later time
	 * return The total number of scanned records that contained file handles
	 */
	int processScanRangeRequest(FileHandleAssociationScanRangeRequest request) throws RecoverableException;	

}
