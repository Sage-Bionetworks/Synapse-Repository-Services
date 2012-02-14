package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.schema.adapter.JSONEntity;

/**
 * Represents basic query results.
 * 
 * @author jmhill
 *
 * @param <T>
 */
public class EntityQueryResults <T extends JSONEntity> {
		
	/**
	 * 
	 * @param results - The sub-list of a single page.
	 * @param totalNumberOfResults
	 */
	public EntityQueryResults(List<T> results, int totalNumberOfResults) {
		super();
		this.results = results;
		this.totalNumberOfResults = totalNumberOfResults;
	}
	
	/**
	 * Given a full list of results and pagination parameters, creates a EntityQueryResults the subList matching the pagination parameters.
	 * @param fullResults - Should be the full list, not just one page.  The fullResults.size() will be used for the totalNumberOfResults.
	 * @param limit - Sets the page size.
	 * @param offest - Sets the start of the page.
	 */
	public EntityQueryResults(List<T> fullResults, int limit, int offest) {
		super();
		if(fullResults == null) throw new IllegalArgumentException("FullResults cannot be null");
		if(offest < 0) throw new IllegalArgumentException("Offset cannot be less than zero");
		if(limit < 0) throw new IllegalArgumentException("Limit cannot be less than zero");
		// Calculate the indices
		// The start is inclusive in List.subList();
		int startIndex = offest;
		// The end is exclusive in List.SubList()
		int endIndex = offest + limit;
		// Note, if limit is Integer.MAX + then offest + limit will be negative.
		if(endIndex > fullResults.size() || endIndex < 0){
			endIndex = fullResults.size();
		}
		this.results = fullResults.subList(startIndex, endIndex);
		this.totalNumberOfResults = fullResults.size();
	}
	
	private List<T> results = null;
	private int totalNumberOfResults;
	public List<T> getResults() {
		return results;
	}
	public void setResults(List<T> results) {
		this.results = results;
	}
	public int getTotalNumberOfResults() {
		return totalNumberOfResults;
	}
	public void setTotalNumberOfResults(int totalNumberOfResults) {
		this.totalNumberOfResults = totalNumberOfResults;
	}

	@Override
	public String toString() {
		return "EntityQueryResults [results=" + results
				+ ", totalNumberOfResults=" + totalNumberOfResults + "]";
	}
	

}
