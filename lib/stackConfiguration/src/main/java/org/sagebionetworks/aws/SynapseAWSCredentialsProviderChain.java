package org.sagebionetworks.aws;

import org.sagebionetworks.PropertyProvider;
import org.sagebionetworks.PropertyProviderImpl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

/**
 * Synapse credential chain will attempt to load AWS credentials in the
 * following order:
 * <ol>
 * <li>sage.bionetworks properties from Maven .m2/settings.xml file</li>
 * <li>sage.bionetworks properties from Java System.properties</i>
 * <li>The default AWS chain provider {@link DefaultAWSCredentialsProviderChain}</li>
 * </ol>
 */
public class SynapseAWSCredentialsProviderChain extends AWSCredentialsProviderChain {

	private static final SynapseAWSCredentialsProviderChain INSTANCE = new SynapseAWSCredentialsProviderChain(
			new PropertyProviderImpl(), new DefaultAWSCredentialsProviderChain());

	/**
	 * Dependency injection constructor.
	 * @param propertyProvider
	 */
	SynapseAWSCredentialsProviderChain(PropertyProvider propertyProvider, AWSCredentialsProvider defaultProvider) {
		super(new MavenSettingsAWSCredentialsProvider(propertyProvider),
				new SynapseSystemPropertiesAWSCredentialsProvider(propertyProvider),
				defaultProvider);
	}

	/**
	 * Access the singleton chain.
	 * @return
	 */
	public static SynapseAWSCredentialsProviderChain getInstance() {
		return INSTANCE;
	}
}
