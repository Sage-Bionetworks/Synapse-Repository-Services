package org.sagebionetworks.tool.migration;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;

public class ClearPreviews {

	
	/**
	 * Throw away program that will be used to clear a specific set of previews
	 * @param args
	 */
	public static void main(String[] args) {
		String sessionToken = "xxxx";
		String repoUrl = "http://localhost:8080/services-repository-develop-SNAPSHOT/repo/v1";
		String authUrl = "http://localhost:8080/services-repository-develop-SNAPSHOT/auth/v1";
		String fileEndpointUrl = "http://localhost:8080/services-repository-develop-SNAPSHOT/file/v1";
		
		Synapse synapseClient = new Synapse();		
		synapseClient.setSessionToken(sessionToken);
		synapseClient.setRepositoryEndpoint(repoUrl);
		synapseClient.setAuthEndpoint(authUrl);
		synapseClient.setFileEndpoint(fileEndpointUrl);
		synapseClient.appendUserAgent("ClearingPreviews");
		
		try {
			synapseClient.clearPreview("92");
		} catch (SynapseException e) {
			e.printStackTrace();
		}
	}

}
