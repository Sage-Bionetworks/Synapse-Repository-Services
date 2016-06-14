package org.sagebionetworks.tool.migration.v3;

import org.sagebionetworks.client.HttpClientProvider;
import org.sagebionetworks.client.HttpClientProviderImpl;
import org.sagebionetworks.client.SharedClientConnection;
import org.sagebionetworks.client.SynapseAdminClientImpl;
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
	public SynapseAdminClientImpl createNewSourceClient() throws SynapseException {
		return createNewConnection(this.config.getSourceConnectionInfo());
	}

	@Override
	public SynapseAdminClientImpl createNewDestinationClient() throws SynapseException {
		return createNewConnection(this.config.getDestinationConnectionInfo());
	}
	
	/**
	 * Create a new Synapse connection using the provided information.
	 * @param info
	 * @return
	 * @throws SynapseException 
	 */
	private static SynapseAdminClientImpl createNewConnection(SynapseConnectionInfo info) throws SynapseException{
		HttpClientProvider provider = new HttpClientProviderImpl();
		provider.setGlobalConnectionTimeout(1000*15); 	// 15 secs
		provider.setGlobalSocketTimeout(1000*60*2);		//  2 mins
		SynapseAdminClientImpl synapse = new SynapseAdminClientImpl(provider);
		synapse.setAuthEndpoint(info.getAuthenticationEndPoint());
		synapse.setRepositoryEndpoint(info.getRepositoryEndPoint());
		synapse.setUserName(info.getUserName());
		synapse.setApiKey(info.getApiKey());
		return synapse;
	}

}
