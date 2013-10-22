package org.sagebionetworks.authutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.FetchRequest;

public class OpenIDConsumerUtilsTest {

	private static final String openIDEndpointURL = "https://www.FOOBAR.com/openid";
	private static final String openIDCallback = "/callback/of/DOOM";
	private static final String openIDIdentifier = "http://some.url.com/";
	
	private ConsumerManager mockManager;
	private AuthRequest mockAuthRequest;
	private DiscoveryInformation mockDiscInfo;
	private ParameterList mockRequestParameters;
	
	@Before
	public void setup() throws Exception {
		mockManager = mock(ConsumerManager.class);
		mockAuthRequest = mock(AuthRequest.class);
		mockDiscInfo = mock(DiscoveryInformation.class);
		
		when(mockManager.discover(any(String.class))).thenReturn(new ArrayList<Discovery>());
		when(mockManager.associate(anyList())).thenReturn(mockDiscInfo);
        when(mockManager.authenticate(any(DiscoveryInformation.class), any(String.class))).thenReturn(mockAuthRequest);
		
		// This is the minimal amount of parameters required to get past the static call to AuthSuccess.createAuthSuccess()
		// The resulting object is inconsequential to the mocking test
		// See: https://code.google.com/p/openid4java/source/browse/trunk/src/org/openid4java/message/AuthSuccess.java
		mockRequestParameters = new ParameterList();
		mockRequestParameters.set(new Parameter("openid.mode", "id_res"));
		mockRequestParameters.set(new Parameter("openid.return_to", openIDEndpointURL));
		mockRequestParameters.set(new Parameter("openid.assoc_handle", "dunno"));
		mockRequestParameters.set(new Parameter("openid.signed", "return_to,identity"));
		mockRequestParameters.set(new Parameter("openid.sig", "dunno"));
		mockRequestParameters.set(new Parameter("openid.identity", openIDIdentifier));

        // Use the mock
        OpenIDConsumerUtils.setConsumerManager(mockManager);
	}
	
	@AfterClass
	public static void teardown() throws Exception {
		// Get rid of the mock
		OpenIDConsumerUtils.setConsumerManager(new ConsumerManager());
	}

	@Test
	public void testAddQueryParameter() throws Exception {
		String queryParameter = "org.sagebionetworks.auth.sessionToken=2u362864826428";
		
		// Simple case
		String url = "https://foo.bar.com";
		assertEquals(url+"?"+queryParameter, OpenIDConsumerUtils.addRequestParameter(url, queryParameter));
		
		// Adding to existing param's
		url = "https://foo.bar.com?bas=blah";
		assertEquals(url+"&"+queryParameter, OpenIDConsumerUtils.addRequestParameter(url, queryParameter));
		
		// Inserting BEFORE a fragment
		url = "https://foo.bar.com?bas=blah";
		assertEquals(url+"&"+queryParameter+"#frag", OpenIDConsumerUtils.addRequestParameter(url+"#frag", queryParameter));

		// Url encoding required
		queryParameter = "sessionToken";
		String queryValue = "2u36286#826.28";
		String urlEncodedQueryValue = URLEncoder.encode(queryValue, "UTF-8");
		url = "https://foo.bar.com";
		assertEquals(url+"?"+queryParameter+"="+urlEncodedQueryValue, OpenIDConsumerUtils.addRequestParameter(url, queryParameter+"="+queryValue));
	}
	
	@Test
	public void testAuthRequest() throws Exception {
		OpenIDConsumerUtils.authRequest(openIDCallback);
		
		verify(mockManager).associate(anyList());
		verify(mockManager).authenticate(any(DiscoveryInformation.class), eq(openIDCallback));
		verify(mockAuthRequest).addExtension(any(FetchRequest.class));
	}
	

	@Test
    public void testVerifyResponse_badInfo() throws Exception {
		when(mockManager.verifyNonce(any(AuthSuccess.class), any(DiscoveryInformation.class))).thenReturn(false);
		
		OpenIDInfo result = OpenIDConsumerUtils.verifyResponse(mockRequestParameters);
		
		verify(mockManager).verifyNonce(any(AuthSuccess.class), any(DiscoveryInformation.class));
		assertNull(result);
    }
	
	@Test
    public void testVerifyResponse_success() throws Exception {
		when(mockManager.verifyNonce(any(AuthSuccess.class), any(DiscoveryInformation.class))).thenReturn(true);
		
		OpenIDInfo result = OpenIDConsumerUtils.verifyResponse(mockRequestParameters);
		
		verify(mockManager).verifyNonce(any(AuthSuccess.class), any(DiscoveryInformation.class));
		assertNotNull(result);
		assertEquals(openIDIdentifier, result.getIdentifier());
    }
}
