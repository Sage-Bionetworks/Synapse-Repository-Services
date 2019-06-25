/**
 * Adapted from {@link com.amazonaws.auth.AWSCredentialsProviderChain} for use with Google Cloud.
 */
package org.sagebionetworks.gcp;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.PropertyProvider;
import org.sagebionetworks.PropertyProviderImpl;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;

/**
 * Synapse credential chain will attempt to load AWS credentials in the
 * following order:
 * <ol>
 * <li>sage.bionetworks properties from Maven .m2/settings.xml file</li>
 * <li>sage.bionetworks properties from Java System.properties</i>
 * <li>the default system credentials</li>
 * </ol>
 */
public class SynapseGCPCredentialsProviderChain implements CredentialsProvider {

	private List<CredentialsProvider> credentialsProviders = new LinkedList<>();
	static private Logger log = LogManager.getLogger(SynapseGCPCredentialsProviderChain.class);

	private static final SynapseGCPCredentialsProviderChain INSTANCE =
			new SynapseGCPCredentialsProviderChain(new PropertyProviderImpl(), new DefaultGCPCredentialsProvider());

	/**
	 * Dependency injection constructor.
	 * @param propertyProvider
	 */
	SynapseGCPCredentialsProviderChain(PropertyProvider propertyProvider, CredentialsProvider defaultProvider) {
		credentialsProviders.add(new MavenSettingsGCPCredentialsProvider(propertyProvider));
		credentialsProviders.add(new SynapseSystemPropertiesGCPCredentialsProvider(propertyProvider));
		credentialsProviders.add(defaultProvider);
	}

	/**
	 * Access the singleton chain.
	 * @return
	 */
	public static SynapseGCPCredentialsProviderChain getInstance() {
		return INSTANCE;
	}

	@Override
	public Credentials getCredentials() {
		List<String> exceptionMessages = null;
		for (CredentialsProvider provider : credentialsProviders) {
			try {
				Credentials credentials = provider.getCredentials();
				if (credentials != null) {
					log.debug("Loading credentials from " + provider.toString());
					return credentials;
				}
			} catch (Exception e) {
				// Ignore any exceptions and move onto the next provider
				String message = provider + ": " + e.getMessage();
				log.debug("Unable to load credentials from " + message);
				if (exceptionMessages == null) {
					exceptionMessages = new LinkedList<>();
				}
				exceptionMessages.add(message);
			}
		}
		throw new RuntimeException(
				"Unable to load Google Cloud credentials from any provider in the chain: "
				+ exceptionMessages);
	}
}
