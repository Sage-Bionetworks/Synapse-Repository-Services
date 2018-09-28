package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;

public class HttpRequestIdentifierUtilsTest {
	MockHttpServletRequest request;

	private final String sessionId = "SESSION ID";
	private Long userId = 123L;
	private String ipAddress = "192.168.1.1";
	private String requestPath = "/pathy/mcPathFace";

	@Before
	public void setUp(){
		request = new MockHttpServletRequest();
	}

	@Test
	public void testGetSessionId_FromHeaders(){
		request.addHeader(HttpRequestIdentifierUtils.SESSION_HEADER_NAME, sessionId);

		assertEquals(sessionId, HttpRequestIdentifierUtils.getSessionId(request));
	}

	@Test
	public void testGetSessionId_HeaderNotExist(){
		assertNull(HttpRequestIdentifierUtils.getSessionId(request));
	}

	@Test
	public void testGetRequestIdentifier(){
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId.toString());
		request.addHeader(HttpRequestIdentifierUtils.SESSION_HEADER_NAME, sessionId);
		request.setRequestURI(requestPath);
		request.setRemoteAddr(ipAddress);


		HttpRequestIdentifier expected = new HttpRequestIdentifier(userId,sessionId,ipAddress,requestPath);
		assertEquals(expected, HttpRequestIdentifierUtils.getRequestIdentifier(request));
	}
}
