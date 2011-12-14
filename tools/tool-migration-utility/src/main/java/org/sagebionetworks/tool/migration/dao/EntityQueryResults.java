package org.sagebionetworks.tool.migration.dao;

import java.util.List;


/**
 * The query results for an given query.
 * @author jmhill
 *
 */
public class EntityQueryResults {
	
	private List<EntityData> resutls;
	private long totalCount = 0;
	public List<EntityData> getResutls() {
		return resutls;
	}
	public void setResutls(List<EntityData> resutls) {
		this.resutls = resutls;
	}
	public long getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}
	public EntityQueryResults(List<EntityData> resutls, long totalCount) {
		super();
		this.resutls = resutls;
		this.totalCount = totalCount;
	}

}
