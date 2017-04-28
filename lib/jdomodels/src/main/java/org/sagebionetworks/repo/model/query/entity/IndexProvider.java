package org.sagebionetworks.repo.model.query.entity;

/**
 * Single source of index values used to build all parts of a query.
 *
 */
public class IndexProvider {

	private int index;

	public IndexProvider() {
		index = 0;
	}

	/**
	 * Get the next index value.
	 * 
	 * @return
	 */
	public int nextIndex() {
		return index++;
	}

}
