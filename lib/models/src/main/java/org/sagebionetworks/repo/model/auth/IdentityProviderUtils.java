package org.sagebionetworks.repo.model.auth;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;

/**
 * Converts an {@link IdentityProvider} to and from the single name that identifies it wherever one has
 * to be persisted or carried in a token. Synapse itself is named, and every other provider is named by
 * its {@link OAuthProvider}, so a stored or transmitted name is unambiguous across both.
 */
public class IdentityProviderUtils {

	/** The name of {@link SynapseIdentityProvider}, Synapse itself. */
	public static final String SYNAPSE_IDENTITY_PROVIDER = "SYNAPSE";

	/**
	 * The name of the given identity provider, or null if there is none.
	 */
	public static String toName(IdentityProvider identityProvider) {
		if (identityProvider == null) {
			return null;
		}
		if (identityProvider instanceof SynapseIdentityProvider) {
			return SYNAPSE_IDENTITY_PROVIDER;
		}
		if (identityProvider instanceof OAuthIdentityProvider) {
			return ((OAuthIdentityProvider) identityProvider).getProvider().name();
		}
		throw new IllegalArgumentException("Unexpected type " + identityProvider.getClass().getName());
	}

	/**
	 * The identity provider the given name refers to, or null if there is none.
	 *
	 * @throws IllegalArgumentException if the name refers to no known provider
	 */
	public static IdentityProvider fromName(String name) {
		if (name == null) {
			return null;
		}
		if (SYNAPSE_IDENTITY_PROVIDER.equals(name)) {
			return new SynapseIdentityProvider();
		}
		return new OAuthIdentityProvider().setProvider(OAuthProvider.valueOf(name));
	}
}
