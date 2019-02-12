package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.transactions.MandatoryWriteReadCommittedTransaction;

public interface UnsuccessfulLoginLockoutDAO {
	/**
	 *
	 * @param userId principal ID of the user
	 * @return UnsuccessfulLoginLockoutDTO of the given userId. null if it does not exist
	 */
	UnsuccessfulLoginLockoutDTO getUnsuccessfulLoginLockoutInfoIfExist(long userId);

	/**
	 *
	 * @return unix timestamp in milliseconds of the database time
	 */
	long getDatabaseTimestampMillis();

	/**
	 * Creates the DTO if it does not already exist. Otherwise updates an existing one with the new information
	 * @param dto dto to be created or updated
	 */
	void createOrUpdateUnsuccessfulLoginLockoutInfo(UnsuccessfulLoginLockoutDTO dto);

	/**
	 * Delete a the UnsuccessfulLoginLockoutDTO associated with the given userId
	 * @param userId principal ID of the user
	 */
	void deleteUnsuccessfulLoginLockoutInfo(long userId);
}
