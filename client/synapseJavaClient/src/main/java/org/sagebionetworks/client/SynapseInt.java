package org.sagebionetworks.client;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Abstraction for Synpase.
 * 
 * @author jmhill
 *
 */
public interface SynapseInt {

	/**
	 * Get the current status of the stack.
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public StackStatus getCurrentStackStatus() throws SynapseException,	JSONObjectAdapterException;
	
	/**
	 * Get the endpoint of the repository service.
	 * @return
	 */
	public String getRepoEndpoint();
}
