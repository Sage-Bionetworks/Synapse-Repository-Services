package org.sagebionetworks.table.cluster.search;

import java.util.List;

@FunctionalInterface
public interface RowSearchProcessor {

	/**
	 * Process the given list of values to compute a single string to use in the search index.
	 * 
	 * @param values A list of typed values, in case of multi-value types the raw data is encoded as a JSON array
	 * @return A string denoting the value computed from the given list, can be null
	 */
	String process(List<TypedCellValue> data);
	
}
