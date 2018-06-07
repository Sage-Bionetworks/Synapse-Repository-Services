package org.sagebionetworks.aws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.PropertyProvider;

import com.amazonaws.auth.AWSCredentials;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public class MavenSettingsAwsCredentialProviderTest {

	@Mock
	PropertyProvider mockPropertyProvider;

	MavenSettingsAwsCredentialProvider credentialProvider;

	String accessId = "theAccessId";
	String accessSecret = "theAccessSecret";
	Properties settings;

	@Before
	public void before() {
		accessId = "theAccessId";
		accessSecret = "theAccessSecret";
		settings = new Properties();
		settings.put(MavenSettingsAwsCredentialProvider.ORG_SAGEBIONETWORKS_STACK_IAM_ID, accessId);
		settings.put(MavenSettingsAwsCredentialProvider.ORG_SAGEBIONETWORKS_STACK_IAM_KEY, accessSecret);
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
	}

	/**
	 * Case where settings exists with valid values.
	 */
	@Test
	public void testChainWithSettings() {
		MavenSettingsAwsCredentialProvider credentialProvider = new MavenSettingsAwsCredentialProvider(
				mockPropertyProvider);
		// call under test
		AWSCredentials creds = credentialProvider.getCredentials();
		assertNotNull(creds);
		assertEquals(accessId, creds.getAWSAccessKeyId());
		assertEquals(accessSecret, creds.getAWSSecretKey());
	}

	/**
	 * Case where the setting do not exist
	 */
	@Test
	public void testChainWithSettingsNull() {
		Properties settings = null;
		when(mockPropertyProvider.getMavenSettingsProperties()).thenReturn(settings);
		MavenSettingsAwsCredentialProvider credentialProvider = new MavenSettingsAwsCredentialProvider(
				mockPropertyProvider);
		try {
			// call under test
			credentialProvider.getCredentials();
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains(MavenSettingsAwsCredentialProvider.AWS_CREDENTIALS_WERE_NOT_FOUND));
		}
	}
	
	/**
	 * Case where settings exists but ID is missing
	 */
	@Test
	public void testChainWithSettingsKeyMissing() {
		settings.remove(MavenSettingsAwsCredentialProvider.ORG_SAGEBIONETWORKS_STACK_IAM_ID);
		MavenSettingsAwsCredentialProvider credentialProvider = new MavenSettingsAwsCredentialProvider(
				mockPropertyProvider);
		try {
			// call under test
			credentialProvider.getCredentials();
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains(MavenSettingsAwsCredentialProvider.AWS_CREDENTIALS_WERE_NOT_FOUND));
		}
	}
	
	/**
	 * Case where settings exists but ID is missing
	 */
	@Test
	public void testChainWithSettingsSecretMissing() {
		settings.remove(MavenSettingsAwsCredentialProvider.ORG_SAGEBIONETWORKS_STACK_IAM_KEY);
		MavenSettingsAwsCredentialProvider credentialProvider = new MavenSettingsAwsCredentialProvider(
				mockPropertyProvider);
		try {
			// call under test
			credentialProvider.getCredentials();
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains(MavenSettingsAwsCredentialProvider.AWS_CREDENTIALS_WERE_NOT_FOUND));
		}
	}
	
	/**
	 * Case where settings exists but contains neither key or secret.
	 */
	@Test
	public void testChainWithSettingsBothMissing() {
		settings.clear();
		MavenSettingsAwsCredentialProvider credentialProvider = new MavenSettingsAwsCredentialProvider(
				mockPropertyProvider);
		try {
			// call under test
			credentialProvider.getCredentials();
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains(MavenSettingsAwsCredentialProvider.AWS_CREDENTIALS_WERE_NOT_FOUND));
		}
	}
}
