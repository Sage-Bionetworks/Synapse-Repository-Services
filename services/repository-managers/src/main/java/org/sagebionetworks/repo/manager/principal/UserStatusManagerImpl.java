package org.sagebionetworks.repo.manager.principal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.auth.UserStatusDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.springframework.stereotype.Service;

@Service
public class UserStatusManagerImpl implements UserStatusManager {

	private static final String NOTIFICATION_USER_ACCOUNT_DEACTIVATION_TPL = "messages/UserAccountDeactivationTemplate.hml.vtl";
	
	private UserStatusDao userStatusDao;
	private OpenIDConnectManager oidcTokenManager;
	private UserManager userManager;
	private UserProfileManager userProfileManager;
	private NotificationManager notificationManager;
	private Clock clock;
	
	public UserStatusManagerImpl(UserStatusDao userStatusDao, UserManager userManager, OpenIDConnectManager oidcTokenManager, NotificationManager notificationManager, UserProfileManager userProfileManager, Clock clock) {
		this.userStatusDao = userStatusDao;
		this.userManager = userManager;
		this.userProfileManager = userProfileManager;
		this.oidcTokenManager = oidcTokenManager;
		this.notificationManager = notificationManager;
		this.clock = clock;
	}


	@Override
	@WriteTransaction
	public int disableInactiveUsers(int maxBatchSize) {
		Date inactivityThreshold = Date.from(clock.now().toInstant().minus(INACTIVITY_DAYS, ChronoUnit.DAYS));
		
		List<Long> inactiveUsers = userStatusDao.getInactiveUsersBatch(inactivityThreshold, maxBatchSize).stream()
			// Does not touch botstrapped users
			.filter(Predicate.not(BOOTSTRAP_PRINCIPAL::isBootstrapPrincipalId))
			.collect(Collectors.toList());		
		
		if (inactiveUsers.isEmpty()) {
			return 0;
		}
		
		for (Long userId : inactiveUsers) {
			// Revoke tokens, PATs and authorization consent
			oidcTokenManager.revokeUserAccess(userId);
			// Delete external OIDC bindings
			userManager.deleteOidcBinding(userId);
			// Now disable the user
			userStatusDao.setDisabled(userId, true);
		}
		
		return inactiveUsers.size();
	}

	@Override
	public int warnSoonToBeInactiveUsers(int maxBatchSize) {
		Instant now = clock.now().toInstant();
		Date warnInactivityThreshold = Date.from(now.minus(WARNING_INACTIVITY_DAYS, ChronoUnit.DAYS));
		Date deactivationDate = Date.from(now.plus(INACTIVITY_WARNING_OFFSET, ChronoUnit.DAYS));

		List<Long> inactiveUsers = userStatusDao.getSoonToBeInactiveUsersBatch(warnInactivityThreshold, maxBatchSize).stream()
				// Does not touch bootstrapped users
				.filter(Predicate.not(BOOTSTRAP_PRINCIPAL::isBootstrapPrincipalId))
				.collect(Collectors.toList());

		if (inactiveUsers.isEmpty()) {
			return 0;
		}

		List<Long> emailedSoonToBeInactiveUserIds = emailSoonToBeInactiveUsers(inactiveUsers, deactivationDate);

		int numUsersRecordedAsWarned = setAsWarned(emailedSoonToBeInactiveUserIds);

		return numUsersRecordedAsWarned;
	}

	private List<Long> emailSoonToBeInactiveUsers(List<Long> soonToBeInactiveUserIds, Date deactivationDate) {

		List<Long> emailedUsers = new LinkedList<>();

		for (Long userId: soonToBeInactiveUserIds) {
			UserInfo userInfo = userManager.getUserInfo(userId);
			UserProfile userProfile = userProfileManager.getUserProfile(userId.toString());
			Map<String, Object> notificationContext = new HashMap<>();
			notificationContext.put("firstName", userProfile.getFirstName());
			notificationContext.put("userName", userProfile.getUserName());
			notificationContext.put("expirationDate", DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(deactivationDate.toInstant()));

			notificationManager.sendTemplatedNotification(userInfo, NOTIFICATION_USER_ACCOUNT_DEACTIVATION_TPL, "ACTION REQUIRED: Your Synapse account will expire soon", notificationContext);

			emailedUsers.add(userId);
		}

		return emailedUsers;
	}

	private int setAsWarned(List<Long> warnedSoonToBeInactiveUserIds) {
		Date now = Date.from(clock.now().toInstant());
		for (Long userId: warnedSoonToBeInactiveUserIds) {
			userStatusDao.setWarnedOn(userId, now);
		}
		return warnedSoonToBeInactiveUserIds.size();
	}

	@Override
	@WriteTransaction
	public void resetUserStatusToEnabled(Long targetUserId) {
		userStatusDao.resetStatusToEnabled(targetUserId);
	}
	
}
