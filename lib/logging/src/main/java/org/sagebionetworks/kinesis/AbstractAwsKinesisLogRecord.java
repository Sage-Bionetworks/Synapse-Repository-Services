package org.sagebionetworks.kinesis;

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

}
