package org.sagebionetworks.repo.manager.audit;

import org.sagebionetworks.kinesis.AbstractAwsKinesisLogRecord;
import org.sagebionetworks.schema.adapter.JSONEntity;

import java.util.Objects;

public class KinesisJsonEntityRecord<T extends JSONEntity> extends AbstractAwsKinesisLogRecord {
    private Long timestamp;
    private T payload;

    public KinesisJsonEntityRecord(Long timestamp, T jsonEntity, String stack, String instance){
        super(stack,instance);
        this.timestamp = timestamp;
        this.payload = jsonEntity;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
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
        KinesisJsonEntityRecord<?> that = (KinesisJsonEntityRecord<?>) o;
        return timestamp.equals(that.timestamp) && payload.equals(that.payload)
                && this.getStack().equals(that.getStack()) && this.getInstance().equals(that.getInstance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, payload, this.getStack(), this.getInstance());
    }
}
