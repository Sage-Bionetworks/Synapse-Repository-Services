package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;

import com.amazonaws.services.sqs.model.Message;

public class ProjectSettingObjectRecordWriterTest {
	private ProjectSettingsDAO mockProjectSettingsDao;
	private ObjectRecordDAO mockObjectRecordDao;
	private ProjectSettingObjectRecordWriter builder;
	private ProjectSetting projectSetting;
	private final Long projectSettingId = 123L;
	private final Long projectId = 456L;

	@Before
	public void before() {
		mockProjectSettingsDao = Mockito.mock(ProjectSettingsDAO.class);
		mockObjectRecordDao = Mockito.mock(ObjectRecordDAO.class);
		builder = new ProjectSettingObjectRecordWriter(mockProjectSettingsDao, mockObjectRecordDao);

		projectSetting = new UploadDestinationListSetting();
		projectSetting.setEtag("etag");
		projectSetting.setId(projectSettingId.toString());
		projectSetting.setProjectId(projectId.toString());
		projectSetting.setSettingsType(ProjectSettingsType.upload);

		Mockito.when(mockProjectSettingsDao.get(projectSettingId.toString())).thenReturn(projectSetting);
	}

	@Test (expected=IllegalArgumentException.class)
	public void deleteChangeMessage() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, projectSettingId.toString(), ObjectType.PROJECT_SETTING, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.buildAndWriteRecord(changeMessage);
	}

	@Test (expected=IllegalArgumentException.class)
	public void invalidObjectType() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, projectSettingId.toString(), ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.buildAndWriteRecord(changeMessage);
	}

	@Test
	public void validChangeMessage() throws IOException {
		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, projectSettingId.toString(), ObjectType.PROJECT_SETTING, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(projectSetting, timestamp);
		builder.buildAndWriteRecord(changeMessage);
		Mockito.verify(mockProjectSettingsDao).get(Mockito.eq(projectSettingId.toString()));
		Mockito.verify(mockObjectRecordDao).saveBatch(Mockito.eq(Arrays.asList(expected)), Mockito.eq(expected.getJsonClassName()));
	}
}
