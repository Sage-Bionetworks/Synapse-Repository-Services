package org.sagebionetworks.file.worker;

import com.amazonaws.services.sqs.model.Message;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.audit.KinesisJsonEntityRecord;
import org.sagebionetworks.repo.manager.statistics.ProjectResolver;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileEvent;
import org.sagebionetworks.repo.model.file.FileEventRecord;
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

        FileEventRecord record = new FileEventRecord().setUserId(event.getUserId()).setFileHandleId(event.getFileHandleId()).
                setAssociateId(event.getAssociateId()).setAssociateType(event.getAssociateType()).setProjectId(projectId);
        // KinesisJsonEntityRecord contains json entity FileEventRecord which logges data in new kinesis stream in json format.
        KinesisJsonEntityRecord kinesisJsonEntityRecord = new KinesisJsonEntityRecord(event.getTimestamp().getTime(), record,
                event.getStack(), event.getInstance());
        //StatisticsFileEvent is required for old kinesis stream which stores data in parquet in defined schema.
        // so we can not use KinesisJsonEntityRecord for old stream for more information see PLFM-7754
        StatisticsFileEventRecord statisticsFileEventRecord = new StatisticsFileEventRecord(event.getStack(), event.getInstance(),
                event.getTimestamp().getTime(), record);

        switch (event.getFileEventType()) {
            case FILE_DOWNLOAD:
                firehoseLogger.logBatch(FILE_RECORD_DOWNLOAD_STREAM, Collections.singletonList(kinesisJsonEntityRecord));
                // Keep old streams for backward compatibility, for more information see PLFM-7754
                firehoseLogger.logBatch(FILE_DOWNLOAD_STREAM, Collections.singletonList(statisticsFileEventRecord));
                return;
            case FILE_UPLOAD:
                firehoseLogger.logBatch(FILE_RECORD_UPLOAD_STREAM, Collections.singletonList(kinesisJsonEntityRecord));
                // Keep old streams for backward compatibility, for more information see PLFM-7754
                firehoseLogger.logBatch(FILE_UPLOAD_STREAM, Collections.singletonList(statisticsFileEventRecord));
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
