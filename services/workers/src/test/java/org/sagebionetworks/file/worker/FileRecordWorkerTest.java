package org.sagebionetworks.file.worker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.audit.KinesisJsonEntityRecord;
import org.sagebionetworks.repo.manager.statistics.ProjectResolver;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileRecord;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

;

@ExtendWith(MockitoExtension.class)
public class FileRecordWorkerTest {
    @InjectMocks
    FileRecordWorker worker;
    @Mock
    private ProgressCallback mockCallback;
    @Mock
    private ProjectResolver projectResolver;
    @Mock
    private AwsKinesisFirehoseLogger firehoseLogger;

    @Test
    public void testRunForUpload() throws RecoverableMessageException, Exception {
        FileRecord record = new FileRecord().setObjectType(ObjectType.FILE_EVENT)
                .setObjectId("123").setTimestamp(Date.from(Instant.now()))
                .setUserId(1L).setFileEventType(FileEventType.FILE_UPLOAD)
                .setFileHandleId("123").setAssociateId("1")
                .setAssociateType(FileHandleAssociateType.FileEntity).setStack("test").setInstance("test");
      KinesisJsonEntityRecord kinesisJsonEntityRecords = new KinesisJsonEntityRecord(record.getTimestamp().getTime(),
                record, record.getStack(),record.getInstance());

        when(projectResolver.resolveProject(any(),any())).thenReturn(23L);

        // Call under test
        worker.run(mockCallback, new com.amazonaws.services.sqs.model.Message(), record);

        verify(firehoseLogger).logBatch("fileUploadRecords",Collections.singletonList(kinesisJsonEntityRecords));
        verify(firehoseLogger).logBatch("fileUploads",Collections.singletonList(kinesisJsonEntityRecords));

    }

    @Test
    public void testRunForDownload() throws RecoverableMessageException, Exception {
        FileRecord record = new FileRecord().setObjectType(ObjectType.FILE_EVENT)
                .setObjectId("123").setTimestamp(Date.from(Instant.now()))
                .setUserId(1L).setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setFileHandleId("123").setAssociateId("1")
                .setAssociateType(FileHandleAssociateType.FileEntity).setStack("test").setInstance("test");
        KinesisJsonEntityRecord kinesisJsonEntityRecords = new KinesisJsonEntityRecord(record.getTimestamp().getTime(),
                record, record.getStack(),record.getInstance());

        when(projectResolver.resolveProject(any(),any())).thenReturn(23L);

        // Call under test
        worker.run(mockCallback, new com.amazonaws.services.sqs.model.Message(), record);

        verify(firehoseLogger).logBatch("fileDownloadRecords",Collections.singletonList(kinesisJsonEntityRecords));
        verify(firehoseLogger).logBatch("fileDownloads",Collections.singletonList(kinesisJsonEntityRecords));

    }

    @Test
    public void testRunWithWrongObjectType() throws RecoverableMessageException, Exception {
        FileRecord record = new FileRecord().setObjectType(ObjectType.TABLE_STATUS_EVENT)
                .setObjectId("123").setTimestamp(Date.from(Instant.now()))
                .setUserId(1L).setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setFileHandleId("123").setAssociateId("1")
                .setAssociateType(FileHandleAssociateType.FileEntity).setStack("test").setInstance("test");

        // call under test
        String message = assertThrows(IllegalStateException.class, () -> {
            worker.run(mockCallback, new com.amazonaws.services.sqs.model.Message(), record);
        }).getMessage();

        assertEquals(message, "Unsupported object type: expected " + ObjectType.FILE_EVENT.name() + ", got " + record.getObjectType());
    }

    @Test
    public void testRunWithoutProjectId() throws RecoverableMessageException, Exception {
        FileRecord record = new FileRecord().setObjectType(ObjectType.FILE_EVENT)
                .setObjectId("123").setTimestamp(Date.from(Instant.now()))
                .setUserId(1L).setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setFileHandleId("123").setAssociateId("1")
                .setAssociateType(FileHandleAssociateType.FileEntity).setStack("test").setInstance("test");

        when(projectResolver.resolveProject(any(),any())).thenThrow(new IllegalStateException());

        // Call under test
        worker.run(mockCallback, new com.amazonaws.services.sqs.model.Message(), record);
        verifyZeroInteractions(firehoseLogger);
    }
}
