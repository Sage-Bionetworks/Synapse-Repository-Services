package org.sagebionetworks.repo.manager.principal;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.auth.UserStatusDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.stereotype.Service;

@Service
public class UserStatusManagerImpl implements UserStatusManager {
	
	private UserStatusDao userStatusDao;
	
	private OpenIDConnectManager oidcTokenManager;
	
	private UserManager userManager;
	
	private Clock clock;
	
	public UserStatusManagerImpl(UserStatusDao userStatusDao, UserManager userManager, OpenIDConnectManager oidcTokenManager, Clock clock) {
		this.userStatusDao = userStatusDao;
		this.userManager = userManager;
		this.oidcTokenManager = oidcTokenManager;
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
	@TemporaryCode(author = "marco", comment = "After the backfill job is complete, this method should be removed.")
	public int backfillUsersLastSeenOn(int maxCount) {
		
		int maxBatchSize = 500;
		int count = 0;
		
		List<Long> batch;
		
		do {
			batch = userStatusDao.getNeverSeenUsersBatch(maxBatchSize).stream()
				// Does not touch botstrapped users
				.filter(Predicate.not(BOOTSTRAP_PRINCIPAL::isBootstrapPrincipalId))
				.collect(Collectors.toList());
			
			if (batch.isEmpty()) {
				return count;
			}
			
			// Get a random date between now and a week ago to spread disabling users over time
			long randomMins = (long) (Math.random() * 7 * 24 * 60);
			
			Date lastSeenOn = Date.from(clock.now().toInstant().minus(randomMins, ChronoUnit.MINUTES));
			
			userStatusDao.setLastSeenOn(batch, lastSeenOn);
			
			count+= batch.size();
			
		} while (count < maxCount);
		
		return count;
		
	}
	
}
