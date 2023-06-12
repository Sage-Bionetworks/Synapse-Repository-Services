package org.sagebionetworks.file.worker;

import org.sagebionetworks.kinesis.AbstractAwsKinesisLogRecord;
import org.sagebionetworks.repo.model.file.FileEventRecord;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import java.util.Objects;

/**
 * This class is deprecated and only used for backward compatibility.
 */
public class StatisticsFileEventRecord extends AbstractAwsKinesisLogRecord {
    private Long userId;
    private Long timestamp;
    private Long projectId;
    private String fileHandleId;
    private FileHandleAssociateType associateType;
    private String associateId;


    /**
     * Copy constructor.
     *
     * @param fileEventRecord
     */
    public StatisticsFileEventRecord(String stack, String instance, Long timestamp, FileEventRecord fileEventRecord) {
        super(stack, instance);
        this.userId = fileEventRecord.getUserId();
        this.timestamp = timestamp;
        this.projectId = fileEventRecord.getProjectId();
        this.fileHandleId = fileEventRecord.getFileHandleId();
        this.associateType = fileEventRecord.getAssociateType();
        this.associateId = fileEventRecord.getAssociateId();
    }

    public Long getUserId() {
        return userId;
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


    public String getFileHandleId() {
        return fileHandleId;
    }


    public FileHandleAssociateType getAssociateType() {
        return associateType;
    }


    public String getAssociateId() {
        return associateId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StatisticsFileEventRecord that = (StatisticsFileEventRecord) o;
        return userId.equals(that.userId) && timestamp.equals(that.timestamp) && projectId.equals(that.projectId) && fileHandleId.equals(that.fileHandleId) && associateType == that.associateType && associateId.equals(that.associateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, timestamp, projectId, fileHandleId, associateType, associateId);
    }

    @Override
    public String toString() {
        return "StatisticsFileEventRecord{" +
                "userId=" + userId +
                ", timestamp=" + timestamp +
                ", projectId=" + projectId +
                ", fileHandleId='" + fileHandleId + '\'' +
                ", associateType=" + associateType +
                ", associateId='" + associateId + '\'' +
                '}';
    }
}
