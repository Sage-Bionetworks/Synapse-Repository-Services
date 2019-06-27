package org.sagebionetworks.gcp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SynapseGoogleCloudCredentialsProviderChainTest {

	@Mock
	CredentialsProvider mockProvider1;
	@Mock
	CredentialsProvider mockProvider2;
	@Mock
	CredentialsProvider mockProvider3;
	@Mock
	Credentials mockCredentials;

	SynapseGoogleCloudCredentialsProviderChain chain;

	@BeforeEach
	public void before() throws Exception {
		chain = new SynapseGoogleCloudCredentialsProviderChain(Arrays.asList(mockProvider1, mockProvider2, mockProvider3));
	}

	@Test
	public void testChainGetFirstOption() throws Exception {
		when(mockProvider1.getCredentials()).thenReturn(mockCredentials);

		// call under test
		Credentials creds = chain.getCredentials();
		assertNotNull(creds);

		verify(mockProvider1).getCredentials();
		verify(mockProvider2, never()).getCredentials();
		verify(mockProvider3, never()).getCredentials();
	}

	@Test
	public void testChainGetMiddleOption() throws Exception {
		when(mockProvider1.getCredentials()).thenReturn(null);
		when(mockProvider2.getCredentials()).thenReturn(mockCredentials);

		// call under test
		Credentials creds = chain.getCredentials();
		assertNotNull(creds);

		verify(mockProvider1).getCredentials();
		verify(mockProvider2).getCredentials();
		verify(mockProvider3, never()).getCredentials();
	}

	@Test
	public void getLastOption() throws Exception {
		when(mockProvider1.getCredentials()).thenReturn(null);
		when(mockProvider2.getCredentials()).thenReturn(null);
		when(mockProvider3.getCredentials()).thenReturn(mockCredentials);

		// call under test
		Credentials creds = chain.getCredentials();
		assertNotNull(creds);

		verify(mockProvider1).getCredentials();
		verify(mockProvider2).getCredentials();
		verify(mockProvider3).getCredentials();
	}

	@Test
	public void testChainNoCreds() throws Exception {
		when(mockProvider1.getCredentials()).thenReturn(null);
		when(mockProvider2.getCredentials()).thenReturn(null);
		when(mockProvider3.getCredentials()).thenReturn(null);

		// call under test
		assertThrows(RuntimeException.class, () -> chain.getCredentials());

		verify(mockProvider1).getCredentials();
		verify(mockProvider2).getCredentials();
		verify(mockProvider3).getCredentials();
	}

	@Test
	public void testConstructorNoProviders() {
		assertThrows(IllegalArgumentException.class, () -> new SynapseGoogleCloudCredentialsProviderChain(null));
		assertThrows(IllegalArgumentException.class, () -> new SynapseGoogleCloudCredentialsProviderChain(Collections.emptyList()));
	}
}
