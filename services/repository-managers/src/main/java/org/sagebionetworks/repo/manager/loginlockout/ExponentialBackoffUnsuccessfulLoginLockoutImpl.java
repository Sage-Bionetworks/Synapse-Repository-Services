package org.sagebionetworks.repo.manager.loginlockout;

import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;
import org.sagebionetworks.repo.transactions.RequiresNewReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;

public class ExponentialBackoffUnsuccessfulLoginLockoutImpl implements UnsuccessfulLoginLockout {
	@Autowired
	UnsuccessfulLoginLockoutDAO unsuccessfulLoginLockoutDAO;

	//Caller of this should be using a NEW, SEPARATE transaction from their business logic code
	@RequiresNewReadCommitted
	@Override
	public LoginAttemptResultReporter checkIsLockedOut(long key) {
		// Use database's unix timestamp for expiration check
		// instead of this machine's timestamp to avoid time sync issues across all machines
		UnsuccessfulLoginLockoutDTO lockoutInfo = unsuccessfulLoginLockoutDAO.getUnsuccessfulLoginLockoutInfoIfExist(key);

		if (lockoutInfo == null){
			lockoutInfo = new UnsuccessfulLoginLockoutDTO(key);
		}

		final long databaseTime = unsuccessfulLoginLockoutDAO.getDatabaseTimestampMillis();
		if (databaseTime < lockoutInfo.getLockoutExpiration()){
			throw new UnsuccessfulLoginLockoutException(
					lockoutInfo.getLockoutExpiration(),
					lockoutInfo.getUnsuccessfulLoginCount());
		}
		return new ExponentialBackoffLoginAttemptReporter(lockoutInfo, unsuccessfulLoginLockoutDAO);
	}
}
