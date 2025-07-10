package org.sagebionetworks.repo.manager.principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.dbo.auth.UserStatusDao;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class UserStatusManagerImpUnitTest {

	private static final int MAX_BATCH_SIZE = 2;

	@Mock
	private UserStatusDao mockUserStatusDao;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private OpenIDConnectManager mockOidcTokenManager;
	@Mock
	private Clock mockClock;

	@InjectMocks
	private UserStatusManagerImpl userStatusManager;
	
	@Captor
	private ArgumentCaptor<Date> dateCaptor;

	@Test
	public void testDisableInactiveUsers() {
		Instant now = Instant.now();

		when(mockClock.now()).thenReturn(Date.from(now));

		when(mockUserStatusDao.getInactiveUsersBatch(Date.from(now.minus(UserStatusManager.INACTIVITY_DAYS, ChronoUnit.DAYS)), MAX_BATCH_SIZE)).thenReturn(List.of(123L, 456L));

		// Call under test
		assertEquals(2, userStatusManager.disableInactiveUsers(MAX_BATCH_SIZE));

		verify(mockOidcTokenManager).revokeUserAccess(123L);
		verify(mockOidcTokenManager).revokeUserAccess(456L);
		verify(mockUserManager).deleteOidcBinding(123L);
		verify(mockUserManager).deleteOidcBinding(456L);
		verify(mockUserStatusDao).setDisabled(123L, true);
		verify(mockUserStatusDao).setDisabled(456L, true);

		verifyNoMoreInteractions(mockUserStatusDao, mockUserManager, mockOidcTokenManager);
	}

	@Test
	public void testDisableInactiveUsersWithBootstrapPrincipal() {
		Instant now = Instant.now();

		when(mockClock.now()).thenReturn(Date.from(now));

		when(mockUserStatusDao.getInactiveUsersBatch(Date.from(now.minus(UserStatusManager.INACTIVITY_DAYS, ChronoUnit.DAYS)), MAX_BATCH_SIZE)).thenReturn(List.of(123L, 1L));

		// Call under test
		assertEquals(1, userStatusManager.disableInactiveUsers(MAX_BATCH_SIZE));

		verify(mockOidcTokenManager).revokeUserAccess(123L);
		verify(mockUserManager).deleteOidcBinding(123L);
		verify(mockUserStatusDao).setDisabled(123L, true);

		verifyNoMoreInteractions(mockUserStatusDao, mockUserManager, mockOidcTokenManager);
	}

	@Test
	public void testDisableInactiveUsersWithNoInactiveUsers() {
		Instant now = Instant.now();

		when(mockClock.now()).thenReturn(Date.from(now));

		when(mockUserStatusDao.getInactiveUsersBatch(Date.from(now.minus(UserStatusManager.INACTIVITY_DAYS, ChronoUnit.DAYS)), MAX_BATCH_SIZE)).thenReturn(Collections.emptyList());

		// Call under test
		assertEquals(0, userStatusManager.disableInactiveUsers(MAX_BATCH_SIZE));

		verifyNoMoreInteractions(mockUserStatusDao, mockUserManager, mockOidcTokenManager);
	}
	
	@Test
	public void testBackfillUsersLastSeenOn() {
		int maxCount = 1000;
		
		Instant now = Instant.now();
		
		List<Long> userList = List.of(123L, 456L);				
		
		when(mockClock.now()).thenReturn(Date.from(now));
		when(mockUserStatusDao.getNeverSeenUsersBatch(anyInt())).thenReturn(userList, Collections.emptyList());
		
		// Call under test
		assertEquals(2, userStatusManager.backfillUsersLastSeenOn(maxCount));
		
		verify(mockUserStatusDao).setLastSeenOn(eq(userList), dateCaptor.capture());

		assertTrue(dateCaptor.getValue().toInstant().isBefore(now));
		assertTrue(dateCaptor.getValue().toInstant().isAfter(now.minus(7, ChronoUnit.DAYS)));
	}
	
	@Test
	public void testBackfillUsersLastSeenOnWithBoostrappedUsers() {
		int maxCount = 1000;
		
		Instant now = Instant.now();
		
		List<Long> userList = List.of(123L, 456L, 1L); // 1L is a bootstrap principal				
		
		when(mockClock.now()).thenReturn(Date.from(now));
		when(mockUserStatusDao.getNeverSeenUsersBatch(anyInt())).thenReturn(userList, Collections.emptyList());
		
		// Call under test
		assertEquals(2, userStatusManager.backfillUsersLastSeenOn(maxCount));
		
		verify(mockUserStatusDao).setLastSeenOn(eq(List.of(123L, 456L)), dateCaptor.capture());

		assertTrue(dateCaptor.getValue().toInstant().isBefore(now));
		assertTrue(dateCaptor.getValue().toInstant().isAfter(now.minus(7, ChronoUnit.DAYS)));
	}
	
	@Test
	public void testBackfillUsersLastSeenOnWithMultipleBatches() {
		int maxCount = 1000;
		
		Instant now = Instant.now();
				
		when(mockClock.now()).thenReturn(Date.from(now));
		when(mockUserStatusDao.getNeverSeenUsersBatch(anyInt())).thenReturn(List.of(123L, 456L), List.of(1L, 789L), Collections.emptyList());
		
		// Call under test
		assertEquals(3, userStatusManager.backfillUsersLastSeenOn(maxCount));
		
		verify(mockUserStatusDao).setLastSeenOn(eq(List.of(123L, 456L)), dateCaptor.capture());
		verify(mockUserStatusDao).setLastSeenOn(eq(List.of(789L)), dateCaptor.capture());

		for (Date date : dateCaptor.getAllValues()) {
			assertTrue(date.toInstant().isBefore(now));
			assertTrue(date.toInstant().isAfter(now.minus(7, ChronoUnit.DAYS)));
		}
	}
}
