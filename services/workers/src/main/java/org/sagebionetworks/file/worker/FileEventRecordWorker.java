package org.sagebionetworks.file.worker;

import com.amazonaws.services.sqs.model.Message;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.audit.KinesisJsonEntityRecord;
import org.sagebionetworks.repo.manager.file.FileRecordUtils;
import org.sagebionetworks.repo.manager.statistics.ProjectResolver;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileEvent;
import org.sagebionetworks.repo.model.file.FileRecord;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;


@Service
public class FileEventRecordWorker implements TypedMessageDrivenRunner<FileEvent> {
    public static final String FILE_RECORD_UPLOAD_STREAM = "fileUploadRecords";
    public static final String FILE_RECORD_DOWNLOAD_STREAM = "fileDownloadRecords";
    public static final String FILE_UPLOAD_STREAM = "fileUploads";
    public static final String FILE_DOWNLOAD_STREAM = "fileDownloads";
    @Autowired
    private ProjectResolver projectResolver;
    @Autowired
    private AwsKinesisFirehoseLogger firehoseLogger;

    @Autowired
    private StackConfiguration configuration;


    @Override
    public void run(ProgressCallback progressCallback, Message message, FileEvent event) throws RecoverableMessageException, Exception {
        if (ObjectType.FILE_EVENT != event.getObjectType()) {
            throw new IllegalStateException("Unsupported object type: expected " + ObjectType.FILE_EVENT.name() + ", got " + event.getObjectType());
        }


        Long projectId = null;

        try {
            projectId = projectResolver.resolveProject(event.getAssociateType(), event.getAssociateId());
        } catch (NotFoundException | IllegalStateException e) {
            // The object does not exist anymore or there is a loop
            return;
        }
        FileRecord record = FileRecordUtils.buildFileRecord(event.getUserId(), event.getFileHandleId(),
                event.getAssociateId(), event.getAssociateType());
        record.setProjectId(projectId);
        KinesisJsonEntityRecord kinesisJsonEntityRecord = new KinesisJsonEntityRecord(record.getTimestamp(),
                record, configuration.getStack(), configuration.getStackInstance());

        switch (event.getFileEventType()) {
            case FILE_DOWNLOAD:
                firehoseLogger.logBatch(FILE_RECORD_DOWNLOAD_STREAM, Collections.singletonList(kinesisJsonEntityRecord));
                // Keep old streams for backward compatibility, for more information see PLFM-7754
                firehoseLogger.logBatch(FILE_DOWNLOAD_STREAM, Collections.singletonList(kinesisJsonEntityRecord));
                return;
            case FILE_UPLOAD:
                firehoseLogger.logBatch(FILE_RECORD_UPLOAD_STREAM, Collections.singletonList(kinesisJsonEntityRecord));
                // Keep old streams for backward compatibility, for more information see PLFM-7754
                firehoseLogger.logBatch(FILE_UPLOAD_STREAM, Collections.singletonList(kinesisJsonEntityRecord));
                return;
            default:
                throw new IllegalArgumentException("Unsupported event type: " + event.getFileEventType().name());

        }
    }

    @Override
    public Class<FileEvent> getObjectClass() {
        return FileEvent.class;
    }
}
