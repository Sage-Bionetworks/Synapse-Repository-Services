package org.sagebionetworks.repo.model.statistics;

import java.time.YearMonth;
import java.util.Objects;

public class StatisticsMonthlyStatus {

	private StatisticsObjectType objectType;
	private YearMonth month;
	private StatisticsStatus status;
	private Long lastSucceededAt;
	private Long lastFailedAt;

	public StatisticsObjectType getObjectType() {
		return objectType;
	}

	public void setObjectType(StatisticsObjectType objectType) {
		this.objectType = objectType;
	}

	public YearMonth getMonth() {
		return month;
	}

	public void setMonth(YearMonth month) {
		this.month = month;
	}

	public StatisticsStatus getStatus() {
		return status;
	}

	public void setStatus(StatisticsStatus status) {
		this.status = status;
	}

	public Long getLastSucceededAt() {
		return lastSucceededAt;
	}

	public void setLastSucceededAt(Long lastSucceededAt) {
		this.lastSucceededAt = lastSucceededAt;
	}

	public Long getLastFailedAt() {
		return lastFailedAt;
	}

	public void setLastFailedAt(Long lastFailedAt) {
		this.lastFailedAt = lastFailedAt;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lastFailedAt, lastSucceededAt, month, objectType, status);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatisticsMonthlyStatus other = (StatisticsMonthlyStatus) obj;
		return Objects.equals(lastFailedAt, other.lastFailedAt) && Objects.equals(lastSucceededAt, other.lastSucceededAt)
				&& Objects.equals(month, other.month) && objectType == other.objectType && status == other.status;
	}

	@Override
	public String toString() {
		return "StatisticsMonthlyStatus [objectType=" + objectType + ", month=" + month + ", status=" + status + ", lastSucceededAt="
				+ lastSucceededAt + ", lastFailedAt=" + lastFailedAt + "]";
	}

}
