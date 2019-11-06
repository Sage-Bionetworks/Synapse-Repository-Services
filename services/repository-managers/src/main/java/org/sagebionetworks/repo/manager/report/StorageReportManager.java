package org.sagebionetworks.repo.manager.report;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

/**
 * Business logic for creating reports about Synapse storage usage statistics.
 *
 */
public interface StorageReportManager {

	/**
	 * Generate a storage report and stream the results to the passed CSVWriter. This method will stream over
	 * the rows and will not keep the row data in memory. This method can be used to stream over results sets that are
	 * larger than the available system memory, as long as the caller does not hold the resulting rows in memory.
	 *
	 * @param user The caller to authorize
	 * @param request The
	 * @param writer
	 * @return
	 * @throws NotFoundException
	 * @throws LockUnavilableException
	 */
	void writeStorageReport(UserInfo user, DownloadStorageReportRequest request, CSVWriterStream writer) throws NotFoundException, LockUnavilableException;
}
