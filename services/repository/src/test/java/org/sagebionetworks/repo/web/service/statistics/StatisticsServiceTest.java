package org.sagebionetworks.repo.web.service.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.statistics.StatisticsManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;

@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {
	
	@Mock
	private ObjectStatisticsRequest mockRequest;
	
	@Mock
	private ObjectStatisticsResponse mockResponse;
	
	@Mock
	private UserInfo mockUserInfo;
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private StatisticsManager mockStatisticsManager;
	
	@InjectMocks
	private StatisticsServiceImpl service;
	
	@Test
	public void testGetStatisticsWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			Long userId = null;
			// Call under test
			service.getStatistics(userId, mockRequest);
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			Long userId = 123L;
			// Call under test
			service.getStatistics(userId, null);
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			when(mockRequest.getObjectId()).thenReturn(null);
			Long userId = 123L;
			// Call under test
			service.getStatistics(userId, mockRequest);
		});
	}
	
	@Test
	public void testGetStatistics() {
		
		String projectId = "4356";
		Long userId = 123L;
		
		when(mockUserManager.getUserInfo(any())).thenReturn(mockUserInfo);
		when(mockStatisticsManager.getStatistics(any(), any())).thenReturn(mockResponse);
		when(mockRequest.getObjectId()).thenReturn(projectId);
		
		// Call under test
		ObjectStatisticsResponse response = service.getStatistics(userId, mockRequest);
		
		assertEquals(mockResponse, response);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mockStatisticsManager).getStatistics(mockUserInfo, mockRequest);
	}

}
