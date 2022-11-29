package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.utils.VirtualMachineIdProvider;
import org.sagebionetworks.aws.utils.s3.KeyGeneratorUtil;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthenticationMethod;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.AccessRecorder;
import org.sagebionetworks.util.TestClock;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultJwt;

/**
 * Unit test for AccessInterceptor.
 * 
 * @author jmhill
 *
 */
@ExtendWith(MockitoExtension.class)
public class AccessInterceptorTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	StackConfiguration mockConfiguration;
	@Mock
	Object mockHandler;

	UserInfo userInfo;

	@Mock
	AccessRecorder mockRecorder;

	@InjectMocks
	AccessInterceptor interceptor;

	@Mock
	OIDCTokenHelper mockOidcTokenHelper;

	Long userId;

	@Mock
	TestClock mockClock;

	@Mock
	AwsKinesisFirehoseLogger firehoseLogger;

	@Captor
	ArgumentCaptor<AccessRecord> recordCaptor;

	int instanceNumber;
	String stack;

	private static final String BEARER_TOKEN_HEADER = "Bearer some-token";
	private static final String OAUTH_CLIENT_ID = "9999";
	private static final String OAUTH_CLIENT_ID_BASIC = "2222";
	private static final String BASIC_AUTH_HEADER = "Basic " + Base64.getEncoder().encodeToString((OAUTH_CLIENT_ID_BASIC + ":password").getBytes(StandardCharsets.US_ASCII));

	@BeforeEach
	public void before() throws Exception {
		userId = 12345L;
		userInfo = new UserInfo(false, 123L);

		// Setup the happy mock
		when(mockRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn(userId.toString());
		when(mockRequest.getRequestURI()).thenReturn("/entity/syn789");
		when(mockRequest.getMethod()).thenReturn("DELETE");
		when(mockRequest.getHeader(any())).thenReturn(null);
		when(mockRequest.getHeader("Host")).thenReturn("localhost8080");
		when(mockRequest.getHeader("User-Agent")).thenReturn("HAL 2000");
		when(mockRequest.getRemoteAddr()).thenReturn("moon.org");
		when(mockRequest.getHeader("Origin")).thenReturn("http://www.example-social-network.com");
		when(mockRequest.getHeader("Via")).thenReturn("1.0 fred, 1.1 example.com");
		when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
		when(mockRequest.getQueryString()).thenReturn("?param1=foo");
		// setup response
		when(mockResponse.getStatus()).thenReturn(200);
		instanceNumber = 101;
		when(mockConfiguration.getStackInstanceNumber()).thenReturn(instanceNumber);
		stack = "dev";
		when(mockConfiguration.getStack()).thenReturn(stack);
		when(mockClock.currentTimeMillis()).thenReturn(100L, 200L);
	}
	
	
	
	@Test
	public void testHappyCase() throws Exception {
		when(mockRequest.getHeader(AuthorizationConstants.SYNAPSE_AUTHENTICATION_METHOD_HEADER_NAME)).thenReturn("SESSIONTOKEN");
		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		interceptor.setReturnObjectId("returnId");
		// finish the call
		Exception exception = null;
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, exception);
		// Now get the results from the stub
		verify(mockRecorder).save(recordCaptor.capture());
		AccessRecord result = recordCaptor.getValue();
		assertNotNull(result);
		assertEquals(100L, result.getElapseMS().longValue());
		assertTrue(result.getSuccess());
		assertNotNull(result.getSessionId());
		assertEquals("/entity/syn789", result.getRequestURL());
		assertEquals("DELETE", result.getMethod());
		assertEquals("localhost8080", result.getHost());
		assertEquals("HAL 2000", result.getUserAgent());
		assertEquals("moon.org", result.getXForwardedFor());
		assertEquals("http://www.example-social-network.com", result.getOrigin());
		assertEquals("1.0 fred, 1.1 example.com", result.getVia());
		assertEquals(new Long(Thread.currentThread().getId()), result.getThreadId());
		String stackInstanceNumber = KeyGeneratorUtil.getInstancePrefix(instanceNumber);
		assertEquals(stackInstanceNumber, result.getInstance());
		assertEquals(mockConfiguration.getStack(), result.getStack());
		assertEquals(VirtualMachineIdProvider.getVMID(), result.getVmId());
		assertEquals("?param1=foo", result.getQueryString());
		assertEquals("returnId", result.getReturnObjectId());
		assertNull(result.getOauthClientId()); // request was made without an OAuth client
		assertNull(result.getBasicAuthUsername());
		assertEquals(AuthenticationMethod.SESSIONTOKEN.name(), result.getAuthenticationMethod());
		verify(mockRequest).getHeader(AuthorizationConstants.SYNAPSE_AUTHENTICATION_METHOD_HEADER_NAME);
	}
	
	@Test
	public void testHappyCaseWithException() throws Exception{
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		// finish the call
		String error = "Something went horribly wrong!!!";
		Exception exception = new IllegalArgumentException(error);
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, exception);
		// Now get the results from the stub
		verify(mockRecorder).save(recordCaptor.capture());
		AccessRecord result = recordCaptor.getValue();
		assertNotNull(result);
		assertEquals(100L, result.getElapseMS().longValue());
		assertFalse(result.getSuccess());
	}
	
	@Test
	public void testStatusCode200() throws Exception{
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		// return a 200
		// setup response
		when(mockResponse.getStatus()).thenReturn(200);
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, null);
		// Now get the results from the stub
		verify(mockRecorder).save(recordCaptor.capture());
		AccessRecord result = recordCaptor.getValue();
		assertNotNull(result);
		assertEquals(100L, result.getElapseMS().longValue());
		assertTrue(result.getSuccess(), "200 is a success");
		assertEquals(new Long(200), result.getResponseStatus());
	}
	
	@Test
	public void testStatusCode400() throws Exception{
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		// return a 200
		// setup response
		when(mockResponse.getStatus()).thenReturn(400);
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, null);
		// Now get the results from the stub
		verify(mockRecorder).save(recordCaptor.capture());
		AccessRecord result = recordCaptor.getValue();
		assertNotNull(result);
		assertEquals(100L, result.getElapseMS().longValue());
		assertFalse(result.getSuccess(), "400 is not a success");
		assertEquals(new Long(400), result.getResponseStatus());
	}

	@Test
	public void testGetOAuthClientIdFromBearerJwt() throws Exception {
		// Put the client ID in the JWT access token
		when(mockRequest.getHeader("Authorization")).thenReturn(BEARER_TOKEN_HEADER);
		when(mockRequest.getHeader("authenticationMethod")).thenReturn("BEARERTOKEN");
		Claims claims = new DefaultClaims();
		claims.setAudience(OAUTH_CLIENT_ID);
		Jwt<JwsHeader,Claims> jwt = new DefaultJwt(null, claims);
		when(mockOidcTokenHelper.parseJWT(any())).thenReturn(jwt);

		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		interceptor.setReturnObjectId("returnId");
		// Wait to add some elapse time
		mockClock.sleep(234);
		// finish the call
		Exception exception = null;
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, exception);
		// Now get the results from the stub
		verify(mockRecorder).save(recordCaptor.capture());
		AccessRecord result = recordCaptor.getValue();

		// Other fields are tested in testHappyCase
		assertEquals(OAUTH_CLIENT_ID, result.getOauthClientId());
		assertNull(result.getBasicAuthUsername());
		assertEquals("BEARERTOKEN", result.getAuthenticationMethod());
	}

	@Test
	public void testGetOAuthClientIdFromBasicAuthCredsAndHeader() throws Exception {
		// Put the client ID in the basic auth header.
		when(mockRequest.getHeader("Authorization")).thenReturn(BASIC_AUTH_HEADER);
		when(mockRequest.getHeader(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER)).thenReturn(OAUTH_CLIENT_ID_BASIC);
		when(mockRequest.getHeader("authenticationMethod")).thenReturn("BASICAUTH");

		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		interceptor.setReturnObjectId("returnId");
		// Wait to add some elapse time
		mockClock.sleep(234);
		// finish the call
		Exception exception = null;
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, exception);
		// Now get the results from the stub
		verify(mockRecorder).save(recordCaptor.capture());
		AccessRecord result = recordCaptor.getValue();

		// Other fields are tested in testHappyCase
		assertEquals(OAUTH_CLIENT_ID_BASIC, result.getOauthClientId());
		assertEquals(OAUTH_CLIENT_ID_BASIC, result.getBasicAuthUsername());
		assertEquals("BASICAUTH", result.getAuthenticationMethod());
	}
}
