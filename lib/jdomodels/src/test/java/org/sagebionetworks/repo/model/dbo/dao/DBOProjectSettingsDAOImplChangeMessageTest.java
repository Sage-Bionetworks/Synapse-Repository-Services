package org.sagebionetworks.repo.model.dbo.dao;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOProjectSetting;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;

public class DBOProjectSettingsDAOImplChangeMessageTest {

	private DBOBasicDao mockBasicDao;
	private IdGenerator mockIdGenerator;
	private TransactionalMessenger mockTransactionalMessenger;
	private DBOProjectSettingsDAOImpl projectSettingDao;
	private ProjectSetting projectSetting;
	private DBOProjectSetting dbo;
	private final Long projectSettingId = 123L;
	private final Long projectId = 456L;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void before() {
		mockBasicDao = Mockito.mock(DBOBasicDao.class);
		mockIdGenerator = Mockito.mock(IdGenerator.class);
		mockTransactionalMessenger = Mockito.mock(TransactionalMessenger.class);
		projectSettingDao = new DBOProjectSettingsDAOImpl(mockBasicDao, mockIdGenerator, mockTransactionalMessenger);
	
		projectSetting = new UploadDestinationListSetting();
		projectSetting.setEtag("etag");
		projectSetting.setId(projectSettingId.toString());
		projectSetting.setProjectId(projectId.toString());
		projectSetting.setSettingsType(ProjectSettingsType.upload);

		dbo = new DBOProjectSetting();
		dbo.setData(projectSetting);
		dbo.setId(projectSettingId);
		dbo.setProjectId(projectId);
		dbo.setType(ProjectSettingsType.upload);
		dbo.setEtag("etag");

		Mockito.when(mockIdGenerator.generateNewId(IdType.PROJECT_SETTINGS_ID)).thenReturn(projectSettingId);
		Mockito.when(mockBasicDao.createNew(dbo)).thenReturn(dbo);

		Mockito.when(mockBasicDao.getObjectByPrimaryKey(
				(Class)Mockito.any(), (SinglePrimaryKeySqlParameterSource)Mockito.any())).thenReturn(dbo);
		Mockito.when(mockBasicDao.update(dbo)).thenReturn(true);
		
	}

	@Test
	public void testCreate() {
		projectSettingDao.create(projectSetting);
		Mockito.verify(mockIdGenerator, Mockito.never()).generateNewId(IdType.PROJECT_SETTINGS_ID);
		Mockito.verify(mockBasicDao).createNew(Mockito.eq(dbo));
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(
				Mockito.eq(projectSettingId.toString()),
				Mockito.eq(ObjectType.PROJECT_SETTING),
				Mockito.anyString(),
				Mockito.eq(ChangeType.CREATE));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testUpdate() {
		projectSettingDao.update(projectSetting);
		Mockito.verify(mockBasicDao, Mockito.times(2)).getObjectByPrimaryKey((Class)Mockito.any(),
				(SinglePrimaryKeySqlParameterSource)Mockito.any());
		Mockito.verify(mockBasicDao).update(Mockito.eq(dbo));
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(
				Mockito.eq(projectSettingId.toString()),
				Mockito.eq(ObjectType.PROJECT_SETTING),
				Mockito.anyString(),
				Mockito.eq(ChangeType.UPDATE));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testDelete() {
		projectSettingDao.delete(projectSettingId.toString());
		Mockito.verify(mockBasicDao).deleteObjectByPrimaryKey(
				(Class)Mockito.any(), (SinglePrimaryKeySqlParameterSource)Mockito.any());
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(
				Mockito.eq(projectSettingId.toString()),
				Mockito.eq(ObjectType.PROJECT_SETTING),
				Mockito.eq(ChangeType.DELETE));
	}
}
