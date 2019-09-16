package org.sagebionetworks.repo.model.athena;

import java.util.Objects;

import com.amazonaws.services.athena.model.QueryExecutionStatistics;

/**
 * Adapter from the AWS {@link QueryExecutionStatistics} to our {@link AthenaQueryStatistics} interface
 * 
 * @author Marco
 *
 */
public class AthenaQueryStatisticsAdapter implements AthenaQueryStatistics {

	private QueryExecutionStatistics queryStatistics;

	public AthenaQueryStatisticsAdapter(QueryExecutionStatistics queryStatistics) {
		this.queryStatistics = queryStatistics;
	}

	@Override
	public Long getDataScanned() {
		return queryStatistics.getDataScannedInBytes();
	}

	@Override
	public Long getExecutionTime() {
		return queryStatistics.getEngineExecutionTimeInMillis();
	}

	@Override
	public int hashCode() {
		return Objects.hash(queryStatistics);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AthenaQueryStatisticsAdapter other = (AthenaQueryStatisticsAdapter) obj;
		return Objects.equals(queryStatistics, other.queryStatistics);
	}

	@Override
	public String toString() {
		return "AthenaQueryStatisticsAdapter [queryStatistics=" + queryStatistics + "]";
	}

}
