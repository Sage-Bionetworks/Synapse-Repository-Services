package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;


@ExtendWith(MockitoExtension.class)
public class ProjectSettingObjectRecordWriterTest {
	@Mock
	private ProjectSettingsDAO mockProjectSettingsDao;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private AwsKinesisFirehoseLogger mockLogger;
	
	@Captor
	private ArgumentCaptor<List<KinesisObjectSnapshotRecord<ProjectSetting>>> recordCaptor;
	
	@InjectMocks
	private ProjectSettingObjectRecordWriter writer;
	
	private ProjectSetting projectSetting;
	private final Long projectSettingId = 123L;
	private final Long projectId = 456L;

	@BeforeEach
	public void before() {

		projectSetting = new UploadDestinationListSetting();
		projectSetting.setEtag("etag");
		projectSetting.setId(projectSettingId.toString());
		projectSetting.setProjectId(projectId.toString());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
	}

	@Test
	public void deleteChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, projectSettingId.toString(), ObjectType.PROJECT_SETTING, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockLogger, never()).logBatch(any(), any());
	}

	@Test
	public void invalidObjectType() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, projectSettingId.toString(), ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		assertThrows(IllegalArgumentException.class, () -> {			
			writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		});
		
		verify(mockLogger, never()).logBatch(any(), any());
	}

	@Test
	public void validChangeMessage() throws IOException {
		Mockito.when(mockProjectSettingsDao.get(projectSettingId.toString())).thenReturn(projectSetting);

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, projectSettingId.toString(),
				ObjectType.PROJECT_SETTING, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		// call under test
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage, changeMessage));
		verify(mockProjectSettingsDao, times(2)).get(eq(projectSettingId.toString()));

		KinesisObjectSnapshotRecord<ProjectSetting> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage,
				projectSetting);
		verify(mockLogger).logBatch(eq(ProjectSettingObjectRecordWriter.STREAM_NAME), recordCaptor.capture());
		KinesisObjectSnapshotRecord<ProjectSetting> captured = recordCaptor.getValue().get(0);
		assertEquals(expectedRecord.withChangeTimestamp(captured.getChangeTimestamp())
				.withSnapshotTimestamp(captured.getSnapshotTimestamp()), captured);
	}
	
	@Test
	public void buildWithNotFound() throws IOException {
		when(mockProjectSettingsDao.get(projectSettingId.toString())).thenThrow(new NotFoundException("not"));

		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, projectSettingId.toString(),
				ObjectType.PROJECT_SETTING, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		// call under test
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage, changeMessage));
		
		verify(mockProjectSettingsDao, times(2)).get(eq(projectSettingId.toString()));
		verify(mockLogger, never()).logBatch(any(), any());
	}
}
