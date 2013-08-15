package org.sagebionetworks.tool.migration.v3;

import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.SynapseConnectionInfo;

/**
 * Simple implementation of the client factory
 * 
 * @author jmhill
 *
 */
public class SynapseClientFactoryImpl implements SynapseClientFactory {

	
	Configuration config;
	
	/**
	 * New factory with the required connection information.
	 * @param config
	 */
	public SynapseClientFactoryImpl(Configuration config) {
		super();
		this.config = config;
	}

	@Override
	public SynapseAdministration createNewSourceClient()throws SynapseException {
		return createNewConnection(this.config.getSourceConnectionInfo());
	}

	@Override
	public SynapseAdministration createNewDestinationClient() throws SynapseException {
		return createNewConnection(this.config.getDestinationConnectionInfo());
	}
	
	/**
	 * Create a new Synapse connection using the provided information.
	 * @param info
	 * @return
	 * @throws SynapseException 
	 */
	private static SynapseAdministration createNewConnection(SynapseConnectionInfo info) throws SynapseException{
		SynapseAdministration synapse = new SynapseAdministration();
		synapse.setAuthEndpoint(info.getAuthenticationEndPoint());
		synapse.setRepositoryEndpoint(info.getRepositoryEndPoint());
		synapse.setUserName(info.getUserName());
		synapse.setApiKey(info.getApiKey());
		return synapse;
	}

}
