package org.sagebionetworks.client;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Abstraction for the Synapse Administration client.
 * 
 * @author jmhill
 *
 */
public interface SynapseAdministrationInt extends SynapseInt {

	/**
	 * Update the current stack status.
	 * @param updated
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public StackStatus updateCurrentStackStatus(StackStatus updated) throws JSONObjectAdapterException, SynapseException;
}
