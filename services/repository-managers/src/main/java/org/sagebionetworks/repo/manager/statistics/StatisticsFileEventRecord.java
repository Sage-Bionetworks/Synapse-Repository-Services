package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.kinesis.AbstractAwsKinesisLogRecord;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

public class StatisticsFileEventRecord extends AbstractAwsKinesisLogRecord {
    private Long userId;
    private Long timestamp;
    private Long projectId;
    private String fileHandleId;
    private FileHandleAssociateType associateType;
    private String associateId;
    private String stack;
    private String instance;

    public StatisticsFileEventRecord(String stack, String instance) {
        super(stack, instance);
    }

    public Long getUserId() {
        return userId;
    }

    public StatisticsFileEventRecord withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public StatisticsFileEventRecord withTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Long getProjectId() {
        return projectId;
    }

    public StatisticsFileEventRecord withProjectId(Long projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getFileHandleId() {
        return fileHandleId;
    }

    public StatisticsFileEventRecord withFileHandleId(String fileHandleId) {
        this.fileHandleId = fileHandleId;
        return this;
    }

    public FileHandleAssociateType getAssociateType() {
        return associateType;
    }

    public StatisticsFileEventRecord withAssociateType(FileHandleAssociateType associateType) {
        this.associateType = associateType;
        return this;
    }

    public String getAssociateId() {
        return associateId;
    }

    public StatisticsFileEventRecord withAssociateId(String associateId) {
        this.associateId = associateId;
        return this;
    }

    @Override
    public String getStack() {
        return stack;
    }

    public StatisticsFileEventRecord withStack(String stack) {
        this.stack = stack;
        return this;
    }

    @Override
    public String getInstance() {
        return instance;
    }

    public StatisticsFileEventRecord withInstance(String instance) {
        this.instance = instance;
        return this;
    }
}
