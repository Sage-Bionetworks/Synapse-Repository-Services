package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.web.HttpRequestIdentifierUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class SessionIdCookieSetterFilterTest {

	@Mock
	FilterChain mockFilterChain;

	MockHttpServletRequest request;
	MockHttpServletResponse response;

	SessionIdCookieSetterFilter filter;
	@Before
	public void setup(){
		filter = new SessionIdCookieSetterFilter();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@Test
	public void testRequestContainsSessionCookie() throws IOException, ServletException {
		request.setCookies(new Cookie(HttpRequestIdentifierUtils.SESSION_ID_COOKIE_NAME, "somefakecookie"));

		filter.doFilter(request,response, mockFilterChain);

		Cookie cookie = response.getCookie(HttpRequestIdentifierUtils.SESSION_ID_COOKIE_NAME);

		assertNull(cookie);
		verify(mockFilterChain).doFilter(request, response);
	}

	@Test
	public void testRequestDoesNotContainCookie() throws IOException, ServletException {
		request.setCookies(null);

		filter.doFilter(request,response, mockFilterChain);

		Cookie cookie = response.getCookie(HttpRequestIdentifierUtils.SESSION_ID_COOKIE_NAME);

		assertNotNull(cookie);
		verify(mockFilterChain).doFilter(request, response);
	}
}
