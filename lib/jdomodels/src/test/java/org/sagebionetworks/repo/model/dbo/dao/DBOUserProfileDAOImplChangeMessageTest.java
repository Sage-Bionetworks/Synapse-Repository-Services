package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

public class DBOUserProfileDAOImplChangeMessageTest {
	private DBOUserProfileDAOImpl dao;
	private DBOBasicDao mockBasicDao;
	private TransactionalMessenger mockTransactionalMessenger;
	private SimpleJdbcTemplate mockSimpleJdbcTemplate;
	private DBOUserProfile jdo;
	private UserProfile dto;

	@Before
	public void before() {
		mockTransactionalMessenger = Mockito.mock(TransactionalMessenger.class);
		mockBasicDao = Mockito.mock(DBOBasicDao.class);
		mockSimpleJdbcTemplate = Mockito.mock(SimpleJdbcTemplate.class);
		dao = new DBOUserProfileDAOImpl();
		ReflectionTestUtils.setField(dao, "transactionalMessenger", mockTransactionalMessenger);
		ReflectionTestUtils.setField(dao, "basicDao", mockBasicDao);
		ReflectionTestUtils.setField(dao, "simpleJdbcTemplate", mockSimpleJdbcTemplate);
		jdo = new DBOUserProfile();
		dto = new UserProfile();
		dto.setEtag("etag");
		dto.setOwnerId("123");
		UserProfileUtils.copyDtoToDbo(dto, jdo);
		Mockito.when(mockBasicDao.createNew(jdo)).thenReturn(jdo);
		Mockito.when(mockBasicDao.update(jdo)).thenReturn(true);
		Mockito.when(mockSimpleJdbcTemplate.queryForObject(Mockito.anyString(),
				(RowMapper) Mockito.any(), (SqlParameterSource) Mockito.any()))
				.thenReturn(jdo);
	}

	@Test
	public void testCreate() {
		dao.create(dto);
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(dto.getOwnerId(),
				ObjectType.PRINCIPAL, dto.getEtag(), ChangeType.CREATE);
	}

	@Test
	public void testUpdate() {
		dao.update(dto);
		ArgumentCaptor<String> ownerIdArgCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectType> objectTypeArgCaptor = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<ChangeType> changeTypeCaptor = ArgumentCaptor.forClass(ChangeType.class);
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(ownerIdArgCaptor.capture(),
				objectTypeArgCaptor.capture(), Mockito.anyString(), changeTypeCaptor.capture());
		assertEquals(ownerIdArgCaptor.getValue(), dto.getOwnerId());
		assertEquals(objectTypeArgCaptor.getValue(), ObjectType.PRINCIPAL);
		assertEquals(changeTypeCaptor.getValue(), ChangeType.UPDATE);
	}

}
