package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.mock.web.MockHttpServletRequest;

public class HttpRequestIdentifierUtilsTest {
	MockHttpServletRequest request;

	private final String sessionId = "SESSION ID";
	private final Cookie irrelevantCookie = new Cookie("IrrelevantCookie", "IrrlevantValue");
	private final Cookie sessionIdCookie = new Cookie(HttpRequestIdentifierUtils.SESSION_ID_COOKIE_NAME, sessionId);
	private Long userId = 123L;
	private String ipAddress = "192.168.1.1";
	private String requestPath = "/pathy/mcPathFace";

	@Before
	public void setUp(){
		request = new MockHttpServletRequest();
	}

	@Test
	public void testGetSessionId_NullCookies(){
		request.setCookies(null);

		assertNull(HttpRequestIdentifierUtils.getSessionId(request));
	}

	@Test
	public void testGetSessionId_SessionCookieFound(){
		request.setCookies(irrelevantCookie,sessionIdCookie);

		assertEquals(sessionId, HttpRequestIdentifierUtils.getSessionId(request));
	}

	@Test
	public void testGetSessionId_SessionCookieNotFound(){
		request.setCookies(irrelevantCookie);

		assertNull(HttpRequestIdentifierUtils.getSessionId(request));
	}

	@Test
	public void testGetRequestIdentifier(){
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId.toString());
		request.setCookies(sessionIdCookie);
		request.setRequestURI(requestPath);
		request.setRemoteAddr(ipAddress);


		HttpRequestIdentifier expected = new HttpRequestIdentifier(userId,sessionId,ipAddress,requestPath);
		assertEquals(expected, HttpRequestIdentifierUtils.getRequestIdentifier(request));
	}
}
