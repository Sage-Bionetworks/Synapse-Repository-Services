package org.sagebionetworks.repo.manager.principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.principal.UserStatusManager.INACTIVITY_WARNING_OFFSET;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
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
	private UserProfileManager mockUserProfileManager;
	@Mock
	private OpenIDConnectManager mockOidcTokenManager;
	@Mock
	private NotificationManager mockNotificationManager;
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
	public void testWarnSoonToBeInactiveUsers() {
		Instant now = Instant.parse("2026-01-02T03:04:05Z");
		Date deactivationDate = Date.from(now.plus(INACTIVITY_WARNING_OFFSET, ChronoUnit.DAYS));
		UserInfo userInfo1 = new UserInfo(false, 123L);
		UserInfo userInfo2 = new UserInfo(false, 456L);
		UserProfile userProfile1 = new UserProfile();
		userProfile1.setUserName("userName123");
		userProfile1.setFirstName("firstName123");
		UserProfile userProfile2 = new UserProfile();
		userProfile2.setUserName("userName456");
		userProfile2.setFirstName("firstName456");

		when(mockClock.now()).thenReturn(Date.from(now));
		when(mockUserStatusDao.getSoonToBeInactiveUsersBatch(Date.from(now.minus(UserStatusManager.WARNING_INACTIVITY_DAYS, ChronoUnit.DAYS)), MAX_BATCH_SIZE)).thenReturn(List.of(123L, 456L));
		when(mockUserProfileManager.getUserProfile("123")).thenReturn(userProfile1);
		when(mockUserManager.getUserInfo(123L)).thenReturn(userInfo1);
		when(mockUserProfileManager.getUserProfile("456")).thenReturn(userProfile2);
		when(mockUserManager.getUserInfo(456L)).thenReturn(userInfo2);

		// call under test
		assertEquals(2, userStatusManager.warnSoonToBeInactiveUsers(MAX_BATCH_SIZE));

		// verify that calls to ses happened
		ArgumentCaptor<UserInfo> userInfoCaptor = ArgumentCaptor.forClass(UserInfo.class);
		ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockNotificationManager, times(2)).sendTemplatedNotification(userInfoCaptor.capture(), templateCaptor.capture(), subjectCaptor.capture(), contextCaptor.capture());
		List<UserInfo> userInfos = userInfoCaptor.getAllValues();
		List<String> templates = templateCaptor.getAllValues();
		List<String> subjects = subjectCaptor.getAllValues();
		List<Map<String, Object>> contexts = contextCaptor.getAllValues();
		assertTrue(templates.stream().allMatch("messages/UserAccountDeactivationTemplate.hml.vtl"::equals));
		assertTrue(subjects.stream().allMatch("ACTION REQUIRED: Your Synapse account will expire soon"::equals));
		Map<String, Object> context1 = contexts.get(0);
		Map<String, Object> context2 = contexts.get(1);
		assertEquals("userName123", context1.get("userName"));
		assertEquals("userName456", context2.get("userName"));
		assertEquals("firstName123", context1.get("firstName"));
		assertEquals("firstName456", context2.get("firstName"));
		String expectedDeactivationDate = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(deactivationDate.toInstant());
		assertTrue(contexts.stream().allMatch(ctx -> Objects.equals(expectedDeactivationDate, ctx.get("expirationDate"))));

		ArgumentCaptor<Date> warnedOnCaptor = ArgumentCaptor.forClass(Date.class);
		verify(mockUserStatusDao, times(2)).setWarnedOn(anyLong(), warnedOnCaptor.capture());
		assertTrue(warnedOnCaptor.getAllValues().stream().allMatch(Date.from(now)::equals));
		verify(mockUserStatusDao).setWarnedOn(123L, Date.from(now));
		verify(mockUserStatusDao).setWarnedOn(456L, Date.from(now));

	}

	@Test
	public void testWarnSoonToBeInactiveUsersWithBootstrapPrincipal() {
		Instant now = Instant.parse("2026-01-02T03:04:05Z");
		Date deactivationDate = Date.from(now.plus(INACTIVITY_WARNING_OFFSET, ChronoUnit.DAYS));
		UserInfo userInfo1 = new UserInfo(false, 123L);
		UserProfile userProfile1 = new UserProfile();
		userProfile1.setUserName("userName123");
		userProfile1.setFirstName("firstName123");

		when(mockClock.now()).thenReturn(Date.from(now));
		when(mockUserStatusDao.getSoonToBeInactiveUsersBatch(Date.from(now.minus(UserStatusManager.WARNING_INACTIVITY_DAYS, ChronoUnit.DAYS)), MAX_BATCH_SIZE)).thenReturn(List.of(123L, 1L));
		when(mockUserProfileManager.getUserProfile("123")).thenReturn(userProfile1);
		when(mockUserManager.getUserInfo(123L)).thenReturn(userInfo1);

		// call under test
		assertEquals(1, userStatusManager.warnSoonToBeInactiveUsers(MAX_BATCH_SIZE));

		// verify that calls to ses happened
		ArgumentCaptor<UserInfo> userInfoCaptor = ArgumentCaptor.forClass(UserInfo.class);
		ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockNotificationManager, times(1)).sendTemplatedNotification(userInfoCaptor.capture(), templateCaptor.capture(), subjectCaptor.capture(), contextCaptor.capture());
		List<UserInfo> userInfos = userInfoCaptor.getAllValues();
		List<String> templates = templateCaptor.getAllValues();
		List<String> subjects = subjectCaptor.getAllValues();
		List<Map<String, Object>> contexts = contextCaptor.getAllValues();
		assertTrue(templates.stream().allMatch("messages/UserAccountDeactivationTemplate.hml.vtl"::equals));
		assertTrue(subjects.stream().allMatch("ACTION REQUIRED: Your Synapse account will expire soon"::equals));
		Map<String, Object> context1 = contexts.get(0);
		assertEquals("userName123", context1.get("userName"));
		assertEquals("firstName123", context1.get("firstName"));
		String expectedDeactivationDate = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(deactivationDate.toInstant());
		assertTrue(contexts.stream().allMatch(ctx -> Objects.equals(expectedDeactivationDate, ctx.get("expirationDate"))));

		ArgumentCaptor<Date> warnedOnCaptor = ArgumentCaptor.forClass(Date.class);
		verify(mockUserStatusDao, times(1)).setWarnedOn(anyLong(), warnedOnCaptor.capture());
		assertTrue(warnedOnCaptor.getAllValues().stream().allMatch(Date.from(now)::equals));
		verify(mockUserStatusDao).setWarnedOn(123L, Date.from(now));

	}

	@Test
	public void testWarnSoonToBeInactiveUsersWithNoSoonToBeInactiveUsers() {
		Instant now = Instant.parse("2026-01-02T03:04:05Z");

		when(mockClock.now()).thenReturn(Date.from(now));
		when(mockUserStatusDao.getSoonToBeInactiveUsersBatch(Date.from(now.minus(UserStatusManager.WARNING_INACTIVITY_DAYS, ChronoUnit.DAYS)), MAX_BATCH_SIZE)).thenReturn(Collections.emptyList());

		// Call under test
		assertEquals(0, userStatusManager.warnSoonToBeInactiveUsers(MAX_BATCH_SIZE));

		verifyNoMoreInteractions(mockUserStatusDao, mockUserManager, mockOidcTokenManager);
	}
	
}
