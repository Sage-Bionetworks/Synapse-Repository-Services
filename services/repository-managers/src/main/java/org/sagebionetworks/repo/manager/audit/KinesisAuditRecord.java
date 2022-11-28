package org.sagebionetworks.repo.manager.audit;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;

public class KinesisAuditRecord implements AwsKinesisLogRecord {

    public static final String STREAM_NAME = "auditData";

    private Long timestamp;
    private Object payload;

    private String stack;
    private String instance;

    public Long getTimestamp() {
        return timestamp;
    }

    public KinesisAuditRecord withTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Object getPayload() {
        return payload;
    }

    public KinesisAuditRecord withPayload(Object payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public String toString() {
        return "KinesisPayloadRecord{" +
                "timestamp=" + timestamp +
                ", payload=" + payload +
                ", stack='" + stack + '\'' +
                ", instance='" + instance + '\'' +
                '}';
    }

    @Override
    public String getStack() {
        return stack;
    }

    @Override
    public KinesisAuditRecord withStack(String stack) {
        this.stack = stack;
        return this;
    }

    @Override
    public String getInstance() {
        return instance;
    }

    @Override
    public KinesisAuditRecord withInstance(String instance) {
        this.instance = instance;
        return this;
    }
}
