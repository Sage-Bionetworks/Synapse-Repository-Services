package org.sagebionetworks.table.model;

/**
 * A grouping of all rows that have values that exist for the same columns of a
 * change set.
 *
 */
public interface Grouping {

	/**
	 * Get the column ID that all rows within this grouping have valid values
	 * for.
	 * 
	 * @return
	 */
	public Iterable<String> getColumnIdsWithValues();

	/**
	 * Iterate over all rows within this grouping.
	 * 
	 * @return
	 */
	public Iterable<SparseRow> rowIterator();

}
