package org.sagebionetworks.repo.manager.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;

@ExtendWith(MockitoExtension.class)
public class StatisticsManagerTest {
	@Mock
	private ObjectStatisticsRequest mockRequest;

	@Mock
	private ObjectStatisticsResponse mockResponse;

	@Mock
	private UserInfo mockUserInfo;

	@Mock
	private StatisticsProvider<ObjectStatisticsRequest, ObjectStatisticsResponse> mockProvider;

	@Mock
	private AuthorizationManager mockAuthManager;
	
	@Mock
	private AuthorizationStatus mockAuthStatus;
	
	@Mock
	private StatisticsManager manager;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void before() {
		when(mockProvider.getSupportedType()).thenReturn((Class<ObjectStatisticsRequest>) mockRequest.getClass());
		manager = new StatisticsManagerImpl(mockAuthManager, Collections.singletonList(mockProvider));
	}

	@Test
	public void testGetStatisticsWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.getStatistics(null, mockRequest);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.getStatistics(mockUserInfo, null);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			when(mockRequest.getObjectId()).thenReturn(null);
			// Call under test
			manager.getStatistics(mockUserInfo, mockRequest);
		});
	}

	@Test
	public void testGetProjectStatisticsAsAnonymous() {

		String projectId = "4356";
		Long userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();

		when(mockRequest.getObjectId()).thenReturn(projectId);
		when(mockUserInfo.getId()).thenReturn(userId);

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.getStatistics(mockUserInfo, mockRequest);
		});

		verifyZeroInteractions(mockProvider);
	}
	
	@Test
	public void testGetProjectStatisticsAsAdmin() {

		String projectId = "4356";
		Long userId = 123L;

		when(mockRequest.getObjectId()).thenReturn(projectId);
		when(mockUserInfo.getId()).thenReturn(userId);
		when(mockUserInfo.isAdmin()).thenReturn(true);
		when(mockProvider.getObjectStatistics(any())).thenReturn(mockResponse);
	
		// Call under test
		ObjectStatisticsResponse response = manager.getStatistics(mockUserInfo, mockRequest);

		assertEquals(mockResponse, response);
		verify(mockUserInfo).isAdmin();
		verifyZeroInteractions(mockAuthManager);
		verify(mockProvider).getObjectStatistics(mockRequest);
	}
	
	@Test
	public void testGetProjectStatisticsWithNoAccess() {
		Long userId = 123L;
		String projectId = "123";

		when(mockRequest.getObjectId()).thenReturn(projectId);
		when(mockUserInfo.getId()).thenReturn(userId);
		when(mockUserInfo.isAdmin()).thenReturn(false);
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(mockAuthStatus);
		doThrow(UnauthorizedException.class).when(mockAuthStatus).checkAuthorizationOrElseThrow();

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.getStatistics(mockUserInfo, mockRequest);
		});
		
		verify(mockAuthManager).canAccess(mockUserInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}

	@Test
	public void testGetStatistics() {
		String projectId = "123";

		when(mockRequest.getObjectId()).thenReturn(projectId);
		when(mockUserInfo.isAdmin()).thenReturn(false);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(mockAuthStatus);
		doNothing().when(mockAuthStatus).checkAuthorizationOrElseThrow();

		when(mockProvider.getObjectStatistics(any())).thenReturn(mockResponse);
		when(mockRequest.getObjectId()).thenReturn(projectId);

		// Call under test
		ObjectStatisticsResponse response =	manager.getStatistics(mockUserInfo, mockRequest);
		
		assertEquals(mockResponse, response);

		verify(mockUserInfo).isAdmin();
		verify(mockAuthManager).canAccess(mockUserInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthStatus).checkAuthorizationOrElseThrow();
		verify(mockProvider).getObjectStatistics(mockRequest);
	}

}
