package org.sagebionetworks.repo.manager.principal;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.PrincipalNameProvider;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.auth.UserStatusDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.springframework.stereotype.Service;

@Service
public class UserStatusManagerImpl implements UserStatusManager {

	private static final Logger log = LogManager.getLogger(UserStatusManagerImpl.class);

	private UserStatusDao userStatusDao;

	private OpenIDConnectManager oidcTokenManager;

	private UserManager userManager;

	private Clock clock;

	private TemplatedMessageSender templatedMessageSender;

	private PrincipalNameProvider principalNameProvider;

	private ProdDetector prodDetector;

	public UserStatusManagerImpl(UserStatusDao userStatusDao, UserManager userManager,
			OpenIDConnectManager oidcTokenManager, Clock clock,
			TemplatedMessageSender templatedMessageSender, PrincipalNameProvider principalNameProvider,
			ProdDetector prodDetector) {
		this.userStatusDao = userStatusDao;
		this.userManager = userManager;
		this.oidcTokenManager = oidcTokenManager;
		this.clock = clock;
		this.templatedMessageSender = templatedMessageSender;
		this.principalNameProvider = principalNameProvider;
		this.prodDetector = prodDetector;
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
	public int warnInactiveUsers(int maxBatchSize) {
		// The inactive user warning email is only sent from the production stack to avoid sending
		// duplicate emails from both the production and staging stacks. If the stack cannot be
		// detected we conservatively skip sending.
		if (!prodDetector.isProductionStack().orElse(false)) {
			return 0;
		}

		Date warningThreshold = Date.from(clock.now().toInstant().minus(INACTIVITY_WARNING_DAYS, ChronoUnit.DAYS));
		Date disableThreshold = Date.from(clock.now().toInstant().minus(INACTIVITY_DAYS, ChronoUnit.DAYS));
		// This is the actual day of deactivation to put in the warning message
		Date disableDateForWarned = Date.from(clock.now().toInstant().plus(WARNING_PERIOD_LENGTH, ChronoUnit.DAYS));

		List<Long> usersToWarn = userStatusDao.getInactiveUsersToWarnBatch(warningThreshold, disableThreshold, maxBatchSize).stream()
				.filter(Predicate.not(BOOTSTRAP_PRINCIPAL::isBootstrapPrincipalId))
				.collect(Collectors.toList());

		if (usersToWarn.isEmpty()) {
			return 0;
		}

		UserInfo sender = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());

		for (Long userId : usersToWarn) {
			try {
				sendInactivityWarningEmail(sender, userId, disableDateForWarned);
			} catch (Exception e) {
				log.error("Failed to send inactivity warning email to user {}, marking as warned anyway", userId, e);
			}
		}

		userStatusDao.setDisableWarningSentOn(usersToWarn);

		return usersToWarn.size();
	}

	// package-private for unit testing
	void sendInactivityWarningEmail(UserInfo sender, Long userId, Date disableDate) {
		String disableDateStr = disableDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Map<String, Object> context = Map.of(
				"displayName", principalNameProvider.getPrincipalName(userId),
				"expiryDate", disableDateStr
		);
		templatedMessageSender.sendMessage(
				MessageTemplate.builder()
						.withSender(sender)
						.withRecipients(Collections.singleton(userId.toString()))
						.withTemplateFile("message/InactivityWarningNotification.html.vtl")
						.withSubject("Action Required: Your Synapse Account Will Be Disabled in 14 Days")
						.withIgnoreNotificationSettings(true)
						.withContext(context)
						.build());
	}

	@Override
	@WriteTransaction
	public void resetUserStatusToEnabled(Long targetUserId) {
		userStatusDao.enableUser(targetUserId);
	}

}
