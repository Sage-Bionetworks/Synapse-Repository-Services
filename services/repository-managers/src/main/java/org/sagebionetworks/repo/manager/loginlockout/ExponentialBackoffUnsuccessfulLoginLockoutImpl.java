package org.sagebionetworks.repo.manager.loginlockout;

import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.transactions.RequiresNewReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;

public class ExponentialBackoffUnsuccessfulLoginLockoutImpl implements UnsuccessfulLoginLockout {
	@Autowired
	UnsuccessfulLoginLockoutDAO unsuccessfulLoginLockoutDAO;

	//Caller of this should be using a NEW, SEPARATE transaction from their business logic code
	@RequiresNewReadCommitted
	@Override
	public AttemptResultReporter checkIsLockedOut(String key) {
		Long lockoutTime = unsuccessfulLoginLockoutDAO.getUnexpiredLockoutTimestampMillis(key);
		if (lockoutTime != null){
			throw new UnsuccessfulLoginLockoutException(
					"You locked out from making any additional attempts until " + lockoutTime,
					lockoutTime,
					unsuccessfulLoginLockoutDAO.getNumFailedAttempts(key));
		}
		return new ExponentialBackoffAttemptReporter(key, unsuccessfulLoginLockoutDAO);
	}
}
