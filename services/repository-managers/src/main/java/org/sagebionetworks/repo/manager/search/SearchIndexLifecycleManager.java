package org.sagebionetworks.repo.manager.search;

import java.util.List;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Manager for search index lifecycle operations (create, update, delete).
 * Orchestrates between the OpenSearchManager (data access layer), entity/table managers,
 * configuration resolution, and the SearchIndexStatusDao.
 */
public interface SearchIndexLifecycleManager {

	/**
	 * Handle a create event for a SearchIndex entity. Always deletes any existing AOSS index,
	 * then builds the index from scratch and indexes all data.
	 *
	 * @param progressCallback Progress callback for long-running operations
	 * @param entityId         The SearchIndex entity ID
	 * @param userId           The user who triggered the change
	 */
	void handleCreate(ProgressCallback progressCallback, String entityId, Long userId) throws Exception;

	/**
	 * Handle an update event for a SearchIndex entity. Unconditionally deletes and rebuilds
	 * the AOSS index — simpler and bulletproof for the MVP.
	 *
	 * @param progressCallback Progress callback for long-running operations
	 * @param entityId         The SearchIndex entity ID
	 * @param userId           The user who triggered the change
	 */
	void handleUpdate(ProgressCallback progressCallback, String entityId, Long userId) throws Exception;

	/**
	 * Handle a delete event for a SearchIndex entity. Acquires the per-entity write lock
	 * to serialize with any concurrent build, then deletes the AOSS index and the status row.
	 *
	 * @param progressCallback Progress callback used to hold the per-entity lock
	 * @param entityId         The SearchIndex entity ID
	 */
	void handleDelete(ProgressCallback progressCallback, String entityId) throws Exception;

	/**
	 * Resolve every SELECT-list column in {@code definingSql} — including literals
	 * and aliases not on the source schema — to a persisted {@link
	 * org.sagebionetworks.repo.model.table.ColumnModel} and bind them to the
	 * SearchIndex. Returns the bound column ids in SELECT-list order.
	 */
	List<String> registerSchema(IdAndVersion searchIndexId, String definingSql);
}
