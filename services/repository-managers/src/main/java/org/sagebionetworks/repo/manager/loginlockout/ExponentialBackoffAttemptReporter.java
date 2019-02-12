package org.sagebionetworks.repo.manager.loginlockout;

import java.util.Objects;

import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;
import org.sagebionetworks.repo.transactions.MandatoryWriteReadCommittedTransaction;
import org.sagebionetworks.util.ValidateArgument;

public class ExponentialBackoffAttemptReporter implements AttemptResultReporter{
	private final UnsuccessfulLoginLockoutDAO dao;
	private UnsuccessfulLoginLockoutDTO lockoutInfo;

	ExponentialBackoffAttemptReporter(UnsuccessfulLoginLockoutDTO lockoutInfo, final UnsuccessfulLoginLockoutDAO dao){
		ValidateArgument.required(lockoutInfo, "lockoutInfo");
		ValidateArgument.required(dao, "dao");

		this.dao = dao;
		this.lockoutInfo = lockoutInfo;
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public void reportSuccess() {
		dao.deleteUnsuccessfulLoginLockoutInfo(lockoutInfo.getUserId());
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public void reportFailure() {
		final long incrementedUnsuccessfulLoginCount = lockoutInfo.getUnsuccessfulLoginCount() + 1;
		final long lockDurationMilliseconds = 1 << incrementedUnsuccessfulLoginCount;

		dao.createOrUpdateUnsuccessfulLoginLockoutInfo(
				lockoutInfo.withUnsuccessfulLoginCount(incrementedUnsuccessfulLoginCount)
				.withLockoutExpiration(dao.getDatabaseTimestampMillis() + lockDurationMilliseconds));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExponentialBackoffAttemptReporter that = (ExponentialBackoffAttemptReporter) o;
		return dao.equals(that.dao) &&
				lockoutInfo.equals(that.lockoutInfo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dao, lockoutInfo);
	}
}
