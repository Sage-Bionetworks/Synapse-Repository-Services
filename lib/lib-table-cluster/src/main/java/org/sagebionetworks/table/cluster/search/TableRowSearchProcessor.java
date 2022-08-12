package org.sagebionetworks.table.cluster.search;

@FunctionalInterface
public interface TableRowSearchProcessor {

	/**
	 * Process the given {@link TableRowData} to compute a single string to use in the search index.
	 * 
	 * @param rowData The data fetched from a single row in a table
	 * @param includeRowId True if the id of the row should be included in the output
	 * @return A string denoting the value computed from the given list, can be null
	 */
	String process(TableRowData rowData, boolean includeRowId);
	
}
