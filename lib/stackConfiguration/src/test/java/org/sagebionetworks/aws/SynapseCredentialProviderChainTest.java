package org.sagebionetworks.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.aws.AbstractSynapseCredentialProvider.ORG_SAGEBIONETWORKS_STACK_IAM_ID;
import static org.sagebionetworks.aws.AbstractSynapseCredentialProvider.ORG_SAGEBIONETWORKS_STACK_IAM_KEY;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.PropertyProvider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

@RunWith(MockitoJUnitRunner.class)
public class SynapseCredentialProviderChainTest {

	@Mock
	PropertyProvider mockPropertyProvider;
	@Mock
	AWSCredentialsProvider mockDefaultProvider;

	String accessId;
	String accessSecret;
	Properties settings;
	Properties system;

	@Before
	public void before() {
		accessId = "theAccessId";
		accessSecret = "theAccessSecret";
		//setup settings
		String settingsSufffix = "-settings";
		settings = new Properties();
		settings.put(ORG_SAGEBIONETWORKS_STACK_IAM_ID, accessId+settingsSufffix);
		settings.put(ORG_SAGEBIONETWORKS_STACK_IAM_KEY, accessSecret+settingsSufffix);
		
		// setup system
		String systemSufffix = "-system";
		system = new Properties();
		system.put(ORG_SAGEBIONETWORKS_STACK_IAM_ID, accessId+systemSufffix);
		system.put(ORG_SAGEBIONETWORKS_STACK_IAM_KEY, accessSecret+systemSufffix);
		
		String defaultSuffix = "-default";
		when(mockDefaultProvider.getCredentials()).thenReturn(new BasicAWSCredentials(accessId+defaultSuffix, accessSecret+defaultSuffix));
	}

	/**
	 * Case where settings exists with valid values.
	 */
	@Test
	public void testChainWithSettings() {
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		assertEquals("theAccessId-settings", creds.getAWSAccessKeyId());
		assertEquals("theAccessSecret-settings", creds.getAWSSecretKey());
		// default provider should not be invoked
		verify(mockDefaultProvider, never()).getCredentials();
	}
	
	/**
	 * Case where system exists with valid values.
	 */
	@Test
	public void testChainWithSysetm() {
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(system);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		assertEquals("theAccessId-system", creds.getAWSAccessKeyId());
		assertEquals("theAccessSecret-system", creds.getAWSSecretKey());
		// default provider should not be invoked
		verify(mockDefaultProvider, never()).getCredentials();
	}
	
	/**
	 * Case where the default provider creates the credentials.
	 * 
	 */
	@Test
	public void testChainWithDefault() {
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		assertEquals("theAccessId-default", creds.getAWSAccessKeyId());
		assertEquals("theAccessSecret-default", creds.getAWSSecretKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}
	
	
	/**
	 * Case settings exists but does not contain the keys
	 * 
	 */
	@Test
	public void testChainWithSettingsMissingValues() {
		settings.clear();
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		assertEquals("theAccessId-default", creds.getAWSAccessKeyId());
		assertEquals("theAccessSecret-default", creds.getAWSSecretKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}
	
	/**
	 * Case settings exists but is missing the id;
	 * 
	 */
	@Test
	public void testChainWithSettingsMissingSecret() {
		settings.remove(ORG_SAGEBIONETWORKS_STACK_IAM_ID);
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		// default provider should be used
		verify(mockDefaultProvider, times(1)).getCredentials();
	}
	
	/**
	 * Case settings exists but is missing the key;
	 * 
	 */
	@Test
	public void testChainWithSettingsMissingKey() {
		settings.remove(ORG_SAGEBIONETWORKS_STACK_IAM_KEY);
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(null);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}
	
	/**
	 * Case system exists but does not contain the keys.
	 * 
	 */
	@Test
	public void testChainWithSystemMissingValues() {
		system.clear();
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(system);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		assertEquals("theAccessId-default", creds.getAWSAccessKeyId());
		assertEquals("theAccessSecret-default", creds.getAWSSecretKey());
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}
	
	/**
	 * Case system exists but does not contain the ID.
	 */
	@Test
	public void testChainWithSystemMissingId() {
		system.remove(ORG_SAGEBIONETWORKS_STACK_IAM_ID);
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(system);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}
	
	/**
	 * Case system exists but does not contain the keys.
	 */
	@Test
	public void testChainWithSystemMissingKey() {
		system.remove(ORG_SAGEBIONETWORKS_STACK_IAM_KEY);
		// both system and settings are null
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(null);
		when(mockPropertyProvider.getSystemProperties()).thenReturn(system);
		
		SynapseCredentialProviderChain chain = createChain();
		// call under test
		AWSCredentials creds = chain.getCredentials();
		assertNotNull(creds);
		// default provider should be invoked
		verify(mockDefaultProvider, times(1)).getCredentials();
	}
	
	/**
	 * Create a new chain.
	 * @return
	 */
	SynapseCredentialProviderChain createChain() {
		return new SynapseCredentialProviderChain(mockPropertyProvider, mockDefaultProvider);
	}
}
