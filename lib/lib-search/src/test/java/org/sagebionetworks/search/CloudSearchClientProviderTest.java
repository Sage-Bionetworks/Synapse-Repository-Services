package org.sagebionetworks.search;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

@ExtendWith(MockitoExtension.class)
public class CloudSearchClientProviderTest {

	@Mock
	private SearchDomainSetupImpl mockSearchDomainSetup;

	private String fakeEndpoint;

	@InjectMocks
	private CloudSearchClientProvider provider;

	@BeforeEach
	public void setup() {
		provider.setSearchEnabled(true);
		fakeEndpoint = "http://endpoint.com";
	}

	@Test
	public void testGetCloudSearchClientSearchDisabled(){
		provider.setSearchEnabled(false);
		
		assertThrows(IllegalStateException.class, () -> {
			provider.getCloudSearchClient();
		});
	}

	@Test
	public void testGetCloudSearchClientSetup(){
		when(mockSearchDomainSetup.postInitialize()).thenReturn(false);
		
		assertThrows(TemporarilyUnavailableException.class, () -> {
			provider.getCloudSearchClient();
		});
		
		verify(mockSearchDomainSetup, times(1)).postInitialize();
		
		when(mockSearchDomainSetup.postInitialize()).thenReturn(true);
		when(mockSearchDomainSetup.getDomainSearchEndpoint()).thenReturn(fakeEndpoint);

		//get cloudsearch client twice
		assertSame(provider.getCloudSearchClient(), provider.getCloudSearchClient());

		//verify that the setup only occurred once
		verify(mockSearchDomainSetup, times(2)).postInitialize();
	}

}
