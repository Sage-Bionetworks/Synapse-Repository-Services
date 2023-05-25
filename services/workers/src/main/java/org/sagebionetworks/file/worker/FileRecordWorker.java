package org.sagebionetworks.file.worker;

import com.amazonaws.services.sqs.model.Message;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.audit.KinesisJsonEntityRecord;
import org.sagebionetworks.repo.manager.statistics.ProjectResolver;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileRecord;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;


@Service
public class FileRecordWorker implements TypedMessageDrivenRunner<FileRecord> {
    public static final String FILE_RECORD_UPLOAD_STREAM = "fileUploadRecords";
    public static final String FILE_RECORD_DOWNLOAD_STREAM = "fileDownloadRecords";
    public static final String FILE_UPLOAD_STREAM = "fileUploads";
    public static final String FILE_DOWNLOAD_STREAM = "fileDownloads";
    @Autowired
    private ProjectResolver projectResolver;
    @Autowired
    private AwsKinesisFirehoseLogger firehoseLogger;


    @Override
    public void run(ProgressCallback progressCallback, Message message, FileRecord record) throws RecoverableMessageException, Exception {
        if (ObjectType.FILE_EVENT != record.getObjectType()) {
            throw new IllegalStateException("Unsupported object type: expected " + ObjectType.FILE_EVENT.name() + ", got " + record.getObjectType());
        }


        Long projectId = null;

        try {
            projectId = projectResolver.resolveProject(record.getAssociateType(), record.getAssociateId());
        } catch (NotFoundException | IllegalStateException e) {
            // The object does not exist anymore or there is a loop
             return ;
        }

        record.setProjectId(projectId);
        if (record.getFileEventType() == FileEventType.FILE_DOWNLOAD) {
            firehoseLogger.logBatch(FILE_RECORD_DOWNLOAD_STREAM, Collections.singletonList(
                    new KinesisJsonEntityRecord(record.getTimestamp().getTime(), record, record.getStack(), record.getInstance())));
            firehoseLogger.logBatch(FILE_DOWNLOAD_STREAM, Collections.singletonList(
                    new KinesisJsonEntityRecord(record.getTimestamp().getTime(), record, record.getStack(), record.getInstance())));

        } else if (record.getFileEventType() == FileEventType.FILE_UPLOAD) {
            firehoseLogger.logBatch(FILE_RECORD_UPLOAD_STREAM, Collections.singletonList(
                    new KinesisJsonEntityRecord(record.getTimestamp().getTime(), record, record.getStack(), record.getInstance())));
            firehoseLogger.logBatch(FILE_UPLOAD_STREAM, Collections.singletonList(
                    new KinesisJsonEntityRecord(record.getTimestamp().getTime(), record, record.getStack(), record.getInstance())));
        }
    }

    @Override
    public Class<FileRecord> getObjectClass() {
        return FileRecord.class;
    }
}
