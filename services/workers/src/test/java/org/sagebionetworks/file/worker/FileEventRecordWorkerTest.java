package org.sagebionetworks.file.worker;

import com.amazonaws.services.sqs.model.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AbstractAwsKinesisLogRecord;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.audit.KinesisJsonEntityRecord;
import org.sagebionetworks.repo.manager.file.FileEventUtils;
import org.sagebionetworks.repo.manager.statistics.ProjectResolver;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEventRecord;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileEvent;
import org.sagebionetworks.repo.model.file.FileEventRecord;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class FileEventRecordWorkerTest {
    private static final String STACK = "stack";
    private static final String INSTANCE = "instance";

    @InjectMocks
    FileEventRecordWorker worker;
    @Mock
    private ProgressCallback progressCallback;
    @Mock
    private ProjectResolver projectResolver;
    @Mock
    private AwsKinesisFirehoseLogger firehoseLogger;
    @Mock
    private Message message;
    @Mock
    private StackConfiguration configuration;
    @Captor
    private ArgumentCaptor<List<AbstractAwsKinesisLogRecord>> fileRecordCaptor;
    @Captor
    private ArgumentCaptor<String> streamNameCaptor;

    @Test
    public void testRunForUpload() throws RecoverableMessageException, Exception {
        FileEventRecord expectedRecord = new FileEventRecord().setUserId(1L).setAssociateId("1").setFileHandleId("123").setProjectId(23L)
                .setAssociateType(FileHandleAssociateType.FileEntity).setStack("test").setInstance("test");
        StatisticsFileEventRecord expectedStatisticsRecord = new StatisticsFileEventRecord("test", "test").withUserId(1L)
                .withAssociateId("1").withFileHandleId("123").withProjectId(23L)
                .withAssociateType(FileHandleAssociateType.FileEntity).withStack("test").withInstance("test");

        FileEvent event = FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 1L, "123",
                "1", FileHandleAssociateType.FileEntity, STACK, INSTANCE);

        when(projectResolver.resolveProject(any(), any())).thenReturn(23L);
        when(configuration.getStack()).thenReturn(STACK);
        when(configuration.getStackInstance()).thenReturn(INSTANCE);

        // Call under test
        worker.run(progressCallback, message, event);

        verify(firehoseLogger, times(2)).logBatch(streamNameCaptor.capture(), fileRecordCaptor.capture());
        assertEquals(List.of("fileUploadRecords", "fileUploads"), streamNameCaptor.getAllValues());
        KinesisJsonEntityRecord kinesisJsonEntityRecord = (KinesisJsonEntityRecord) fileRecordCaptor.getAllValues().get(0).get(0);
        FileEventRecord actualRecord = (FileEventRecord) kinesisJsonEntityRecord.getPayload();
        expectedRecord.setTimestamp(actualRecord.getTimestamp());
        assertEquals(expectedRecord, actualRecord);
        StatisticsFileEventRecord statisticsFileEventRecord = (StatisticsFileEventRecord) fileRecordCaptor.getAllValues().get(1).get(0);
        expectedStatisticsRecord.withTimestamp(statisticsFileEventRecord.getTimestamp());
        assertEquals(expectedStatisticsRecord, statisticsFileEventRecord);
    }

    @Test
    public void testRunForDownload() throws RecoverableMessageException, Exception {
        FileEventRecord expectedRecord = new FileEventRecord().setUserId(1L).setAssociateId("1").setFileHandleId("123").setProjectId(23L)
                .setAssociateType(FileHandleAssociateType.FileEntity).setStack("test").setInstance("test");
        StatisticsFileEventRecord expectedStatisticsRecord = new StatisticsFileEventRecord("test", "test").withUserId(1L)
                .withAssociateId("1").withFileHandleId("123").withProjectId(23L)
                .withAssociateType(FileHandleAssociateType.FileEntity).withStack("test").withInstance("test");

        FileEvent event = FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 1L, "123",
                "1", FileHandleAssociateType.FileEntity, STACK, INSTANCE);

        when(projectResolver.resolveProject(any(), any())).thenReturn(23L);
        when(configuration.getStack()).thenReturn(STACK);
        when(configuration.getStackInstance()).thenReturn(INSTANCE);
        // Call under test
        worker.run(progressCallback, message, event);

        verify(firehoseLogger, times(2)).logBatch(streamNameCaptor.capture(), fileRecordCaptor.capture());
        assertEquals(List.of("fileDownloadRecords", "fileDownloads"), streamNameCaptor.getAllValues());
        KinesisJsonEntityRecord kinesisJsonEntityRecord = (KinesisJsonEntityRecord) fileRecordCaptor.getAllValues().get(0).get(0);
        FileEventRecord actualRecord = (FileEventRecord) kinesisJsonEntityRecord.getPayload();
        assertNotNull(actualRecord.getTimestamp());
        expectedRecord.setTimestamp(actualRecord.getTimestamp());
        assertEquals(expectedRecord, actualRecord);
        StatisticsFileEventRecord statisticsFileEventRecord = (StatisticsFileEventRecord) fileRecordCaptor.getAllValues().get(1).get(0);
        assertNotNull(statisticsFileEventRecord.getTimestamp());
        expectedStatisticsRecord.withTimestamp(statisticsFileEventRecord.getTimestamp());
        assertEquals(expectedStatisticsRecord, statisticsFileEventRecord);
    }

    @Test
    public void testRunWithWrongObjectType() throws RecoverableMessageException, Exception {
        FileEvent event = new FileEvent().setObjectType(ObjectType.TABLE_STATUS_EVENT)
                .setObjectId("123").setTimestamp(Date.from(Instant.now()))
                .setUserId(1L).setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setFileHandleId("123").setAssociateId("1")
                .setAssociateType(FileHandleAssociateType.FileEntity);

        // call under test
        String message = assertThrows(IllegalStateException.class, () -> {
            worker.run(progressCallback, this.message, event);
        }).getMessage();

        assertEquals(message, "Unsupported object type: expected " + ObjectType.FILE_EVENT.name() + ", got " + event.getObjectType());
    }

    @Test
    public void testRunWithoutProjectId() throws RecoverableMessageException, Exception {
        FileEvent event = new FileEvent().setObjectType(ObjectType.FILE_EVENT)
                .setObjectId("123").setTimestamp(Date.from(Instant.now()))
                .setUserId(1L).setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setFileHandleId("123").setAssociateId("1")
                .setAssociateType(FileHandleAssociateType.FileEntity);

        when(projectResolver.resolveProject(any(), any())).thenThrow(new IllegalStateException());

        // Call under test
        worker.run(progressCallback, message, event);
        verifyZeroInteractions(firehoseLogger);
    }
}
