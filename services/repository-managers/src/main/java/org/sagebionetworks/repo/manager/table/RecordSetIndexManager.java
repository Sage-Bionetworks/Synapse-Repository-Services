package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.util.progress.ProgressCallback;

/**
 * Builds and maintains the queryable indexes for a {@code RecordSet} entity.
 * Each rebuild populates two index tables from the current version's CSV: the
 * entity-level alias T{id} (target of unversioned "syn123" queries) and the
 * per-version immutable snapshot T{id}_{v} (target of "syn123.{v}" queries).
 * Triggered by RECORDSET change messages from
 * {@link org.sagebionetworks.repo.service.metadata.RecordSetMetadataProvider}.
 */
public interface RecordSetIndexManager {

	/**
	 * Build (or rebuild) both index tables for the current version of the given
	 * RecordSet: the entity-level alias T{id} and the per-version snapshot
	 * T{id}_{v}. The schema is inferred from the CSV, persisted via
	 * {@link ColumnModelManager#bindColumnsToDefaultVersionOfObject(java.util.List, String)}
	 * and
	 * {@link ColumnModelManager#bindColumnsToVersionOfObject(java.util.List, IdAndVersion)},
	 * and the same rows are loaded into both index tables in a single CSV pass.
	 */
	void createOrUpdateRecordSetIndex(IdAndVersion idAndVersion, ProgressCallback progressCallback) throws Exception;

	/**
	 * Entity-level delete: drops the entity-level alias T{id} (and the
	 * companion T{id}_STATUS) and unbinds all columns for the object id. Any
	 * version on the input is ignored — there is no version-aware delete.
	 * Per-version snapshot tables T{id}_{v} are intentionally left as
	 * unreachable orphans, matching how TableEntity treats versioned snapshot
	 * index tables after the entity is deleted.
	 */
	void deleteRecordSetIndex(IdAndVersion idAndVersion);

}
