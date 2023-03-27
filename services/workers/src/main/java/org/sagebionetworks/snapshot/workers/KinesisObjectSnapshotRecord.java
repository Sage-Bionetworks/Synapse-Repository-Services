package org.sagebionetworks.snapshot.workers;

import java.util.Objects;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONEntity;

public class KinesisObjectSnapshotRecord<T extends JSONEntity> implements AwsKinesisLogRecord {

	private String stack;
	private String instance;

	private ObjectType objectType;
	private ChangeType changeType;
	private Long timestamp;
	private Long userId;
	private T snapshot;

	public KinesisObjectSnapshotRecord() {}
	
	public ObjectType getObjectType() {
		return objectType;
	}
	
	public KinesisObjectSnapshotRecord<T> withObjectType(ObjectType objectType) {
		this.objectType = objectType;
		return this;
	}

	public ChangeType getChangeType() {
		return changeType;
	}
	
	public KinesisObjectSnapshotRecord<T> withChangeType(ChangeType changeType) {
		this.changeType = changeType;
		return this;
	}

	public Long getTimestamp() {
		return timestamp;
	}
	
	public KinesisObjectSnapshotRecord<T> withTimestamp(Long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public Long getUserId() {
		return userId;
	}
	
	public KinesisObjectSnapshotRecord<T> withUserId(Long userId) {
		this.userId = userId;
		return this;
	}

	public T getSnapshot() {
		return snapshot;
	}
	
	public KinesisObjectSnapshotRecord<T> withSnapshot(T snapshot) {
		this.snapshot = snapshot;
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
	
	public static final <T extends JSONEntity> KinesisObjectSnapshotRecord<T> map(ChangeMessage message, T snapshot) {
		return new KinesisObjectSnapshotRecord<T>()
			.withChangeType(message.getChangeType())
			.withObjectType(message.getObjectType())
			.withTimestamp(message.getTimestamp().getTime())
			.withUserId(message.getUserId())
			.withSnapshot(snapshot);
	}

	@Override
	public int hashCode() {
		return Objects.hash(changeType, instance, objectType, snapshot, stack, timestamp, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof KinesisObjectSnapshotRecord)) {
			return false;
		}
		KinesisObjectSnapshotRecord<?> other = (KinesisObjectSnapshotRecord<?>) obj;
		return changeType == other.changeType && Objects.equals(instance, other.instance) && objectType == other.objectType
				&& Objects.equals(snapshot, other.snapshot) && Objects.equals(stack, other.stack)
				&& Objects.equals(timestamp, other.timestamp) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "KinesisObjectSnapshotRecord [stack=" + stack + ", instance=" + instance + ", objectType=" + objectType + ", changeType="
				+ changeType + ", timestamp=" + timestamp + ", userId=" + userId + ", snapshot=" + snapshot + "]";
	}
	
}
