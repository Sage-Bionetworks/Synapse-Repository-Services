package org.sagebionetworks.table.cluster.search;

import java.util.Optional;

import javax.sql.DataSource;

import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;

public interface SearchIndexStatusDao {

	void setDataSource(DataSource dataSource);

	void createTableIfDoesNotExist();

	void createOrUpdate(SearchIndexStatus status);

	Optional<SearchIndexState> getState(Long searchIndexId);

	Optional<SearchIndexStatus> getStatus(Long searchIndexId);

	boolean exists(Long searchIndexId);

	void delete(Long searchIndexId);

	void truncateAll();

	/**
	 * Save (or overwrite) the as-built authorization snapshot for a SearchIndex. The status row already
	 * exists at capture time, so this updates the SNAPSHOT_JSON column in place, leaving the lifecycle
	 * state untouched. Written just before the alias swap that makes the freshly-built index live, so a
	 * reader authorizes against the same as-built state the live index reflects.
	 *
	 * @param searchIndexId the SearchIndex id the snapshot describes.
	 * @param snapshot      the as-built snapshot to persist.
	 */
	void saveSnapshot(Long searchIndexId, IndexAuthorizationSnapshot snapshot);

	/**
	 * @param searchIndexId the SearchIndex id to read.
	 * @return the persisted snapshot, or empty when the status row is absent or no snapshot has been
	 *         captured yet.
	 */
	Optional<IndexAuthorizationSnapshot> getSnapshot(Long searchIndexId);
}
