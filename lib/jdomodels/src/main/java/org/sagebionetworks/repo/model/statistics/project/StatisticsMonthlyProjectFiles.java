package org.sagebionetworks.repo.model.statistics.project;

import java.time.YearMonth;
import java.util.Objects;

import org.sagebionetworks.repo.model.statistics.FileEvent;

public class StatisticsMonthlyProjectFiles {

	private Long projectId;
	private YearMonth month;
	private FileEvent eventType;
	private Integer filesCount;
	private Integer usersCount;
	private Long lastUpdatedOn;

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public YearMonth getMonth() {
		return month;
	}

	public void setMonth(YearMonth month) {
		this.month = month;
	}

	public FileEvent getEventType() {
		return eventType;
	}

	public void setEventType(FileEvent eventType) {
		this.eventType = eventType;
	}

	public Integer getFilesCount() {
		return filesCount;
	}

	public void setFilesCount(Integer filesCount) {
		this.filesCount = filesCount;
	}

	public Integer getUsersCount() {
		return usersCount;
	}

	public void setUsersCount(Integer usersCount) {
		this.usersCount = usersCount;
	}

	public Long getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Long lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(eventType, filesCount, lastUpdatedOn, month, projectId, usersCount);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatisticsMonthlyProjectFiles other = (StatisticsMonthlyProjectFiles) obj;
		return eventType == other.eventType && Objects.equals(filesCount, other.filesCount)
				&& Objects.equals(lastUpdatedOn, other.lastUpdatedOn) && Objects.equals(month, other.month)
				&& Objects.equals(projectId, other.projectId) && Objects.equals(usersCount, other.usersCount);
	}

	@Override
	public String toString() {
		return "StatisticsMonthlyProjectFiles [projectId=" + projectId + ", month=" + month + ", eventType=" + eventType + ", filesCount="
				+ filesCount + ", usersCount=" + usersCount + ", lastUpdatedOn=" + lastUpdatedOn + "]";
	}

}
