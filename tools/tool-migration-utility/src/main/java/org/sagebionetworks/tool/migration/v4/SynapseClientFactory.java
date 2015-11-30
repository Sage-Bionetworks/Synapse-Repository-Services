package org.sagebionetworks.tool.migration.v4;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;

/**
 * Factory for creating SynapseAdministration clients.
 * 
 * @author jmhill
 *
 */
public interface SynapseClientFactory {

	/**
	 * Create a new client connected to the source stack.
	 * @return
	 * @throws SynapseException
	 */
	public SynapseAdminClient createNewSourceClient() throws SynapseException;
	
	/**
	 * Create a new client connected to the destination stack
	 * @return
	 * @throws SynapseException
	 */
	public SynapseAdminClient createNewDestinationClient() throws SynapseException;
}
