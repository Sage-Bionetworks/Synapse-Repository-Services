package org.sagebionetworks.repo.web.filter;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class JsonpCallbackFilterTest {

	@BeforeEach
	public void setUp() throws Exception {
	}
	
	private static final String RESPONSE_BODY="{name=René}";
	private static final String CHARACTER_ENCODING = "ISO-8859-1";
	private static final String JSON_FUNCTION_NAME = "myfunction";
	
	private static MockHttpServletRequest createHttpServletRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest(){
			public Map<String,String[]> getParameterMap() {
				Map<String,String[]> result = new HashMap<String,String[]>();
				result.put("callback", new String[]{JSON_FUNCTION_NAME});
				return result;
			}
		};
		return request;
	}
	
	private static HttpServletResponse createHttpServletResponse(final OutputStream os) {
		HttpServletResponse response = new MockHttpServletResponse() {
			public ServletOutputStream getOutputStream() {
				return new FilterServletOutputStream(os);}
			
			private PrintWriter writer = null;
	
			public PrintWriter getWriter() {
				if (writer!=null) return writer;
				try {
					String charsetName = getCharacterEncoding();
					if (charsetName==null) charsetName= "ISO-8859-1";
					writer = new PrintWriter(new OutputStreamWriter(os, charsetName), true);
					return writer;
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}	
		};
		
		return response;
	}

	@Test
	public void testPrintWriter() throws Exception {
		JsonpCallbackFilter filter = new JsonpCallbackFilter();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		HttpServletRequest request = createHttpServletRequest();
		HttpServletResponse response = createHttpServletResponse(baos);
		
		FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response) throws IOException,
					ServletException {
				response.setContentType("application/json; charset="+CHARACTER_ENCODING);
				PrintWriter writer = response.getWriter();
				writer.print(RESPONSE_BODY);
				writer.flush();
			}};
		filter.doFilter(request, response, filterChain);
		
		assertEquals(JSON_FUNCTION_NAME+"("+RESPONSE_BODY+");", 
				new String(baos.toByteArray(), response.getCharacterEncoding()));
	}

	@Test
	public void testOutputStream() throws Exception {
		JsonpCallbackFilter filter = new JsonpCallbackFilter();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		HttpServletRequest request = createHttpServletRequest();
		HttpServletResponse response = createHttpServletResponse(baos);
		
		FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response) throws IOException,
					ServletException {
				response.setContentType("application/json; charset="+CHARACTER_ENCODING);
				OutputStream os = response.getOutputStream();
				os.write(RESPONSE_BODY.getBytes(CHARACTER_ENCODING));
				os.flush();
			}};
		filter.doFilter(request, response, filterChain);
		
		assertEquals(JSON_FUNCTION_NAME+"("+RESPONSE_BODY+");", 
				new String(baos.toByteArray(), response.getCharacterEncoding()));
	}


	@Test
	public void testRejectRequestWithSessionTokenHeader() throws Exception {
		JsonpCallbackFilter filter = new JsonpCallbackFilter();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		MockHttpServletRequest request = createHttpServletRequest();
		request.addHeader("sessionToken", "xxxxxxx");

		HttpServletResponse response = createHttpServletResponse(baos);
		
		FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response) throws IOException,
					ServletException {
			}};
			
		assertThrows(ServletException.class, () -> {
			filter.doFilter(request, response, filterChain);
		});
		
	}


	@Test
	public void testRejectRequestWithAuthorizationHeader() throws Exception {
		JsonpCallbackFilter filter = new JsonpCallbackFilter();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		MockHttpServletRequest request = createHttpServletRequest();
		request.addHeader("Authorization", "Bearer xxxx");

		HttpServletResponse response = createHttpServletResponse(baos);
		
		FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response) throws IOException,
					ServletException {
			}};
			
		assertThrows(ServletException.class, () -> {
			filter.doFilter(request, response, filterChain);
		});
		
	}

}
