package org.sagebionetworks.dynamo;

/**
 * Sets up DynamoDB.
 *
 * @author ewu
 */
public interface DynamoSetup {

	/**
	 * The default timeout to wait for DynamoDB to create or update tables.
	 */
	public static final long TIMEOUT_IN_MILLIS = 30 * 60 * 1000; // 30 minitues

	/**
	 * Creates and updates tables according to configuration. If a table
	 * does not exist, it will be created. If a table already exists but
	 * is different only in provisioned throughput, the throughput will be
	 * updated. If a table already exists but different other than the
	 * provisioned throughput, TableExistsException is thrown. This method
	 * blocks on creation. It either times out or finishes when the tables
	 * will be active and ready for use.
	 */
	void setup(DynamoConfig config) throws DynamoTableExistsException, DynamoTimeoutException;

	/**
	 * Creates and updates tables according to configuration. If a table
	 * does not exist, it will be created. If a table already exists but
	 * is different only in provisioned throughput, the throughput will be
	 * updated. If a table already exists but different other than the
	 * provisioned throughput, TableExistsException is thrown.
	 *
	 * @param blockOnCreation Whether to wait for the tables to be active
	 * @param timeoutInMillis How long to wait for the tables to be active
	 */
	void setup(boolean blockOnCreation, long timeoutInMillis, DynamoConfig config);
}
