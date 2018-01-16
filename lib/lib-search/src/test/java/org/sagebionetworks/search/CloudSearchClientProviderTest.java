package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchClientProviderTest {

	@Mock
	private AmazonCloudSearchDomainClient mockAwsCloudSearchClient;

	@Mock
	private SearchDomainSetupImpl mockSearchDomainSetup;

	private String fakeEndpoint;

	private CloudSearchClientProvider provider;

	@Before
	public void setup(){
		provider = new CloudSearchClientProvider();
		provider.setSearchEnabled(true);
		fakeEndpoint = "http://endpoint.com";

		ReflectionTestUtils.setField(provider,"searchDomainSetup", mockSearchDomainSetup);
		ReflectionTestUtils.setField(provider,"awsCloudSearchDomainClient", mockAwsCloudSearchClient);
		ReflectionTestUtils.setField(CloudSearchClientProvider.class, "singletonWrapper", null);
	}

	@Test (expected = IllegalStateException.class)
	public void testGetCloudSearchClientSearchDisabled(){
		provider.setSearchEnabled(false);
		provider.getCloudSearchClient();
	}

	@Test
	public void testGetCloudSearchClientIncompleteSetup(){
		when(mockSearchDomainSetup.postInitialize()).thenReturn(false);
		try{
			provider.getCloudSearchClient();
		}catch (TemporarilyUnavailableException e) {
			verify(mockSearchDomainSetup, times(1)).postInitialize();
			verify(mockAwsCloudSearchClient, never()).setEndpoint(anyString());
		}
	}

	@Test
	public void testGetCloudSearchClientCompletedSetupSingleton(){
		when(mockSearchDomainSetup.postInitialize()).thenReturn(true);
		when(mockSearchDomainSetup.getDomainSearchEndpoint()).thenReturn(fakeEndpoint);

		//get cloudsearch client twice
		assertSame(provider.getCloudSearchClient(), provider.getCloudSearchClient());

		//verify that the setup only occurred once
		verify(mockSearchDomainSetup, times(1)).postInitialize();
		verify(mockAwsCloudSearchClient, times(1)).setEndpoint(fakeEndpoint);
	}

}
