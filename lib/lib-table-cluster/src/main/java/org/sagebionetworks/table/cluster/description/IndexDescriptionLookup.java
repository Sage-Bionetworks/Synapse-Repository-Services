package org.sagebionetworks.table.cluster.description;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

@FunctionalInterface
public interface IndexDescriptionLookup {

	/**
	 * Get the index description for the given table/view.
	 * @param idAndVersion
	 * @return
	 */
	IndexDescription getIndexDescription(IdAndVersion idAndVersion);
}
