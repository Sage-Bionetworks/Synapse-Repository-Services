package org.sagebionetworks.repo.model.athena;

import java.util.List;
import java.util.Objects;

public class AthenaQueryResultPage<T> {

	private String queryExecutionId;
	private List<T> results;
	private String nextPageToken;

	public String getQueryExecutionId() {
		return queryExecutionId;
	}
	
	public AthenaQueryResultPage<T> withQueryExecutionId(String queryExecutionId) {
		this.queryExecutionId = queryExecutionId;
		return this;
	}

	public List<T> getResults() {
		return results;
	}
	
	public AthenaQueryResultPage<T> withResults(List<T> pageResults) {
		this.results = pageResults;
		return this;
	}

	public String getNextPageToken() {
		return nextPageToken;
	}

	public AthenaQueryResultPage<T> withNextPageToken(String nextPageToken) {
		this.nextPageToken = nextPageToken;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nextPageToken, queryExecutionId, results);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AthenaQueryResultPage other = (AthenaQueryResultPage) obj;
		return Objects.equals(nextPageToken, other.nextPageToken) && Objects.equals(queryExecutionId, other.queryExecutionId)
				&& Objects.equals(results, other.results);
	}

	@Override
	public String toString() {
		return "AthenaQueryResultPage [queryExecutionId=" + queryExecutionId + ", resultPage=" + results + ", nextPageToken="
				+ nextPageToken + "]";
	}

}
