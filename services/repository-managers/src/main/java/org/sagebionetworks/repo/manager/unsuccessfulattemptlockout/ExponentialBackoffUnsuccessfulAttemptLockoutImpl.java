package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

import org.sagebionetworks.repo.model.dbo.dao.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;

public class ExponentialBackoffUnsuccessfulAttemptLockoutImpl implements UnsuccessfulAttemptLockout{
	@Autowired
	UnsuccessfulAttemptLockoutDAO unsuccessfulAttemptLockoutDAO;

	@WriteTransactionReadCommitted
	@Override
	public void reportSuccess(String key) {
		unsuccessfulAttemptLockoutDAO.removeLockout(key);
	}

	@WriteTransactionReadCommitted
	@Override
	public void reportFailure(String key) {
		long numFailed = unsuccessfulAttemptLockoutDAO.incrementNumFailedAttempts(key);
		unsuccessfulAttemptLockoutDAO.setExpiration(key, 1 << numFailed);

	}

	//TODO: return lockout object that reports lockout or not
	@WriteTransactionReadCommitted
	@Override
	public void checkIsLockedOut(String key) {
		Long lockoutTime = unsuccessfulAttemptLockoutDAO.isLockedOut(key);
		if (lockoutTime != null){
			throw new UnsuccessfulAttemptLockoutException("You locked out from making any additional attempts until " + lockoutTime, lockoutTime);
		}
	}
}
