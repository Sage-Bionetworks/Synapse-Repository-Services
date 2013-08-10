package org.sagebionetworks.tool.migration;

import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;

/**
 * 
 * @author John
 *
 */
public class ClientFactoryImpl implements ClientFactory {
	
	/**
	 * Create a new Synapse connection using the provided information.
	 * @param info
	 * @return
	 * @throws SynapseException 
	 */
	public SynapseAdministration createNewConnection(SynapseConnectionInfo info) throws SynapseException{
		// We need to accept all SSL certificates for this client.
//		AcceptAllCertificateHttpClientProvider clientProvider = new AcceptAllCertificateHttpClientProvider();
//		Synapse synapse = new Synapse(clientProvider);
		SynapseAdministration synapse = new SynapseAdministration();
		synapse.setAuthEndpoint(info.getAuthenticationEndPoint());
		synapse.setRepositoryEndpoint(info.getRepositoryEndPoint());
		synapse.setApiKey(info.getApiKey());
		return synapse;
	}

	@Override
	public SynapseAdministration createNewSourceClient(Configuration configuration) throws SynapseException {
		// Create a factory using the source info.
		return createNewConnection(configuration.getSourceConnectionInfo());
	}

	@Override
	public SynapseAdministration createNewDestinationClient(Configuration configuration) throws SynapseException {
		// Create a factory using the destination info
		return createNewConnection(configuration.getDestinationConnectionInfo());
	}

}
