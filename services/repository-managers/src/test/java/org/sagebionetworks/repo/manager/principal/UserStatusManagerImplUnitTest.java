package org.sagebionetworks.repo.manager.principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.PrincipalNameProvider;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.auth.UserStatusDao;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class UserStatusManagerImplUnitTest {

	@Mock
	private UserStatusDao mockUserStatusDao;

	@Mock
	private UserManager mockUserManager;

	@Mock
	private OpenIDConnectManager mockOidcTokenManager;

	@Mock
	private Clock mockClock;

	@Mock
	private TemplatedMessageSender mockTemplatedMessageSender;

	@Mock
	private PrincipalNameProvider mockPrincipalNameProvider;

	@Mock
	private ProdDetector mockProdDetector;

	@Spy
	@InjectMocks
	private UserStatusManagerImpl manager;

	@Test
	public void testWarnInactiveUsersWithNoUsersToWarn() {
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		Instant now = Instant.now();
		when(mockClock.now()).thenReturn(Date.from(now));
		Date expectedWarningThreshold = Date.from(now.minus(UserStatusManager.INACTIVITY_WARNING_DAYS, ChronoUnit.DAYS));
		Date expectedDisableThreshold = Date.from(now.minus(UserStatusManager.INACTIVITY_DAYS, ChronoUnit.DAYS));
		when(mockUserStatusDao.getInactiveUsersToWarnBatch(expectedWarningThreshold, expectedDisableThreshold, 500)).thenReturn(Collections.emptyList());

		// call under test
		int result = manager.warnInactiveUsers(500);

		assertEquals(0, result);
		verify(mockTemplatedMessageSender, never()).sendMessage(any());
		verify(mockUserStatusDao, never()).setDisableWarningSentOn(any());
	}

	@Test
	public void testWarnInactiveUsers() {
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		long userId1 = 111L;
		long userId2 = 222L;
		Instant now = Instant.now();
		when(mockClock.now()).thenReturn(Date.from(now));
		Date expectedWarningThreshold = Date.from(now.minus(UserStatusManager.INACTIVITY_WARNING_DAYS, ChronoUnit.DAYS));
		Date expectedDisableThreshold = Date.from(now.minus(UserStatusManager.INACTIVITY_DAYS, ChronoUnit.DAYS));
		Date expectedDisableDateForWarned = Date.from(now.plus(UserStatusManager.WARNING_PERIOD_LENGTH, ChronoUnit.DAYS));
		when(mockUserStatusDao.getInactiveUsersToWarnBatch(expectedWarningThreshold, expectedDisableThreshold, 500))
				.thenReturn(List.of(userId1, userId2));

		UserInfo sender = new UserInfo(false, BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId()))
				.thenReturn(sender);

		when(mockPrincipalNameProvider.getPrincipalName(userId1)).thenReturn("Alice");
		when(mockPrincipalNameProvider.getPrincipalName(userId2)).thenReturn("Bob");
		when(mockTemplatedMessageSender.sendMessage(any())).thenReturn(new MessageToUser().setId("msg-1"));

		// call under test
		int result = manager.warnInactiveUsers(500);

		assertEquals(2, result);
		verify(mockTemplatedMessageSender, times(2)).sendMessage(any());
		verify(mockUserStatusDao).setDisableWarningSentOn(List.of(userId1, userId2));
	}

	@Test
	public void testWarnInactiveUsersWithBootstrapPrincipal() {
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		long userId = 111L;
		long bootstrapId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		Instant now = Instant.now();
		when(mockClock.now()).thenReturn(Date.from(now));
		Date expectedWarningThreshold = Date.from(now.minus(UserStatusManager.INACTIVITY_WARNING_DAYS, ChronoUnit.DAYS));
		Date expectedDisableThreshold = Date.from(now.minus(UserStatusManager.INACTIVITY_DAYS, ChronoUnit.DAYS));
		when(mockUserStatusDao.getInactiveUsersToWarnBatch(expectedWarningThreshold, expectedDisableThreshold, 500))
				.thenReturn(List.of(userId, bootstrapId));

		UserInfo sender = new UserInfo(false, BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId()))
				.thenReturn(sender);

		when(mockPrincipalNameProvider.getPrincipalName(userId)).thenReturn("Alice");
		when(mockTemplatedMessageSender.sendMessage(any())).thenReturn(new MessageToUser().setId("msg-1"));

		// call under test
		int result = manager.warnInactiveUsers(500);

		assertEquals(1, result);
		verify(mockTemplatedMessageSender, times(1)).sendMessage(any());
		verify(mockUserStatusDao).setDisableWarningSentOn(List.of(userId));
	}

	@Test
	public void testWarnInactiveUsersWithEmailFailure() {
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		long userId1 = 111L;
		long userId2 = 222L;
		Instant now = Instant.now();
		when(mockClock.now()).thenReturn(Date.from(now));
		Date expectedWarningThreshold = Date.from(now.minus(UserStatusManager.INACTIVITY_WARNING_DAYS, ChronoUnit.DAYS));
		Date expectedDisableThreshold = Date.from(now.minus(UserStatusManager.INACTIVITY_DAYS, ChronoUnit.DAYS));
		Date expectedDisableDateForWarned = Date.from(now.plus(UserStatusManager.WARNING_PERIOD_LENGTH, ChronoUnit.DAYS));
		when(mockUserStatusDao.getInactiveUsersToWarnBatch(expectedWarningThreshold, expectedDisableThreshold, 500))
				.thenReturn(List.of(userId1, userId2));

		UserInfo sender = new UserInfo(false, BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId()))
				.thenReturn(sender);

		doThrow(new RuntimeException("email service unavailable")).when(manager).sendInactivityWarningEmail(sender, userId1, expectedDisableDateForWarned);
		when(mockPrincipalNameProvider.getPrincipalName(userId2)).thenReturn("Bob");
		when(mockTemplatedMessageSender.sendMessage(any())).thenReturn(new MessageToUser().setId("msg-1"));

		// call under test
		int result = manager.warnInactiveUsers(500);

		assertEquals(2, result);
		// userId1 email failed, userId2 email succeeded
		verify(mockTemplatedMessageSender, times(1)).sendMessage(any());
		// both users must be marked warned regardless of email failure
		verify(mockUserStatusDao).setDisableWarningSentOn(List.of(userId1, userId2));
	}

	@Test
	public void testWarnInactiveUsersWithStagingStack() {
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(false));

		// call under test
		int result = manager.warnInactiveUsers(500);

		assertEquals(0, result);
		// On staging we skip entirely: no users queried, no emails sent, nothing marked warned
		verifyNoInteractions(mockUserStatusDao, mockTemplatedMessageSender, mockUserManager,
				mockPrincipalNameProvider, mockClock);
		verify(mockProdDetector).isProductionStack();
	}

	@Test
	public void testWarnInactiveUsersWithUndetectableStack() {
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.empty());

		// call under test
		int result = manager.warnInactiveUsers(500);

		assertEquals(0, result);
		// When the stack cannot be detected we conservatively skip just like staging
		verifyNoInteractions(mockUserStatusDao, mockTemplatedMessageSender, mockUserManager,
				mockPrincipalNameProvider, mockClock);
		verify(mockProdDetector).isProductionStack();
	}

	@Test
	public void testSendInactivityWarningEmail() {
		long userId = 42L;
		UserInfo sender = new UserInfo(false, BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);
		// Build the disable date from a fixed local date so the formatted expiryDate is deterministic
		Date disableDate = Date.from(LocalDate.of(2026, 6, 19).atStartOfDay(ZoneId.systemDefault()).toInstant());
		when(mockPrincipalNameProvider.getPrincipalName(userId)).thenReturn("Alice");
		when(mockTemplatedMessageSender.sendMessage(any())).thenReturn(new MessageToUser().setId("msg-1"));

		ArgumentCaptor<MessageTemplate> templateCaptor = ArgumentCaptor.forClass(MessageTemplate.class);

		// call under test
		manager.sendInactivityWarningEmail(sender, userId, disableDate);

		verify(mockTemplatedMessageSender).sendMessage(templateCaptor.capture());
		MessageTemplate captured = templateCaptor.getValue();

		assertEquals("message/InactivityWarningNotification.html.vtl", captured.getTemplateFile());
		assertEquals(Collections.singleton("42"), captured.getRecipients());
		assertEquals(Map.of("displayName", "Alice", "expiryDate", "2026-06-19"), captured.getContext());
		assertEquals(true, captured.ignoreNotificationSettings());
		assertEquals("Action Required: Your Synapse Account Will Be Disabled in 14 Days",
				captured.getSubject().orElse(null));
	}
}
