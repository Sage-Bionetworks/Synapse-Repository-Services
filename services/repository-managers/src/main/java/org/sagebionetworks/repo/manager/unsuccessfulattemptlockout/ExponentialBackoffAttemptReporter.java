package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;

public class ExponentialBackoffAttemptReporter implements AttemptResultReporter{
	private UnsuccessfulAttemptLockoutDAO dao;
	private String attemptKey;

	ExponentialBackoffAttemptReporter(String attemptKey, UnsuccessfulAttemptLockoutDAO dao){
		this.attemptKey = attemptKey;
		this.dao = dao;
	}

	@WriteTransactionReadCommitted
	@Override
	public void reportSuccess() {
		dao.removeLockout(attemptKey);
	}

	@WriteTransactionReadCommitted
	@Override
	public void reportFailure() {
		long numFailed = dao.incrementNumFailedAttempts(attemptKey);
		dao.setExpiration(attemptKey, 1 << numFailed);
	}
}
