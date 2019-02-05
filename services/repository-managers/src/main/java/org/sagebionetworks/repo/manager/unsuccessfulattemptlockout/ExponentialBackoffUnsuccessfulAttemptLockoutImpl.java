package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;

public class ExponentialBackoffUnsuccessfulAttemptLockoutImpl implements UnsuccessfulAttemptLockout{
	@Autowired
	UnsuccessfulAttemptLockoutDAO unsuccessfulAttemptLockoutDAO;


	@WriteTransactionReadCommitted
	@Override
	public AttemptResultReporter checkIsLockedOut(String key) {
		Long lockoutTime = unsuccessfulAttemptLockoutDAO.getLockoutExpirationTimestamp(key);
		if (lockoutTime != null){
			//TODO: handle in base controller
			throw new UnsuccessfulAttemptLockoutException(
					"You locked out from making any additional attempts until " + lockoutTime,
					lockoutTime,
					unsuccessfulAttemptLockoutDAO.getNumFailedAttempts(key));
		}
		return new ExponentialBackoffAttemptReporter(key, unsuccessfulAttemptLockoutDAO);
	}
}
