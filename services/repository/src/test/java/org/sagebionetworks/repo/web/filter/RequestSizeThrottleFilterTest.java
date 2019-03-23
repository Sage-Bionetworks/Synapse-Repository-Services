package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class RequestSizeThrottleFilterTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	FilterChain mockChain;
	@Mock
	ServletInputStream mockInputStream;
	@Mock
	PrintWriter mockPrintWriter;
	
	byte[] dataToRead;
	
	RequestSizeThrottleFilter filter;
	
	String readFromStream;
	
	long maxBytes;
	int arrayIndex;
	
	@Before
	public void before() throws Exception {
		maxBytes = 101L;
		filter = new RequestSizeThrottleFilter();

		
		when(mockRequest.getInputStream()).thenReturn(mockInputStream);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		// This is the data that the input stream will read.
		dataToRead = "some data".getBytes("UTF-8");
		
		arrayIndex = 0;
		doAnswer(new Answer<Integer>() {

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				// read one byte from the stream.
				if(arrayIndex < dataToRead.length) {
					return new Integer(dataToRead[arrayIndex++]);
				}else {
					// end of stream.
					return -1;
				}
			}
		}).when(mockInputStream).read();
		
		// Setup the mock chain to read the entire stream
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				HttpServletRequest request = (HttpServletRequest) invocation.getArguments()[0];
				HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
				ServletInputStream inputStream = request.getInputStream();
				// read all of the data from the stream.
				StringBuilder builder = new StringBuilder();
				int charInt = 0;
				while((charInt = inputStream.read())>-1){
					builder.append((char)charInt);
				}
				readFromStream = builder.toString();
				// setup okay
				response.setStatus(200);
				return null;
			}
		}).when(mockChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		
	}
	
	@Test
	public void testReadUnderLimit() throws IOException, ServletException {
		// Set the limit larger than the data size.
		maxBytes = dataToRead.length+1;
		ReflectionTestUtils.setField(filter, "maximumInputStreamBytes", maxBytes);
		filter.doFilter(mockRequest, mockResponse, mockChain);
		// should result in 200 status
		verify(mockResponse).setStatus(200);
		// the data should be read from the stream
		assertEquals("some data", readFromStream);
	}
	
	@Test (expected=ByteLimitExceededException.class)
	public void testReadOverLimit() throws IOException, ServletException {
		// Set limit smaller than the data size
		maxBytes = dataToRead.length-1;
		ReflectionTestUtils.setField(filter, "maximumInputStreamBytes", maxBytes);
		// call under test
		filter.doFilter(mockRequest, mockResponse, mockChain);
	}
	
}
