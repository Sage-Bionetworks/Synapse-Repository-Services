package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;

import com.amazonaws.services.sqs.model.Message;


@ExtendWith(MockitoExtension.class)
public class ProjectSettingObjectRecordWriterTest {
	@Mock
	private ProjectSettingsDAO mockProjectSettingsDao;
	@Mock
	private ObjectRecordDAO mockObjectRecordDao;
	@Mock
	private ProgressCallback mockCallback;
	
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
		verify(mockObjectRecordDao, never()).saveBatch(anyList(), anyString());
	}

	@Test
	public void invalidObjectType() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, projectSettingId.toString(), ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		assertThrows(IllegalArgumentException.class, () -> {			
			writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		});
	}

	@Test
	public void validChangeMessage() throws IOException {
		Mockito.when(mockProjectSettingsDao.get(projectSettingId.toString())).thenReturn(projectSetting);
		
		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, projectSettingId.toString(), ObjectType.PROJECT_SETTING, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(projectSetting, timestamp);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage, changeMessage));
		verify(mockProjectSettingsDao, times(2)).get(eq(projectSettingId.toString()));
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected, expected)), eq(expected.getJsonClassName()));
	}
}
