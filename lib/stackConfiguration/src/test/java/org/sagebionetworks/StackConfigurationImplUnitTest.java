package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *  To test methods in StackConfigurationImpl that are not directly related to the stack
 *
 *  This class is missing a lot of tests! Please add them :)
 */
@ExtendWith(MockitoExtension.class)
public class StackConfigurationImplUnitTest {
	
	StackConfigurationImpl config;

	@Mock
	ConfigurationProperties mockProperties;
	
	@Mock
	StackEncrypter stackEncrypter;
	
	@BeforeEach
	public void before() {
		this.config = new StackConfigurationImpl(mockProperties, stackEncrypter);
	}

	@Test
	public void testGetDoiPrefixOnProd() {
		String stackName = "prod";
		String prefix = "a prefix";
		when(mockProperties.getProperty(StackConstants.STACK_PROPERTY_NAME)).thenReturn(stackName); // config.isProduction() will return true
		String doiPrefixPropertyName = "org.sagebionetworks.doi.prefix";
		when(mockProperties.getProperty(DOI_PREFIX_PROPERTY)).thenReturn(prefix);

		// Call under test
		String actualPrefix = config.getDoiPrefix();

		assertEquals(prefix, actualPrefix);
		verify(mockProperties).getProperty(StackConstants.STACK_PROPERTY_NAME);
		verify(mockProperties, never()).getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME);
		verify(mockProperties).getProperty(doiPrefixPropertyName);

	}

	@Test
	public void testGetDoiPrefixNotOnProd() {
		String stackPropertyName = "testNotProd";
		String stackInstancePropertyName = "testStack";
		String prefix = "a prefix";
		when(mockProperties.getProperty(StackConstants.STACK_PROPERTY_NAME)).thenReturn(stackPropertyName); // config.isProduction() will return false
		when(mockProperties.getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME)).thenReturn(stackInstancePropertyName);
		String doiPrefixPropertyName = "org.sagebionetworks.doi.prefix";
		when(mockProperties.getProperty(doiPrefixPropertyName)).thenReturn(prefix);

		String expectedPrefix = prefix + "/" + stackInstancePropertyName;

		// Call under test
		String actualPrefix = config.getDoiPrefix();

		assertEquals(expectedPrefix, actualPrefix);
		verify(mockProperties).getProperty(StackConstants.STACK_PROPERTY_NAME);
		verify(mockProperties).getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME);
		verify(mockProperties).getProperty(doiPrefixPropertyName);
	}
	
	@Test
	public void testGetSharedS3BackupBucket() {
		String stackName = "prod";
		when(mockProperties.getProperty(StackConstants.STACK_PROPERTY_NAME)).thenReturn(stackName);
		when(mockProperties.getProperty("org.sagebionetworks.shared.s3.backup.bucket")).thenReturn(".bucket");
		assertEquals("prod.bucket", config.getSharedS3BackupBucket());
	}


	private static final String DOI_PREFIX_PROPERTY = "org.sagebionetworks.doi.prefix";

	@Test
	public void testGetTempCredentialsIamRoleArn() {
		when(mockProperties.hasProperty(StackConfigurationImpl.CONFIG_KEY_STS_IAM_ARN)).thenReturn(true);
		when(mockProperties.getProperty(StackConfigurationImpl.CONFIG_KEY_STS_IAM_ARN)).thenReturn("dummy-arn");

		// Method under test
		String result = config.getTempCredentialsIamRoleArn();
		assertEquals("dummy-arn", result);
	}

	@Test
	public void testGtTempCredentialsIamRoleArn_NullConfig() {
		when(mockProperties.hasProperty(StackConfigurationImpl.CONFIG_KEY_STS_IAM_ARN)).thenReturn(false);

		// Method under test
		String result = config.getTempCredentialsIamRoleArn();
		assertNull(result);

		verify(mockProperties, never()).getProperty(any());
	}
	
	@Test
	public void testGetServiceAuthKey() {
		String serviceName = "someService";
		
		// Call under test
		config.getServiceAuthKey(serviceName);
		
		verify(stackEncrypter).getDecryptedProperty("org.sagebionetworks." + serviceName + ".auth.key");
	}
	
	@Test
	public void testGetServiceAuthSecret() {
		String serviceName = "someService";
		
		// Call under test
		config.getServiceAuthSecret(serviceName);
		
		verify(stackEncrypter).getDecryptedProperty("org.sagebionetworks." + serviceName + ".auth.secret");
	}
	
	@Test
	public void testGetCloudMailInUser() {
		
		// Call under test
		config.getCloudMailInUser();
		
		verify(stackEncrypter).getDecryptedProperty("org.sagebionetworks." + StackConfiguration.SERVICE_CLOUDMAILIN + ".auth.key");
	}
	
	@Test
	public void testGetCloudMailInPassword() {
		
		// Call under test
		config.getCloudMailInPassword();
		
		verify(stackEncrypter).getDecryptedProperty("org.sagebionetworks." + StackConfiguration.SERVICE_CLOUDMAILIN + ".auth.secret");
	}
	
	@Test
	public void testGetDockerRegistryUser() {
		
		// Call under test
		config.getDockerRegistryUser();
		
		verify(stackEncrypter).getDecryptedProperty("org.sagebionetworks." + StackConfiguration.SERVICE_DOCKER_REGISTRY + ".auth.key");
	}
	
	@Test
	public void testGetDockerRegistryPassword() {
		
		// Call under test
		config.getDockerRegistryPassword();
		
		verify(stackEncrypter).getDecryptedProperty("org.sagebionetworks." + StackConfiguration.SERVICE_DOCKER_REGISTRY + ".auth.secret");
	}
	
	@Test
	public void testGetRepositoryServiceProdEndpoint() {
		
		// Call under test
		config.getRepositoryServiceProdEndpoint();
		
		verify(mockProperties).getProperty("org.sagebionetworks.repositoryservice.endpoint.prod");
		
	}
}
