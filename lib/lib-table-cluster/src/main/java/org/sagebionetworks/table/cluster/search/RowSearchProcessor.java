package org.sagebionetworks.table.cluster.search;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnModel;

@FunctionalInterface
public interface RowSearchProcessor {

	/**
	 * Process the given list of values to compute a single string to use in the search index. Note that multi-value types are encoded as JSON arrays
	 * 
	 * @param columns The list of columns mapped to the values
	 * @param rowValues The list of values for each column
	 * @return An optional single string containing the search content, empty if nothing to index
	 */
	Optional<String> process(List<ColumnModel> columns, List<String> rowValues);
	
}
