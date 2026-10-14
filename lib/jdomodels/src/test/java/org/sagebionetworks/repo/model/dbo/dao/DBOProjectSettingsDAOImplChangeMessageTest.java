package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.jdbc.core.JdbcTemplate;

@RunWith(MockitoJUnitRunner.class)
public class DBOProjectSettingsDAOImplChangeMessageTest {

	@Mock
	private DBOBasicDao mockBasicDao;
	@Mock
	private JdbcTemplate mockJdbcTemplate; // Injected by @InjectMocks but not used in these tests
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;

	@InjectMocks
	private DBOProjectSettingsDAOImpl projectSettingDao;
	private ProjectSetting projectSetting;
	private DBOProjectSetting dbo;
	private final Long projectSettingId = 123L;
	private final Long projectId = 456L;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void before() {
	
		projectSetting = new UploadDestinationListSetting();
		projectSetting.setEtag("etag");
		projectSetting.setId(projectSettingId.toString());
		projectSetting.setProjectId(projectId.toString());
		projectSetting.setSettingsType(ProjectSettingsType.upload);

		dbo = new DBOProjectSetting();
		try {
			dbo.setJson(EntityFactory.createJSONStringForEntity(projectSetting));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		dbo.setId(projectSettingId);
		dbo.setProjectId(projectId);
		dbo.setType(ProjectSettingsType.upload.name());
		dbo.setEtag("etag");

		Mockito.when(mockBasicDao.createNew(dbo)).thenReturn(dbo);

		Mockito.when(mockBasicDao.getObjectByPrimaryKey(
				(Class)Mockito.any(), (SinglePrimaryKeySqlParameterSource)Mockito.any())).thenReturn(Optional.of(dbo));
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
				Mockito.eq(ChangeType.UPDATE));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testDelete() {
		projectSettingDao.delete(projectSettingId.toString());
		Mockito.verify(mockBasicDao).deleteObjectByPrimaryKey(
				(Class)Mockito.any(), (SinglePrimaryKeySqlParameterSource)Mockito.any());
		Mockito.verify(mockTransactionalMessenger).sendDeleteMessageAfterCommit(
				Mockito.eq(projectSettingId.toString()),
				Mockito.eq(ObjectType.PROJECT_SETTING));
	}
}
