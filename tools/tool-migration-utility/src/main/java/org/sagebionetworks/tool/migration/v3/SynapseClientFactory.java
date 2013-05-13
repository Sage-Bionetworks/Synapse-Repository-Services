package org.sagebionetworks.tool.migration.v3;

import org.sagebionetworks.client.SynapseAdministrationInt;
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
	public SynapseAdministrationInt createNewSourceClient() throws SynapseException;
	
	/**
	 * Create a new client connected to the destination stack
	 * @return
	 * @throws SynapseException
	 */
	public SynapseAdministrationInt createNewDestinationClient() throws SynapseException;
}
