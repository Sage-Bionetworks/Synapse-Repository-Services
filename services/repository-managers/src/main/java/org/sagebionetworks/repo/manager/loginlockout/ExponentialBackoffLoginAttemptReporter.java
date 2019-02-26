package org.sagebionetworks.repo.manager.loginlockout;

import java.util.Objects;

import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.util.ValidateArgument;

public class ExponentialBackoffLoginAttemptReporter implements LoginAttemptResultReporter {
	private final UnsuccessfulLoginLockoutDAO dao;
	private UnsuccessfulLoginLockoutDTO lockoutInfo;

	private boolean reported;

	ExponentialBackoffLoginAttemptReporter(UnsuccessfulLoginLockoutDTO lockoutInfo, final UnsuccessfulLoginLockoutDAO dao){
		ValidateArgument.required(lockoutInfo, "lockoutInfo");
		ValidateArgument.required(dao, "dao");

		this.dao = dao;

		this.lockoutInfo = new UnsuccessfulLoginLockoutDTO(lockoutInfo.getUserId())
				.withUnsuccessfulLoginCount(lockoutInfo.getUnsuccessfulLoginCount())
				.withLockoutExpiration(lockoutInfo.getLockoutExpiration());

		this.reported = false;
	}

	@MandatoryWriteTransaction
	@Override
	public void reportSuccess() {
		if(reported){
			return;
		}
		this.reported = true;

		dao.createOrUpdateUnsuccessfulLoginLockoutInfo(lockoutInfo
				.withLockoutExpiration(0)
				.withUnsuccessfulLoginCount(0));
	}

	@MandatoryWriteTransaction
	@Override
	public void reportFailure() {
		if(reported){
			return;
		}
		this.reported = true;

		final long incrementedUnsuccessfulLoginCount = lockoutInfo.getUnsuccessfulLoginCount() + 1;
		final long lockDurationMilliseconds = 1 << incrementedUnsuccessfulLoginCount;

		dao.createOrUpdateUnsuccessfulLoginLockoutInfo(
				lockoutInfo
						.withUnsuccessfulLoginCount(incrementedUnsuccessfulLoginCount)
						.withLockoutExpiration(dao.getDatabaseTimestampMillis() + lockDurationMilliseconds));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExponentialBackoffLoginAttemptReporter that = (ExponentialBackoffLoginAttemptReporter) o;
		return reported == that.reported &&
				dao.equals(that.dao) &&
				lockoutInfo.equals(that.lockoutInfo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dao, lockoutInfo, reported);
	}
}
