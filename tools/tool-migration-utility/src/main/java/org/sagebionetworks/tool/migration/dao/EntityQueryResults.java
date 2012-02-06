package org.sagebionetworks.tool.migration.dao;

import java.util.List;


/**
 * The query results for an given query.
 * @author jmhill
 *
 */
public class EntityQueryResults {
	
	private List<EntityData> results;
	private long totalCount = 0;
	public List<EntityData> getResults() {
		return results;
	}
	public void setResults(List<EntityData> results) {
		this.results = results;
	}
	public long getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}
	public EntityQueryResults(List<EntityData> results, long totalCount) {
		super();
		this.results = results;
		this.totalCount = totalCount;
	}

}
