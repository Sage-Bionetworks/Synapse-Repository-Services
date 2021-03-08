package org.sagebionetworks.repo.manager.file.scanner;

import java.util.Objects;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

/**
 * Record sent to kinesis
 */
public class FileHandleAssociationRecord implements AwsKinesisLogRecord {
	
	public static final String KINESIS_STREAM_NAME = "fileHandleAssociations";

	private Long timestamp;
	private FileHandleAssociateType associateType;
	private Long associateId;
	private Long fileHandleId;
	private String stack;
	private String instance;

	public Long getTimestamp() {
		return timestamp;
	}

	public FileHandleAssociationRecord withTimestamp(Long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public FileHandleAssociateType getAssociateType() {
		return associateType;
	}

	public FileHandleAssociationRecord withAssociateType(FileHandleAssociateType associateType) {
		this.associateType = associateType;
		return this;
	}

	public Long getAssociateId() {
		return associateId;
	}

	public FileHandleAssociationRecord withAssociateId(Long associateId) {
		this.associateId = associateId;
		return this;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public FileHandleAssociationRecord withFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
		return this;
	}

	@Override
	public String getStack() {
		return stack;
	}

	@Override
	public AwsKinesisLogRecord withStack(String stack) {
		this.stack = stack;
		return this;
	}

	@Override
	public String getInstance() {
		return instance;
	}

	@Override
	public AwsKinesisLogRecord withInstance(String instance) {
		this.instance = instance;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(associateId, associateType, fileHandleId, instance, stack, timestamp);
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
		FileHandleAssociationRecord other = (FileHandleAssociationRecord) obj;
		return Objects.equals(associateId, other.associateId) && associateType == other.associateType
				&& Objects.equals(fileHandleId, other.fileHandleId) && Objects.equals(instance, other.instance)
				&& Objects.equals(stack, other.stack) && Objects.equals(timestamp, other.timestamp);
	}

	@Override
	public String toString() {
		return "FileHandleAssociationRecord [timestamp=" + timestamp + ", associateType=" + associateType + ", associateId=" + associateId
				+ ", fileHandleId=" + fileHandleId + ", stack=" + stack + ", instance=" + instance + "]";
	}

}
