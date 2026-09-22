package org.sagebionetworks.repo.model.dao.table;

import java.util.Optional;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnProvenance;

/**
 * DAO for the derived, non-migrated column-provenance cache. Rows are lazily (re)populated per stack
 * from the defining SQL and bound schema of a defining-SQL object.
 */
public interface ColumnProvenanceDao {

	/**
	 * Store (upsert) the provenance document for the given object version.
	 *
	 * @param object     the object version the provenance describes.
	 * @param provenance the immediate-lineage document to cache.
	 */
	void saveColumnProvenance(IdAndVersion object, ColumnProvenance provenance);

	/**
	 * @param object the object version to look up.
	 * @return the cached provenance document, or empty if none has been computed yet.
	 */
	Optional<ColumnProvenance> getColumnProvenance(IdAndVersion object);

	/**
	 * Remove any cached provenance for the given object version so it will be recomputed on next read.
	 *
	 * @param object the object version to clear.
	 */
	void clear(IdAndVersion object);

	/**
	 * Remove all cached provenance rows (test/bootstrap support).
	 */
	void truncateAll();

}
