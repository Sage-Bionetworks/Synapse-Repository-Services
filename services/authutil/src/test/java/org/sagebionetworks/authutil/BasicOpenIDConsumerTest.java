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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ax.FetchRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class BasicOpenIDConsumerTest {
	
	private static final String openIDEndpoint = "FOOBAR";
	private static final String openIDEndpointURL = "https://www.FOOBAR.com/openid";
	private static final String openIDCallback = "/callback/of/DOOM";
	
	private ConsumerManager mockManager;
	private AuthRequest mockAuthRequest;
	private DiscoveryInformation mockDiscInfo;
	private MockHttpServletRequest mockRequest;
	private MockHttpServletResponse mockResponse;
	
	@Before
	public void setup() throws Exception {
		mockManager = mock(ConsumerManager.class);
		mockAuthRequest = mock(AuthRequest.class);
		mockDiscInfo = mock(DiscoveryInformation.class);
		mockRequest = new MockHttpServletRequest();
		mockResponse = new MockHttpServletResponse();
		
		when(mockManager.discover(eq(openIDEndpoint))).thenReturn(new ArrayList<Discovery>());
		when(mockManager.associate(anyList())).thenReturn(mockDiscInfo);
        when(mockManager.authenticate(eq(mockDiscInfo), eq(openIDCallback))).thenReturn(mockAuthRequest);
        when(mockAuthRequest.getDestinationUrl(eq(true))).thenReturn(openIDEndpointURL);
		
		// This is the minimal amount of parameters required to get past the static call to AuthSuccess.createAuthSuccess()
		// The resulting object is inconsequential to the mocking test
		// See: https://code.google.com/p/openid4java/source/browse/trunk/src/org/openid4java/message/AuthSuccess.java
		mockRequest.setParameter("openid.mode", "id_res");
		mockRequest.setParameter("openid.return_to", openIDEndpointURL);
		mockRequest.setParameter("openid.assoc_handle", "dunno");
		mockRequest.setParameter("openid.signed", "return_to,identity");
		mockRequest.setParameter("openid.sig", "dunno");
		mockRequest.setParameter("openid.identity", "dunno");

        // Use the mock
        BasicOpenIDConsumer.setConsumerManager(mockManager);
	}
	
	@After
	public void teardown() throws Exception {
		// Get rid of the mock
		BasicOpenIDConsumer.setConsumerManager(new ConsumerManager());
	}
	
	@Test 
	public void testSerializationRoundTrip() throws Exception {
		List<Integer> o = new ArrayList<Integer>();
		o.add(1);
		o.add(2);
		o.add(100);
		o.add(-10000);
		
		String e = BasicOpenIDConsumer.encryptingSerializer(o);

		ArrayList<Integer> o2 = BasicOpenIDConsumer.decryptingDeserializer(e);
		assertEquals(o, o2);
	}
	
	@Test
	public void testAuthRequest() throws Exception {
		BasicOpenIDConsumer.authRequest(openIDEndpoint, openIDCallback, mockRequest, mockResponse);
		
		verify(mockManager).discover(eq(openIDEndpoint));
		verify(mockManager).associate(anyList());
		verify(mockManager).authenticate(eq(mockDiscInfo), eq(openIDCallback));
		verify(mockAuthRequest).addExtension(any(FetchRequest.class));
		
		// Must be present
		Cookie discInfoCookie = mockResponse.getCookie(OpenIDInfo.DISCOVERY_INFO_COOKIE_NAME);
		assertNotNull(discInfoCookie);
		
		assertEquals(openIDEndpointURL, mockResponse.getRedirectedUrl());
	}

	@Test(expected=RuntimeException.class)
    public void testVerifyResponse_NeedsDiscoveryInfoParam() throws Exception {
		BasicOpenIDConsumer.verifyResponse(mockRequest);
    }

	@Test
    public void testVerifyResponse_badInfo() throws Exception {
		mockRequest.setParameter(OpenIDInfo.DISCOVERY_INFO_PARAM_NAME, BasicOpenIDConsumer.encryptingSerializer(mockDiscInfo));
		when(mockManager.verifyNonce(any(AuthSuccess.class), any(DiscoveryInformation.class))).thenReturn(false);
		
		OpenIDInfo result = BasicOpenIDConsumer.verifyResponse(mockRequest);
		
		verify(mockManager).verifyNonce(any(AuthSuccess.class), any(DiscoveryInformation.class));
		assertNull(result);
    }

	@Test
    public void testVerifyResponse_success() throws Exception {
		mockRequest.setParameter(OpenIDInfo.DISCOVERY_INFO_PARAM_NAME, BasicOpenIDConsumer.encryptingSerializer(mockDiscInfo));
		when(mockManager.verifyNonce(any(AuthSuccess.class), any(DiscoveryInformation.class))).thenReturn(true);
		
		OpenIDInfo result = BasicOpenIDConsumer.verifyResponse(mockRequest);
		
		verify(mockManager).verifyNonce(any(AuthSuccess.class), any(DiscoveryInformation.class));
		assertNotNull(result);
    }
}
