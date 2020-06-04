package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricUtils;
import org.sagebionetworks.cloudwatch.ProfileData;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class FilterHelperTest {
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private PrintWriter mockPrintWriter;

	@Mock
	private HttpServletResponse mockResponse;
	
	@Mock
	private Consumer mockConsumer;
	
	@InjectMocks
	private FilterHelper filterHelper;
	
	@Captor 
	private ArgumentCaptor<List<ProfileData>> captorProfileData;
	
	private static final String STACK_INSTANCE = "instance";
		
	@Test
	public void testRejectRequestWithNoReporting() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		filterHelper.rejectRequest(false, mockResponse, "Some message");
		
		verifyRejectRequest("{\"reason\":\"Some message\"}");
		
		verifyZeroInteractions(mockConsumer);
	}
	
	@Test
	public void testRejectRequestWithReporting() throws Exception {
		when(mockConfig.getStackInstance()).thenReturn(STACK_INSTANCE);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		String message = "Some message";
		
		filterHelper.rejectRequest(true, mockResponse, message);
		
		verifyRejectRequest("{\"reason\":\"Some message\"}");
		
		verify(mockConsumer).addProfileData(captorProfileData.capture());
		
		List<ProfileData> data = captorProfileData.getValue();
		
		assertEquals(2, data.size());
		
		List<ProfileData> expected = ImmutableList.of(
				generateProfileData(data.get(0).getTimestamp(), null),
				generateProfileData(data.get(1).getTimestamp(), message)
		);
		
		assertEquals(expected, data);
	}
	
	@Test 
	public void testRejectRequestWithExceptionReporting() throws Exception {
		when(mockConfig.getStackInstance()).thenReturn(STACK_INSTANCE);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		IllegalArgumentException ex = new IllegalArgumentException("Some message");
		
		String message = MetricUtils.stackTracetoString(ex);
		
		filterHelper.rejectRequest(true, mockResponse, ex);
		
		verifyRejectRequest("{\"reason\":\"Some message\"}");
		
		verify(mockConsumer).addProfileData(captorProfileData.capture());
		
		List<ProfileData> data = captorProfileData.getValue();
		
		assertEquals(2, data.size());
		
		List<ProfileData> expected = ImmutableList.of(
				generateProfileData(data.get(0).getTimestamp(), null),
				generateProfileData(data.get(1).getTimestamp(), message)
		);
		
		assertEquals(expected, data);
	}
	
	private ProfileData generateProfileData(Date timestamp, String message) {
		ProfileData data = new ProfileData();
		
		data.setNamespace("Authentication - " + STACK_INSTANCE);
		data.setName("BadCredentials");
		data.setUnit(StandardUnit.Count.toString());
		data.setValue(1.0);
		data.setTimestamp(timestamp);
		
		Map<String, String> dimensions = new HashMap<>();
		
		dimensions.put("filterClass", FilterHelper.class.getName());

		if (message != null) {
			dimensions.put("message", message);
		}
		
		data.setDimension(dimensions);
		
		return data;
		
	}
	
	private void verifyRejectRequest(String message) throws IOException, ServletException {
		verify(mockResponse).setStatus(HttpStatus.SC_UNAUTHORIZED);
		verify(mockPrintWriter).println(message);
	}
}
