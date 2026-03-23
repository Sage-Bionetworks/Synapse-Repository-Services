package org.sagebionetworks.repo.model.grid;

import java.sql.Timestamp;
import java.util.Objects;

public class GridSnapshot {

	private Long id;
	private String sessionId;
	private ClockTable clockTable;
	private Timestamp createdOn;
	private Long createdBy;
	private String s3Key;

	public Long getId() {
		return id;
	}

	public GridSnapshot setId(Long id) {
		this.id = id;
		return this;
	}

	public String getSessionId() {
		return sessionId;
	}

	public GridSnapshot setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public ClockTable getClockTable() {
		return clockTable;
	}

	public GridSnapshot setClockTable(ClockTable clockTable) {
		this.clockTable = clockTable;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public GridSnapshot setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public GridSnapshot setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getS3Key() {
		return s3Key;
	}

	public GridSnapshot setS3Key(String s3Key) {
		this.s3Key = s3Key;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		GridSnapshot that = (GridSnapshot) o;
		return Objects.equals(id, that.id) && Objects.equals(sessionId, that.sessionId) &&
				Objects.equals(clockTable, that.clockTable) && Objects.equals(createdOn, that.createdOn) &&
				Objects.equals(createdBy, that.createdBy) && Objects.equals(s3Key, that.s3Key);
	}


	@Override
	public String toString() {
		return "GridSnapshot{" +
				"id=" + id +
				", sessionId='" + sessionId + '\'' +
				", clockTable=" + clockTable +
				", createdOn=" + createdOn +
				", createdBy=" + createdBy +
				", s3Key='" + s3Key + '\'' +
				'}';
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, sessionId, clockTable, createdOn, createdBy, s3Key);
	}
}
