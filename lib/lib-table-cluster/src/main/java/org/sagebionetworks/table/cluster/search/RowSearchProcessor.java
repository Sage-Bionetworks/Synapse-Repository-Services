package org.sagebionetworks.table.cluster.search;

import java.util.Optional;

@FunctionalInterface
public interface RowSearchProcessor {

	/**
	 * Process the given row data to compute a single string to use in the search index. Note that multi-value types are encoded as JSON arrays in the cell value
	 * 
	 * @param rowData The data for a single row
	 * @return An optional SearchRowContent containing the search content, empty if nothing to index
	 */
	Optional<RowSearchContent> process(TableRowData rowData);
	
}
