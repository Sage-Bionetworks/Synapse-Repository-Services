package org.sagebionetworks.repo.model.athena;

import java.util.Objects;

import com.amazonaws.services.athena.model.QueryExecution;

/**
 * Adapter from AWS {@link QueryExecution} to our {@link AthenaQueryExecution}
 * 
 * @author Marco
 *
 */
public class AthenaQueryExecutionAdapter implements AthenaQueryExecution {

	private QueryExecution queryExecution;

	public AthenaQueryExecutionAdapter(QueryExecution queryExecution) {
		this.queryExecution = queryExecution;
	}

	@Override
	public String getQueryExecutionId() {
		return queryExecution.getQueryExecutionId();
	}

	@Override
	public AthenaQueryStatistics getStatistics() {
		return new AthenaQueryStatisticsAdapter(queryExecution.getStatistics());
	}

	@Override
	public AthenaQueryExecutionState getState() {
		return AthenaQueryExecutionState.valueOf(queryExecution.getStatus().getState());
	}

	@Override
	public String getStateChangeReason() {
		return queryExecution.getStatus().getStateChangeReason();
	}

	@Override
	public int hashCode() {
		return Objects.hash(queryExecution);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AthenaQueryExecutionAdapter other = (AthenaQueryExecutionAdapter) obj;
		return Objects.equals(queryExecution, other.queryExecution);
	}

	@Override
	public String toString() {
		return "AthenaQueryExecutionAdapter [queryExecution=" + queryExecution + "]";
	}

}
