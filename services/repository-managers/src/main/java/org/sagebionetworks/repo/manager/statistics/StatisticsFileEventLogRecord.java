package org.sagebionetworks.repo.manager.statistics;

import java.util.Objects;

import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

public class StatisticsFileEventLogRecord implements StatisticsEventLogRecord {

	private Long userId;
	private Long timestamp;
	private Long projectId;
	private String fileHandleId;
	private FileHandleAssociateType associateType;
	private String associateId;
	private String stack;
	private String instance;

	@Override
	public String getStack() {
		return stack;
	}

	@Override
	public StatisticsFileEventLogRecord withStack(String stack) {
		this.stack = stack;
		return this;
	}

	@Override
	public String getInstance() {
		return instance;
	}

	@Override
	public StatisticsFileEventLogRecord withInstance(String instance) {
		this.instance = instance;
		return this;
	}

	@Override
	public Long getTimestamp() {
		return timestamp;
	}

	@Override
	public StatisticsFileEventLogRecord withTimestamp(Long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public StatisticsFileEventLogRecord withUserId(Long userId) {
		this.userId = userId;
		return this;
	}

	public Long getProjectId() {
		return projectId;
	}

	public StatisticsFileEventLogRecord withProjectId(Long projectId) {
		this.projectId = projectId;
		return this;
	}

	public String getFileHandleId() {
		return fileHandleId;
	}

	public StatisticsFileEventLogRecord withFileHandleId(String fileHandleId) {
		this.fileHandleId = fileHandleId;
		return this;
	}

	public FileHandleAssociateType getAssociateType() {
		return associateType;
	}

	public String getAssociateId() {
		return associateId;
	}

	public StatisticsFileEventLogRecord withAssociation(FileHandleAssociateType associateType, String associateId) {
		this.associateType = associateType;
		this.associateId = associateId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(associateId, associateType, fileHandleId, instance, projectId, stack, timestamp, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatisticsFileEventLogRecord other = (StatisticsFileEventLogRecord) obj;
		return Objects.equals(associateId, other.associateId) && associateType == other.associateType
				&& Objects.equals(fileHandleId, other.fileHandleId) && Objects.equals(instance, other.instance)
				&& Objects.equals(projectId, other.projectId) && Objects.equals(stack, other.stack)
				&& Objects.equals(timestamp, other.timestamp) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "StatisticsFileEventLogRecord [userId=" + userId + ", timestamp=" + timestamp + ", projectId="
				+ projectId + ", fileHandleId=" + fileHandleId + ", associateType=" + associateType + ", associateId="
				+ associateId + ", stack=" + stack + ", instance=" + instance + "]";
	}

}
