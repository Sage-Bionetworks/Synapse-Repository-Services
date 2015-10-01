package org.sagebionetworks.object.snapshot.worker.utils;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
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

public class ProjectSettingObjectRecordBuilderTest {
	private ProjectSettingsDAO mockProjectSettingsDao;
	private ProjectSettingObjectRecordBuilder builder;
	private ProjectSetting projectSetting;
	private final Long projectSettingId = 123L;
	private final Long projectId = 456L;

	@Before
	public void before() {
		mockProjectSettingsDao = Mockito.mock(ProjectSettingsDAO.class);
		builder = new ProjectSettingObjectRecordBuilder(mockProjectSettingsDao);

		projectSetting = new UploadDestinationListSetting();
		projectSetting.setEtag("etag");
		projectSetting.setId(projectSettingId.toString());
		projectSetting.setProjectId(projectId.toString());
		projectSetting.setSettingsType(ProjectSettingsType.upload);

		Mockito.when(mockProjectSettingsDao.get(projectSettingId.toString())).thenReturn(projectSetting);
	}

	@Test (expected=IllegalArgumentException.class)
	public void deleteChangeMessage() {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, projectSettingId.toString(), ObjectType.PROJECT_SETTING, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}

	@Test (expected=IllegalArgumentException.class)
	public void invalidObjectType() {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, projectSettingId.toString(), ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}

	@Test
	public void validChangeMessage() {
		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, projectSettingId.toString(), ObjectType.PROJECT_SETTING, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(projectSetting, timestamp);
		ObjectRecord actual = builder.build(changeMessage).get(0);
		Mockito.verify(mockProjectSettingsDao).get(Mockito.eq(projectSettingId.toString()));
		assertEquals(expected, actual);
	}
}
