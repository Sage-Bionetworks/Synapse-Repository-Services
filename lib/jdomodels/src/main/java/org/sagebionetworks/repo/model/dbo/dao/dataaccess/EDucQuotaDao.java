package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

public interface EDucQuotaDao {

	DBOEDucQuota create(Long userId, Long accessRequirementId, String envelopeId);

	long getCount(Long userId, Long accessRequirementId, long fromEpochMs, long toEpochMs);

	long getGlobalCount(long fromEpochMs, long toEpochMs);

	void delete(Long id);

	/**
	 * Deletes all quota records for the given user and access requirement, resetting their usage.
	 *
	 * @param userId              the user whose quota records should be removed
	 * @param accessRequirementId the access requirement the quota records are scoped to
	 * @return the number of records deleted
	 */
	int deleteByUserAndAccessRequirement(Long userId, Long accessRequirementId);

	void truncateAll();
}
