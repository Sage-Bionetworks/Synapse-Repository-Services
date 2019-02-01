package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class ExponentialBackoffUnsuccessfulAttemptLockoutImpl implements UnsuccessfulAttemptLockout{
	@Autowired
	UnsuccessfulAttemptLockoutDAO unsuccessfulAttemptLockoutDAO;

	@Override
	public void reportSuccess(String key) {
		unsuccessfulAttemptLockoutDAO.removeLockout(key);
	}

	@Override
	public void reportFailure(String key) {
		long numFailed = unsuccessfulAttemptLockoutDAO.incrementNumFailedAttempts(key);

	}

	@Override
	public void checkIsLockedOut(String key) {

	}
}
