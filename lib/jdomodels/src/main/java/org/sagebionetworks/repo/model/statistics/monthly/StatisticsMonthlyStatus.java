package org.sagebionetworks.repo.model.statistics.monthly;

import java.time.YearMonth;
import java.util.Objects;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;

public class StatisticsMonthlyStatus {

	private StatisticsObjectType objectType;
	private YearMonth month;
	private StatisticsStatus status;
	private Long lastStartedOn;
	private Long lastUpdatedOn;

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

	public Long getLastStartedOn() {
		return lastStartedOn;
	}

	public void setLastStartedOn(Long lastStartedOn) {
		this.lastStartedOn = lastStartedOn;
	}

	public Long getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Long lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lastStartedOn, lastUpdatedOn, month, objectType, status);
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
		return Objects.equals(lastStartedOn, other.lastStartedOn) && Objects.equals(lastUpdatedOn, other.lastUpdatedOn)
				&& Objects.equals(month, other.month) && objectType == other.objectType && status == other.status;
	}

	@Override
	public String toString() {
		return "StatisticsMonthlyStatus [objectType=" + objectType + ", month=" + month + ", status=" + status + ", lastStartedOn="
				+ lastStartedOn + ", lastUpdatedOn=" + lastUpdatedOn + "]";
	}

}
