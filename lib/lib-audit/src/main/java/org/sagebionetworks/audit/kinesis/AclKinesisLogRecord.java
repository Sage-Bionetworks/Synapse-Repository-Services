package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.audit.AclRecord;

import java.util.Objects;

public class AclKinesisLogRecord implements AwsKinesisLogRecord {

	public static final String KINESIS_STREAM_NAME = "aclSnapshots";

	private long timestamp;
	private String stack;
	private String instance;
	private AclRecord aclRecord;

	public long getTimestamp() {
		return timestamp;
	}
	public AclKinesisLogRecord withTimestamp(long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	@Override
	public String getStack() {
		return this.stack;
	}
	@Override
	public AwsKinesisLogRecord withStack(String stack) {
		this.stack =stack;
		return this;
	}

	@Override
	public String getInstance() {
		return this.instance;
	}
	@Override
	public AwsKinesisLogRecord withInstance(String instance) {
		this.instance = instance;
		return this;
	}

	public AclRecord getAclRecord() { return this.aclRecord; }
	public AclKinesisLogRecord withAclRecord(AclRecord aclRecord) {
		this.aclRecord = aclRecord;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AclKinesisLogRecord that = (AclKinesisLogRecord) o;
		return timestamp == that.timestamp &&
				Objects.equals(stack, that.stack) &&
				Objects.equals(instance, that.instance) &&
				aclRecord.equals(that.aclRecord);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, stack, instance, aclRecord);
	}

	@Override
	public String toString() {
		return "AclKinesisLogRecord{" +
				"timestamp=" + timestamp +
				", stack='" + stack + '\'' +
				", instance='" + instance + '\'' +
				", aclRecord=" + aclRecord +
				'}';
	}
}
