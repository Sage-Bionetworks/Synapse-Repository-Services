package org.sagebionetworks.repo.manager.audit;

import org.sagebionetworks.kinesis.AbstractAwsKinesisLogRecord;
import org.sagebionetworks.schema.adapter.JSONEntity;

import java.util.Objects;

public class KinesisJsonEntityRecord<T extends JSONEntity> extends AbstractAwsKinesisLogRecord {
    private final Long timestamp;
    private final T payload;

    public KinesisJsonEntityRecord(Long timestamp, T jsonEntity, String stack, String instance){
        super(stack,instance);
        this.timestamp = timestamp;
        this.payload = jsonEntity;
    }

    public KinesisJsonEntityRecord(Long timestamp, T jsonEntity) {
        super(null, null);
        this.timestamp = timestamp;
        this.payload = jsonEntity;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "KinesisJsonEntityRecord{" +
                "timestamp=" + timestamp +
                ", payload=" + payload +
                ", stack=" + this.getStack() +
                ", instance=" + this.getInstance() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        KinesisJsonEntityRecord<?> that = (KinesisJsonEntityRecord<?>) o;
        return timestamp.equals(that.timestamp) && payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp, payload);
    }
}
