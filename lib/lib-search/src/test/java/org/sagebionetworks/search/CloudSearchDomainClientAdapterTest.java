package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchDomainClientAdapterTest {

	@Mock
	AmazonCloudSearchDomainClient mockCloudSearchDomainClient;

	CloudsSearchDomainClientAdapter cloudSearchDomainClientAdapter;

	@Mock
	SearchResult mockResponse;

	SearchRequest searchRequest;

	String endpoint = "http://www.ImALittleEmdpoint.com";
	@Before
	public void before() {
		//TODO: fix
		MockitoAnnotations.initMocks(this);
		cloudSearchDomainClientAdapter = new CloudsSearchDomainClientAdapter(new BasicAWSCredentials("FakeKey", "FakeKey")); //TODO:z fix
		ReflectionTestUtils.setField(cloudSearchDomainClientAdapter, "client", mockCloudSearchDomainClient);
		searchRequest = new SearchRequest().withQuery("aQuery");
	}


	@Test
	public void testPLFM2968NoError() throws Exception {
		//TODO: mock SearchUtils?
		cloudSearchDomainClientAdapter.setEndpoint(endpoint);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenReturn(mockResponse);
		assertEquals(mockResponse, cloudSearchDomainClientAdapter.search(searchRequest));
		verify(mockCloudSearchDomainClient).search(searchRequest);
		verify(mockCloudSearchDomainClient).setEndpoint(endpoint);
	}
//
//	@Test
//	public void testPLFM2968NoRecover() throws Exception {
//		//TODO: fix
//		when(mockResponse.getStatusCode()).thenReturn(507);
//		when(mockCloudSearchDomainClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
//		try {
//			cloudSearchDomainClientAdapter.search(searchRequest);
//		} catch (SearchException e) {
//			assertEquals(507, e.getStatusCode());
//		} finally {
//			verify(mockCloudSearchDomainClient, times(6)).get(any(SimpleHttpRequest.class));
//		}
//	}
//
//	@Test
//	public void testPLFM3777() throws Exception {
//		//TODO: fix
//		when(mockResponse.getStatusCode()).thenReturn(504);
//		when(mockCloudSearchDomainClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
//		try {
//			cloudSearchDomainClientAdapter.search(searchRequest);
//		} catch (SearchException e) {
//			assertEquals(504, e.getStatusCode());
//		} finally {
//			verify(mockCloudSearchDomainClient).get(any(SimpleHttpRequest.class));
//		}
//	}

}
