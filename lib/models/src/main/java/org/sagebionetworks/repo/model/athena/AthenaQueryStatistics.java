package org.sagebionetworks.repo.model.athena;

/**
 * Statistics about an Athena query execution, contains information about the execution time as well as the amount of
 * data scanned.
 * 
 * @author Marco
 *
 */
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
