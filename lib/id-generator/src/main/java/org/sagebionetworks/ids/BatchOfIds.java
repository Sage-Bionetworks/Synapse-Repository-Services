package org.sagebionetworks.ids;

/**
 * Result of generating a range of IDs.
 *
 */
public class BatchOfIds {
	
	/** 
	 * The first ID in the range (inclusive).
	 */
	Long firstId;
	/**
	 * The last ID in the range (inclusive).
	 */
	Long lastId;
	
	/**
	 * 
	 * @param firstId The first ID in the range (inclusive).
	 * @param lastId The last ID in the range (inclusive).
	 */
	public BatchOfIds(Long firstId, Long lastId) {
		super();
		this.firstId = firstId;
		this.lastId = lastId;
	}

	/**
	 * The first ID in the range (inclusive).
	 * @return
	 */
	public Long getFirstId() {
		return firstId;
	}

	/**
	 * The last ID in the range (inclusive).
	 * @return
	 */
	public Long getLastId() {
		return lastId;
	}
	
}
