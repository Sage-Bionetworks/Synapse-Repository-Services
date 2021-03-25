package org.sagebionetworks.file.worker;

import java.util.Objects;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;

/**
 * DTO for sending file handle data to kinesis
 */
public class FileHandleRecord implements AwsKinesisLogRecord {
	
	private long id;
	private long createdOn;
	private String status;
	private boolean isPreview;
	private String stack;
	private String instance;

	public long getId() {
		return id;
	}

	public FileHandleRecord withId(long id) {
		this.id = id;
		return this;
	}

	public long getCreatedOn() {
		return createdOn;
	}

	public FileHandleRecord withCreatedOn(long createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public FileHandleRecord withStatus(String status) {
		this.status = status;
		return this;
	}

	public boolean getIsPreview() {
		return isPreview;
	}

	public FileHandleRecord withIsPreview(boolean isPreview) {
		this.isPreview = isPreview;
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
		return Objects.hash(createdOn, id, instance, isPreview, stack, status);
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
		FileHandleRecord other = (FileHandleRecord) obj;
		return createdOn == other.createdOn && id == other.id && Objects.equals(instance, other.instance) && isPreview == other.isPreview
				&& Objects.equals(stack, other.stack) && Objects.equals(status, other.status);
	}

	@Override
	public String toString() {
		return "FileHandleRecord [id=" + id + ", createdOn=" + createdOn + ", status=" + status + ", isPreview=" + isPreview + ", stack="
				+ stack + ", instance=" + instance + "]";
	}
}
