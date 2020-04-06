package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

@ExtendWith(MockitoExtension.class)
public class UserInfoMethodArgumentResolverTest {
	
	@Mock
	private MethodParameter parameter;
	
	@Mock
	private OpenIDConnectManager oidcManager;
	
	@Mock
	private NativeWebRequest webRequest;

	@InjectMocks
	private UserInfoMethodArgumentResolver resolver;
	
	private static final String ACCESS_TOKEN = "access token";
	private static final UserInfo USER_INFO = new UserInfo(false);

	@Test
	void testSupportsParameter() throws Exception {
		// unsupported type
		when(parameter.getParameterType()).thenReturn((Class)String.class);
		
		// method under test
		assertFalse(resolver.supportsParameter(parameter));
		
		// supported type
		when(parameter.getParameterType()).thenReturn((Class)UserInfo.class);
		
		// method under test
		assertTrue(resolver.supportsParameter(parameter));
	}

	@Test
	void testResolveArgument() throws Exception {
		when(webRequest.getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME)).thenReturn("Bearer "+ACCESS_TOKEN);
		when(oidcManager.getUserAuthorization(ACCESS_TOKEN)).thenReturn(USER_INFO);
		
		// method under test
		assertEquals(USER_INFO, resolver.resolveArgument(null, null, webRequest, null));
		
		// if header is missing an exception should be thrown
		when(webRequest.getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME)).thenReturn(null);
		
		// method under test
		assertThrows(IllegalArgumentException.class, ()->{resolver.resolveArgument(null, null, webRequest, null);});
	}

}
