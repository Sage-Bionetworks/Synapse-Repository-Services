package org.sagebionetworks.repo.manager.loginlockout;

import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;

public class ExponentialBackoffLoginLockoutStatusImpl implements LoginLockoutStatus {
	@Autowired
	UnsuccessfulLoginLockoutDAO unsuccessfulLoginLockoutDAO;

	//Caller of this should be using a NEW, SEPARATE transaction from their business logic code
	@NewWriteTransaction
	@Override
	public LoginAttemptResultReporter checkIsLockedOut(long userId) {
		// Use database's unix timestamp for expiration check
		// instead of this machine's timestamp to avoid time sync issues across all machines
		UnsuccessfulLoginLockoutDTO lockoutInfo = unsuccessfulLoginLockoutDAO.getUnsuccessfulLoginLockoutInfoIfExist(userId);

		if (lockoutInfo == null){
			lockoutInfo = new UnsuccessfulLoginLockoutDTO(userId);
		}

		final long databaseTime = unsuccessfulLoginLockoutDAO.getDatabaseTimestampMillis();
		if (databaseTime < lockoutInfo.getLockoutExpiration()){
			throw new UnsuccessfulLoginLockoutException(
					databaseTime,
					lockoutInfo.getLockoutExpiration(),
					lockoutInfo.getUnsuccessfulLoginCount());
		}
		return new ExponentialBackoffLoginAttemptReporter(lockoutInfo, unsuccessfulLoginLockoutDAO);
	}

	//Caller should be using this as an additional operation on an existing transaction
	@MandatoryWriteTransaction
	@Override
	public void forceResetLockoutCount(final long userId) {
		unsuccessfulLoginLockoutDAO.createOrUpdateUnsuccessfulLoginLockoutInfo(new UnsuccessfulLoginLockoutDTO(userId)
				.withLockoutExpiration(0)
				.withUnsuccessfulLoginCount(0));
	}
}
