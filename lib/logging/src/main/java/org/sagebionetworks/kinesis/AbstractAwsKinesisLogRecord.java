package org.sagebionetworks.kinesis;

import java.util.Objects;

public abstract class AbstractAwsKinesisLogRecord implements AwsKinesisLogRecord {
    private String stack;
    private String instance;

    public AbstractAwsKinesisLogRecord(String stack, String instance) {
        this.stack = stack;
        this.instance = instance;
    }

    @Override
    public String getStack() {
        return stack;
    }

    @Override
    public String getInstance() {
        return instance;
    }
    @Override
    public AwsKinesisLogRecord withStack(String stack) {
        this.stack =stack;
        return this;
    }

    @Override
    public AwsKinesisLogRecord withInstance(String instance) {
        this.instance = instance;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractAwsKinesisLogRecord that = (AbstractAwsKinesisLogRecord) o;
        return stack.equals(that.stack) && instance.equals(that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, instance);
    }
}
