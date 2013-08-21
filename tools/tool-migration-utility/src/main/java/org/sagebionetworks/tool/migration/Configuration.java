package org.sagebionetworks.tool.migration;

/**
 * Provides configuration information
 * 
 * @author John
 * 
 */
public interface Configuration {

	/**
	 * Get the source connection information.
	 * 
	 * @return
	 */
	public SynapseConnectionInfo getSourceConnectionInfo();

	/**
	 * Get the destination connection information.
	 * 
	 * @return
	 */
	public SynapseConnectionInfo getDestinationConnectionInfo();

	public int getMaximumNumberThreads();

	/**
	 * The Maximum batch size.
	 * 
	 * @return
	 */
	public int getMaximumBatchSize();

	public long getWorkerTimeoutMs();

	/**
	 * Maximum number of migration retries
	 */
	public int getMaxRetries();
	
	/**
	 * Defer exceptions
	 */
	public boolean getDeferExceptions();

}
