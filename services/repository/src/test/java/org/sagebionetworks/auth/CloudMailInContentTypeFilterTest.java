package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ExtendWith(MockitoExtension.class)
class CloudMailInContentTypeFilterTest {

	MockHttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	FilterChain mockChain;

	@Captor
	ArgumentCaptor<HttpServletRequest> requestCaptor;


	@BeforeEach
	public void setUp(){
		mockRequest = new MockHttpServletRequest("POST", "/repo/v1/cloudMailInMessage");
	}


	@Test
	public void testFilter() throws IOException, ServletException {
		CloudMailInCharacterEncodingFilter filter = new CloudMailInCharacterEncodingFilter();

		//set request's initial value to something else
		mockRequest.setCharacterEncoding("ISO-8859-1");

		filter.doFilter(mockRequest, mockResponse, mockChain);

		verify(mockChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		//assert replaced by UTF-8
		assertEquals("utf-8", requestCaptor.getValue().getCharacterEncoding());
	}

}