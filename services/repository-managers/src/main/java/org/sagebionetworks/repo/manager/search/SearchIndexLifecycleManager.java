package org.sagebionetworks.repo.manager.search;

import java.util.List;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Orchestrates the build / rebuild / delete lifecycle of an AOSS index that backs a
 * {@code SearchIndex} entity. Implementations serialize work per entity via a
 * cluster-wide write lock, resolve the effective {@code SearchConfiguration}, validate
 * referenced analyzer / synonym qnames, build the index, stream rows from the source
 * table as the realm's anonymous user (so {@code addRowLevelFilter} enforces benefactor
 * ACLs and only publicly-visible rows reach AOSS), and persist final state into
 * {@code SEARCH_INDEX_STATUS}. Transient failures propagate so the worker can translate
 * them to {@code RecoverableMessageException}; permanent failures are recorded as
 * FAILED with a truncated error message.
 */
public interface SearchIndexLifecycleManager {

	/**
	 * Handle a create event for a SearchIndex entity. Always deletes any existing AOSS index,
	 * then builds the index from scratch and indexes all data.
	 *
	 * @param progressCallback Progress callback used to refresh the per-entity write lock
	 *        while the build runs.
	 * @param entityId         The SearchIndex entity ID being built.
	 * @throws Exception transient failures (table unavailable, AOSS retryable error)
	 *         propagate so the worker can re-queue. Permanent failures are recorded as
	 *         FAILED on {@code SearchIndexStatus} and not rethrown.
	 */
	void handleCreate(ProgressCallback progressCallback, String entityId) throws Exception;

	/**
	 * Handle an update event for a SearchIndex entity. Unconditionally deletes and rebuilds
	 * the AOSS index — simpler and bulletproof for the MVP.
	 *
	 * @param progressCallback Progress callback used to refresh the per-entity write lock
	 *        while the rebuild runs.
	 * @param entityId         The SearchIndex entity ID being rebuilt.
	 * @throws Exception same retry / fail-recording semantics as {@link #handleCreate}.
	 */
	void handleUpdate(ProgressCallback progressCallback, String entityId) throws Exception;

	/**
	 * Handle a delete event for a SearchIndex entity. Acquires the per-entity write lock
	 * to serialize with any concurrent build, then deletes the AOSS index and the status row.
	 *
	 * @param progressCallback Progress callback used to hold the per-entity write lock.
	 * @param entityId         The SearchIndex entity ID being deleted.
	 * @throws Exception transient AOSS / lock errors propagate; the AOSS delete itself is
	 *         idempotent.
	 */
	void handleDelete(ProgressCallback progressCallback, String entityId) throws Exception;

	/**
	 * Resolve every SELECT-list column in {@code definingSql} — including literals
	 * and aliases not on the source schema — to a persisted {@link
	 * org.sagebionetworks.repo.model.table.ColumnModel} and bind them to the
	 * SearchIndex. Called from the entity metadata provider on create / update so a
	 * malformed query fails synchronously with HTTP 400 instead of FAILED'ing the async
	 * build.
	 *
	 * @param searchIndexId The SearchIndex entity ID (with version) whose schema is being
	 *                      bound.
	 * @param definingSql   The SQL the SearchIndex is defined by.
	 * @return The bound column ids in SELECT-list order — also the order rows stream out
	 *         at index-build time.
	 */
	List<String> registerSchema(IdAndVersion searchIndexId, String definingSql);

	/**
	 * Under the per-entity write lock, rebuild
	 * the index when it is WAITING_FOR_SOURCE (first-availability) or when it is ACTIVE and its source's
	 * content version has moved since the last build (live-sync); no-op otherwise (covers a CREATING
	 * index, a FAILED index, and a no-op source touch of an up-to-date ACTIVE index). If the lock is
	 * held by an in-flight build, the message is consumed and a fresh rebuild request is republished
	 * (NOT a recoverable retry, which would DLQ before a minutes-long build releases the lock).
	 *
	 * @param progressCallback Refreshes the per-entity write lock while the rebuild runs.
	 * @param entityId         The dependent SearchIndex entity id to rebuild.
	 * @throws Exception transient failures propagate so the worker can re-queue.
	 */
	void rebuildIfStale(ProgressCallback progressCallback, String entityId) throws Exception;
}
