package org.sagebionetworks.repo.model.athena;

public interface AthenaQueryStatistics {

	/**
	 * @return The query execution time in ms spent on the Athena engine
	 */
	Long getExecutionTime();

	/**
	 * @return The number of bytes scanned by the Athena engine
	 */
	Long getDataScanned();

}
