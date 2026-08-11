package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

public interface EDucQuotaDao {

	DBOEDucQuota create(Long userId, Long accessRequirementId, String envelopeId);

	long getCount(Long userId, Long accessRequirementId, long fromEpochMs, long toEpochMs);

	long getGlobalCount(long fromEpochMs, long toEpochMs);

	void delete(Long id);

	void truncateAll();
}
