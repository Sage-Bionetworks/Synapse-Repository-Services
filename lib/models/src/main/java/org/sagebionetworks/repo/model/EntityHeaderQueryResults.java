package org.sagebionetworks.repo.model;

import java.util.List;

public class EntityHeaderQueryResults {
	private List<EntityHeader> entityHeaders;
	private long totalNumberOfResults;
	
	/**
	 * @return the entityHeaders
	 */
	public List<EntityHeader> getEntityHeaders() {
		return entityHeaders;
	}
	/**
	 * @param entityHeaders the entityHeaders to set
	 */
	public void setEntityHeaders(List<EntityHeader> entityHeaders) {
		this.entityHeaders = entityHeaders;
	}
	/**
	 * @return the totalNumberOfResults
	 */
	public long getTotalNumberOfResults() {
		return totalNumberOfResults;
	}
	/**
	 * @param totalNumberOfResults the totalNumberOfResults to set
	 */
	public void setTotalNumberOfResults(long totalNumberOfResults) {
		this.totalNumberOfResults = totalNumberOfResults;
	}
	

}
