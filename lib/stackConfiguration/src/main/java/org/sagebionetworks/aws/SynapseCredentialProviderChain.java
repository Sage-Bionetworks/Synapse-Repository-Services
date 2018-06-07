package org.sagebionetworks.aws;

import org.sagebionetworks.PropertyProviderImpl;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;

/**
 * Synapse credential chain will attempt to load AWS credentials in the
 * following order:
 * <ol>
 * <li>Maven .m2/settings.xml file</li>
 * <li>EC2 container role.</li>
 * </ol>
 *
 */
public class SynapseCredentialProviderChain extends AWSCredentialsProviderChain {
	
	private static final SynapseCredentialProviderChain INSTANCE = new SynapseCredentialProviderChain();

	public SynapseCredentialProviderChain() {
		super(new MavenSettingsAwsCredentialProvider(new PropertyProviderImpl()),
				new EC2ContainerCredentialsProviderWrapper());
	}

	public static SynapseCredentialProviderChain getInstance() {
		return INSTANCE;
	}
}
