package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.transactions.MandatoryWriteReadCommittedTransaction;
import org.sagebionetworks.repo.transactions.RequiresNewReadCommitted;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;

public class ExponentialBackoffUnsuccessfulAttemptLockoutImpl implements UnsuccessfulAttemptLockout{
	@Autowired
	UnsuccessfulAttemptLockoutDAO unsuccessfulAttemptLockoutDAO;

	//Caller of this should be using a NEW, SEPARATE transaction from their business logic code
	@RequiresNewReadCommitted
	@Override
	public AttemptResultReporter checkIsLockedOut(String key) {
		Long lockoutTime = unsuccessfulAttemptLockoutDAO.getUnexpiredLockoutTimestampMillis(key);
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
