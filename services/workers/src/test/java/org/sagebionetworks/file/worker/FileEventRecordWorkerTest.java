package org.sagebionetworks.file.worker;

import com.amazonaws.services.sqs.model.Message;

import org.junit.jupiter.api.BeforeEach;
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
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileEvent;
import org.sagebionetworks.repo.model.file.FileEventRecord;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    
    private String sessionId;
    
    @BeforeEach
    public void before() {
    	sessionId = UUID.randomUUID().toString();
    }

    @Test
    public void testRunForUpload() throws RecoverableMessageException, Exception {
        FileEventRecord expectedRecord = new FileEventRecord().setUserId(1L).setAssociateId("1").setFileHandleId("123").setProjectId(23L)
                .setAssociateType(FileHandleAssociateType.FileEntity).setSessionId(sessionId);
        StatisticsFileEventRecord expectedStatisticsRecord = new StatisticsFileEventRecord(STACK, INSTANCE,
                Date.from(Instant.now()).getTime(), expectedRecord);

        FileEvent event = FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 1L, "123",
                "1", FileHandleAssociateType.FileEntity, STACK, INSTANCE).setSessionId(sessionId);

        when(projectResolver.resolveProject(any(), any())).thenReturn(Optional.of(23L));

        // Call under test
        worker.run(progressCallback, message, event);

        verify(firehoseLogger, times(2)).logBatch(streamNameCaptor.capture(), fileRecordCaptor.capture());
        assertEquals(List.of("fileUploadRecords", "fileUploads"), streamNameCaptor.getAllValues());
        KinesisJsonEntityRecord kinesisJsonEntityRecord = (KinesisJsonEntityRecord) fileRecordCaptor.getAllValues().get(0).get(0);
        FileEventRecord actualRecord = (FileEventRecord) kinesisJsonEntityRecord.getPayload();
        assertEquals(expectedRecord, actualRecord);
        StatisticsFileEventRecord statisticsFileEventRecord = (StatisticsFileEventRecord) fileRecordCaptor.getAllValues().get(1).get(0);
        expectedStatisticsRecord.withTimestamp(statisticsFileEventRecord.getTimestamp());
        assertEquals(expectedStatisticsRecord, statisticsFileEventRecord);
    }

    @Test
    public void testRunForDownload() throws RecoverableMessageException, Exception {
        FileEventRecord expectedRecord = new FileEventRecord().setUserId(1L).setAssociateId("1").setFileHandleId("123").setProjectId(23L)
                .setAssociateType(FileHandleAssociateType.FileEntity).setSessionId(sessionId);
        StatisticsFileEventRecord expectedStatisticsRecord = new StatisticsFileEventRecord(STACK, INSTANCE,
                Date.from(Instant.now()).getTime(), expectedRecord);

        FileEvent event = FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 1L, "123",
                "1", FileHandleAssociateType.FileEntity, STACK, INSTANCE).setSessionId(sessionId);

        when(projectResolver.resolveProject(any(), any())).thenReturn(Optional.of(23L));
        // Call under test
        worker.run(progressCallback, message, event);

        verify(firehoseLogger, times(2)).logBatch(streamNameCaptor.capture(), fileRecordCaptor.capture());
        assertEquals(List.of("fileDownloadRecords", "fileDownloads"), streamNameCaptor.getAllValues());
        KinesisJsonEntityRecord kinesisJsonEntityRecord = (KinesisJsonEntityRecord) fileRecordCaptor.getAllValues().get(0).get(0);
        FileEventRecord actualRecord = (FileEventRecord) kinesisJsonEntityRecord.getPayload();
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
                .setAssociateType(FileHandleAssociateType.FileEntity).setSessionId(sessionId);

        // call under test
        String message = assertThrows(IllegalStateException.class, () -> {
            worker.run(progressCallback, this.message, event);
        }).getMessage();

        assertEquals(message, "Unsupported object type: expected " + ObjectType.FILE_EVENT.name() + ", got " + event.getObjectType());
    }

    @Test
    public void testRunWithNullProjectId() throws Exception {
        FileEventRecord expectedRecord = new FileEventRecord().setUserId(1L).setAssociateId("1").setFileHandleId("123").setProjectId(null)
                .setAssociateType(FileHandleAssociateType.WikiAttachment).setSessionId(sessionId);
        StatisticsFileEventRecord expectedStatisticsRecord = new StatisticsFileEventRecord(STACK, INSTANCE,
                Date.from(Instant.now()).getTime(), expectedRecord);

        FileEvent event = FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 1L, "123",
                "1", FileHandleAssociateType.WikiAttachment, STACK, INSTANCE).setSessionId(sessionId);

        when(projectResolver.resolveProject(any(), any())).thenReturn(Optional.empty());
        // Call under test
        worker.run(progressCallback, message, event);

        verify(firehoseLogger, times(2)).logBatch(streamNameCaptor.capture(), fileRecordCaptor.capture());
        assertEquals(List.of("fileDownloadRecords", "fileDownloads"), streamNameCaptor.getAllValues());
        KinesisJsonEntityRecord kinesisJsonEntityRecord = (KinesisJsonEntityRecord) fileRecordCaptor.getAllValues().get(0).get(0);
        FileEventRecord actualRecord = (FileEventRecord) kinesisJsonEntityRecord.getPayload();
        assertEquals(expectedRecord, actualRecord);
        StatisticsFileEventRecord statisticsFileEventRecord = (StatisticsFileEventRecord) fileRecordCaptor.getAllValues().get(1).get(0);
        assertNotNull(statisticsFileEventRecord.getTimestamp());
        expectedStatisticsRecord.withTimestamp(statisticsFileEventRecord.getTimestamp());
        assertEquals(expectedStatisticsRecord, statisticsFileEventRecord);
    }
    
    @Test
    public void testRunWithNullSessionId() throws Exception {
        FileEventRecord expectedRecord = new FileEventRecord().setUserId(1L).setAssociateId("1").setFileHandleId("123").setProjectId(456L)
                .setAssociateType(FileHandleAssociateType.WikiAttachment).setSessionId(null);
        StatisticsFileEventRecord expectedStatisticsRecord = new StatisticsFileEventRecord(STACK, INSTANCE,
                Date.from(Instant.now()).getTime(), expectedRecord);

        FileEvent event = FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 1L, "123",
                "1", FileHandleAssociateType.WikiAttachment, STACK, INSTANCE).setSessionId(null);

        when(projectResolver.resolveProject(any(), any())).thenReturn(Optional.of(456L));
        // Call under test
        worker.run(progressCallback, message, event);

        verify(firehoseLogger, times(2)).logBatch(streamNameCaptor.capture(), fileRecordCaptor.capture());
        assertEquals(List.of("fileDownloadRecords", "fileDownloads"), streamNameCaptor.getAllValues());
        KinesisJsonEntityRecord kinesisJsonEntityRecord = (KinesisJsonEntityRecord) fileRecordCaptor.getAllValues().get(0).get(0);
        FileEventRecord actualRecord = (FileEventRecord) kinesisJsonEntityRecord.getPayload();
        assertEquals(expectedRecord, actualRecord);
        StatisticsFileEventRecord statisticsFileEventRecord = (StatisticsFileEventRecord) fileRecordCaptor.getAllValues().get(1).get(0);
        assertNotNull(statisticsFileEventRecord.getTimestamp());
        expectedStatisticsRecord.withTimestamp(statisticsFileEventRecord.getTimestamp());
        assertEquals(expectedStatisticsRecord, statisticsFileEventRecord);
    }
}
