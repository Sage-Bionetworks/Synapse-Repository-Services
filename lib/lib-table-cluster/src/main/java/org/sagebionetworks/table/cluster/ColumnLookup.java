package org.sagebionetworks.table.cluster;

import java.util.Optional;

import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.query.model.ColumnReference;

@FunctionalInterface
public interface ColumnLookup {

	/**
	 * Attempt to resolve the given ColumnReference to one of the columns one of the
	 * referenced tables. Optional.empty() returned if no match was found.s
	 * 
	 * @param columnReference
	 * @return
	 */
	public Optional<ColumnTranslationReference> lookupColumnReference(ColumnReference columnReference);

}
