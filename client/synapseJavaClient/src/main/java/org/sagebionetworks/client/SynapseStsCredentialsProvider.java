package org.sagebionetworks.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;

/**
 * This provider wraps around the Synapse STS API (temporary credentials) and automatically renews credentials when
 * they expire.
 */
public class SynapseStsCredentialsProvider implements AWSCredentialsProvider {
	// Instance invariants.
	private final SynapseClient synapseClient;
	private final String entityId;
	private final StsPermission permission;

	// State.
	private AWSCredentials credentials;
	private long expiration;

	public SynapseStsCredentialsProvider(SynapseClient synapseClient, String entityId, StsPermission permission) {
		this.synapseClient = synapseClient;
		this.entityId = entityId;
		this.permission = permission;
	}

	/** {@inheritDoc} */
	@Override
	public AWSCredentials getCredentials() {
		if (credentials == null || System.currentTimeMillis() > expiration) {
			refresh();
		}
		return credentials;
	}

	/** {@inheritDoc} */
	@Override
	public void refresh() {
		// Get credentials from server.
		StsCredentials stsCredentials;
		try {
			stsCredentials = synapseClient.getTemporaryCredentialsForEntity(entityId, permission);
		} catch (SynapseException ex) {
			// Wrap in a runtime exception, since refresh() can't throw.
			throw new RuntimeException(ex);
		}

		// Convert to AWS Credentials.
		credentials = new BasicSessionCredentials(stsCredentials.getAccessKeyId(), stsCredentials.getSecretAccessKey(),
				stsCredentials.getSessionToken());
		expiration = stsCredentials.getExpiration().getTime();
	}
}
