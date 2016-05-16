package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.table.QueryResult;

/**
 * Includes both the query results and count of the total number of rows that match
 * the given query.
 *
 */
public class QueryResultWithCount {

	QueryResult queryResult;
	Long count;
	
	
	/**
	 * 
	 * @param queryResult The results of the query.
	 * @param count The total number of rows for the given query result.
	 * For a paginated query this count will be larger than the number
	 * of rows in the query result.
	 */
	public QueryResultWithCount(QueryResult queryResult, Long count) {
		super();
		this.queryResult = queryResult;
		this.count = count;
	}

	/**
	 * The results of the query.
	 * @return
	 */
	public QueryResult getQueryResult() {
		return queryResult;
	}
	
	/**
	 * The total number of rows for the given query result.
	 * For a paginated query this count will be larger than the number
	 * of rows in the query result. 
	 * 
	 * @return
	 */
	public Long getCount() {
		return count;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((count == null) ? 0 : count.hashCode());
		result = prime * result
				+ ((queryResult == null) ? 0 : queryResult.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryResultWithCount other = (QueryResultWithCount) obj;
		if (count == null) {
			if (other.count != null)
				return false;
		} else if (!count.equals(other.count))
			return false;
		if (queryResult == null) {
			if (other.queryResult != null)
				return false;
		} else if (!queryResult.equals(other.queryResult))
			return false;
		return true;
	}
	
	

}
