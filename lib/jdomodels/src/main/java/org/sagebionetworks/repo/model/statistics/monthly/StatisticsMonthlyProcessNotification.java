package org.sagebionetworks.repo.model.statistics.monthly;

import java.time.YearMonth;
import java.util.Objects;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bean to send out SQS notifications
 * 
 * @author Marco
 *
 */
public class StatisticsMonthlyProcessNotification {

	private StatisticsObjectType objectType;
	private YearMonth month;

	@JsonCreator
	public StatisticsMonthlyProcessNotification(@JsonProperty("objectType") StatisticsObjectType objectType,
			@JsonProperty("month") YearMonth month) {
		this.objectType = objectType;
		this.month = month;
	}

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

	@Override
	public int hashCode() {
		return Objects.hash(month, objectType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatisticsMonthlyProcessNotification other = (StatisticsMonthlyProcessNotification) obj;
		return Objects.equals(month, other.month) && objectType == other.objectType;
	}

	@Override
	public String toString() {
		return "StatisticsMonthlyProcessNotification [objectType=" + objectType + ", month=" + month + "]";
	}

}
