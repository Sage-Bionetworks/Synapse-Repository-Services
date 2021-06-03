package org.sagebionetworks.repo.web.filter;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;

@ExtendWith({MockitoExtension.class})
public class StackStatusFilterTest extends AbstractAutowiredControllerTestBase {
	
	private static final String CURRENT_STATUS_2 = " for StackStatusInterceptorTest.test";
	private static final String CURRENT_STATUS_1 = "Setting the status to ";
	private static final String MSG_FORMAT = CURRENT_STATUS_1 + "%s" + CURRENT_STATUS_2;
	
	private static final String GATED_URI = "/repo/v1/entity";
	
	private static final String REASON_JSON_FORMAT = "{\"reason\":\"%s\"}";
	private static final String REASON_JSON_FORMAT_WITH_MESSAGE_FORMAT = String.format("{\"reason\":\"%s\"}", MSG_FORMAT);
	
	
	@Mock
	private HttpServletRequest mockHttpRequest;
	
	@Mock
	private HttpServletResponse mockHttpResponse;
	
	@Mock
	private PrintWriter mockPrintWriter;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@Mock
	private StackStatusDao stackStatusDao;
		
	@InjectMocks
	private StackStatusFilter filter;
	
	/**
	 * Helper to mock the status.
	 * @param toSet
	 */
	private void mockFullStackStatus(StatusEnum toSet, String msg){
		when(stackStatusDao.getCurrentStatus()).thenReturn(toSet);

		StackStatus status = new StackStatus();
		status.setStatus(toSet);
		status.setCurrentMessage(statusMessage(toSet, msg));
		status.setPendingMaintenanceMessage("Pending the completion of StackStatusInterceptorTest.test");
		when(stackStatusDao.getFullCurrentStatus()).thenReturn(status);
	}

	private String statusMessage(StatusEnum status, String msg) {
		if ((msg != null) && (! msg.isEmpty())) {
			return String.format(msg, status);
		}
		return msg;
	}
		
	@Test
	public void testStatusMessage() {
		assertNull(statusMessage(StatusEnum.READ_ONLY, null));
		assertEquals("", statusMessage(StatusEnum.READ_ONLY, ""));
		assertEquals(String.format(MSG_FORMAT, StatusEnum.READ_ONLY), statusMessage(StatusEnum.READ_ONLY, MSG_FORMAT));
	}

	@Test
	public void testReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		when(stackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		when(mockHttpRequest.getRequestURI()).thenReturn(GATED_URI);
		
		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
	}
	
	@Test
	public void testReadOnly() throws Exception {
		// Set the status to be read only
		mockFullStackStatus(StatusEnum.READ_ONLY, MSG_FORMAT);
		when(mockHttpResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);

		verify(mockFilterChain, never()).doFilter(mockHttpRequest, mockHttpResponse);
		verify(mockHttpResponse).setStatus(503);
		verify(mockPrintWriter).println(String.format(REASON_JSON_FORMAT_WITH_MESSAGE_FORMAT, StatusEnum.READ_ONLY));
	}

	@Test
	public void testReadOnlyNullMsg() throws Exception {
		// Set the status to be read only
		mockFullStackStatus(StatusEnum.READ_ONLY, null);
		when(mockHttpResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);

		verify(mockFilterChain, never()).doFilter(mockHttpRequest, mockHttpResponse);
		verify(mockHttpResponse).setStatus(503);
		verify(mockPrintWriter).println(String.format(REASON_JSON_FORMAT, "Synapse is down for maintenance."));
	}

	@Test
	public void testReadOnlyEmptyMsg() throws Exception {
		// Set the status to be read only
		mockFullStackStatus(StatusEnum.READ_ONLY, "");
		when(mockHttpResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);

		verify(mockFilterChain, never()).doFilter(mockHttpRequest, mockHttpResponse);
		verify(mockHttpResponse).setStatus(503);
		verify(mockPrintWriter).println(String.format(REASON_JSON_FORMAT, "Synapse is down for maintenance."));
	}

	@Test
	public void testDown() throws Exception {
		// Set the status to be read only
		mockFullStackStatus(StatusEnum.DOWN, MSG_FORMAT);
		when(mockHttpResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);

		verify(mockFilterChain, never()).doFilter(mockHttpRequest, mockHttpResponse);
		verify(mockHttpResponse).setStatus(503);
		verify(mockPrintWriter).println(String.format(REASON_JSON_FORMAT_WITH_MESSAGE_FORMAT, StatusEnum.DOWN));
	}
	
	@Test
	public void testGetVersionReadOnly() throws Exception {
		// We should be able to get when the status is read-write
		when(mockHttpRequest.getRequestURI()).thenReturn("/repo/v1/version");
		
		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
	}

	@Test
	public void testAllBypassURIs() {
		assertFalse(StackStatusFilter.isBypassUri(GATED_URI));
		
		assertTrue(StackStatusFilter.isBypassUri("/repo/v1/admin/foo"));
		assertTrue(StackStatusFilter.isBypassUri("/repo/v1/migration/bar"));
		assertTrue(StackStatusFilter.isBypassUri("/repo/v1/version"));
		assertTrue(StackStatusFilter.isBypassUri("/repo/v1/status"));
	}
}
