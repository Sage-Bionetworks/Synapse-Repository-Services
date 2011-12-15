package org.sagebionetworks.tool.migration;

import org.sagebionetworks.client.AcceptAllCertificateHttpClientProvider;
import org.sagebionetworks.client.Synapse;
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
	public Synapse createNewConnection(SynapseConnectionInfo info) throws SynapseException{
		// We need to accept all SSL certificates for this client.
//		AcceptAllCertificateHttpClientProvider clientProvider = new AcceptAllCertificateHttpClientProvider();
//		Synapse synapse = new Synapse(clientProvider);
		Synapse synapse = new Synapse();
		synapse.setAuthEndpoint(info.getAuthenticationEndPoint());
		synapse.setRepositoryEndpoint(info.getRepositoryEndPoint());
		synapse.login(info.getAdminUsername(), info.getAdminPassword());
		return synapse;
	}

	@Override
	public Synapse createNewSourceClient() throws SynapseException {
		// Create a factory using the source info.
		return createNewConnection(Configuration.getSourceConnectionInfo());
	}

	@Override
	public Synapse createNewDestinationClient() throws SynapseException {
		// Create a factory using the destination info
		return createNewConnection(Configuration.getDestinationConnectionInfo());
	}

}
