package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Container class for result data from a user query.
 *
 */
public class NodeQueryResults {
	
	private List<String> resultIds;
	private List<Map<String, Object>> allSelectedData;
	private long totalNumberOfResults;
		
	public NodeQueryResults(List<String> resultIds, List<Map<String, Object>> allSelectedData, long totalCount){	
		this.resultIds = resultIds;
		this.allSelectedData = allSelectedData;
		this.totalNumberOfResults = totalCount;
	}
	
	public NodeQueryResults() {
		this.resultIds =  new ArrayList<String>();
		this.allSelectedData = new ArrayList<Map<String, Object>>();
		this.totalNumberOfResults = 0;
	}

	public List<String> getResultIds() {
		return resultIds;
	}
	public void setResultIds(List<String> resultIds) {
		this.resultIds = resultIds;
	}
	public long getTotalNumberOfResults() {
		return totalNumberOfResults;
	}
	public void setTotalNumberOfResults(int totalNumberOfResults) {
		this.totalNumberOfResults = totalNumberOfResults;
	}

	public List<Map<String, Object>> getAllSelectedData() {
		return allSelectedData;
	}

	public void setAllSelectedData(List<Map<String, Object>> allSelectedData) {
		this.allSelectedData = allSelectedData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((resultIds == null) ? 0 : resultIds.hashCode());
		result = prime * result
				+ (int) (totalNumberOfResults ^ (totalNumberOfResults >>> 32));
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
		NodeQueryResults other = (NodeQueryResults) obj;
		if (resultIds == null) {
			if (other.resultIds != null)
				return false;
		} else if (!resultIds.equals(other.resultIds))
			return false;
		if (totalNumberOfResults != other.totalNumberOfResults)
			return false;
		return true;
	}

}
