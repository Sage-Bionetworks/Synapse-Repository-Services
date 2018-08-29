package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;

public class HttpRequestIdentifierUtilsTest {
	MockHttpServletRequest request;

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
		String sessionId = "SESSION ID";
		request.setCookies(new Cookie("IrrelevantCookie", "IrrlevantValue"),
				new Cookie(HttpRequestIdentifierUtils.SESSION_ID_COOKIE_NAME, sessionId));

		assertEquals(sessionId, HttpRequestIdentifierUtils.getSessionId(request));
	}

	@Test
	public void testGetSessionId_SessionCookieNotFound(){
		String sessionId = "SESSION ID";
		request.setCookies(new Cookie("IrrelevantCookie", "IrrlevantValue"));

		assertNull(HttpRequestIdentifierUtils.getSessionId(request));
	}
}
