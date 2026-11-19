package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Optional;

/**
 * Persists the secret salt of each aggregate data source. The salt keys the noise of every count released
 * about the source, so once stored it never changes.
 */
public interface AggregateDataSaltDao {

	/**
	 * @param objectId the id of the aggregate data source.
	 * @return the stored salt of the source, or empty when none has been stored yet.
	 */
	Optional<String> getSalt(long objectId);

	/**
	 * Store the given salt for the source unless the source already has one. The first salt stored wins and
	 * is never replaced.
	 *
	 * @param objectId     the id of the aggregate data source; it must identify an existing node.
	 * @param candidateSalt the salt to store when the source has none.
	 * @return the salt stored for the source: the candidate, or the salt already stored.
	 */
	String createSaltIfAbsent(long objectId, String candidateSalt);

	/**
	 * Delete every stored salt. For tests only.
	 */
	void truncateAll();
}
