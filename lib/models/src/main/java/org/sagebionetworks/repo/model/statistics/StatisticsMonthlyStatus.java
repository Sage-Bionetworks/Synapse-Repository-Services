package org.sagebionetworks.repo.model.statistics;

import java.util.Objects;

public class StatisticsMonthlyStatus {

	private StatisticsObjectType objectType;
	private MonthOfTheYear month;
	private StatisticsStatus status;

	public StatisticsObjectType getObjectType() {
		return objectType;
	}

	public void setObjectType(StatisticsObjectType objectType) {
		this.objectType = objectType;
	}

	public MonthOfTheYear getMonth() {
		return month;
	}
	
	public void setMonth(MonthOfTheYear month) {
		this.month = month;
	}
	
	public StatisticsStatus getStatus() {
		return status;
	}

	public void setStatus(StatisticsStatus status) {
		this.status = status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(month, objectType, status);
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
		StatisticsMonthlyStatus other = (StatisticsMonthlyStatus) obj;
		return Objects.equals(month, other.month) && objectType == other.objectType && status == other.status;
	}

	@Override
	public String toString() {
		return "StatisticsMonthlyStatus [objectType=" + objectType + ", month=" + month + ", status=" + status + "]";
	}

	
}
