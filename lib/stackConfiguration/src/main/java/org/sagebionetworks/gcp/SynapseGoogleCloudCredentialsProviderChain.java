/**
 * Adapted from {@link com.amazonaws.auth.AWSCredentialsProviderChain} for use with Google Cloud.
 */
package org.sagebionetworks.gcp;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.PropertyProviderImpl;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;

/**
 * Synapse credential chain will attempt to load Google Cloud credentials in the
 * following order:
 * <ol>
 * <li>sage.bionetworks properties from Maven .m2/settings.xml file</li>
 * <li>sage.bionetworks properties from Java System.properties</i>
 * <li>the default system credentials</li>
 * </ol>
 */
public class SynapseGoogleCloudCredentialsProviderChain implements CredentialsProvider {

	private List<CredentialsProvider> credentialsProviders = new LinkedList<>();
	static private Logger log = LogManager.getLogger(SynapseGoogleCloudCredentialsProviderChain.class);

	public SynapseGoogleCloudCredentialsProviderChain(List<CredentialsProvider> providers) {
		if (providers == null || providers.isEmpty()) {
			throw new IllegalArgumentException("At least one Google Cloud credentials provider must be given.");
		}
		credentialsProviders.addAll(providers);
	}

	private static final SynapseGoogleCloudCredentialsProviderChain INSTANCE =
			new SynapseGoogleCloudCredentialsProviderChain(
					Arrays.asList(
							new MavenSettingsGoogleCloudCredentialsProvider(new PropertyProviderImpl()),
							new SynapseSystemPropertiesGoogleCloudCredentialsProvider(new PropertyProviderImpl()),
							new DefaultGoogleCloudCredentialsProvider()
					)
			);

	/**
	 * Access the singleton chain.
	 * @return
	 */
	public static SynapseGoogleCloudCredentialsProviderChain getInstance() {
		return INSTANCE;
	}

	@Override
	public Credentials getCredentials() {
		List<String> exceptionMessages = new LinkedList<>();
		for (CredentialsProvider provider : credentialsProviders) {
			try {
				Credentials credentials = provider.getCredentials();
				if (credentials != null) {
					log.debug("Loading credentials from " + provider.toString());
					return credentials;
				}
			} catch (Exception e) {
				// Ignore any exceptions and move onto the next provider
				String providerExceptionMessage = provider + ": " + e.getMessage();
				log.debug("Unable to load credentials from " + providerExceptionMessage);
				exceptionMessages.add(providerExceptionMessage);
			}
		}
		throw new RuntimeException(
				"Unable to load Google Cloud credentials from any provider in the chain: "
				+ exceptionMessages);
	}
}
