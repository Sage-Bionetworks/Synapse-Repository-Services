package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class DBOUserProfileDAOImplChangeMessageTest {
	private DBOUserProfileDAOImpl dao;
	@Mock
	private DBOBasicDao mockBasicDao;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	private DBOUserProfile jdo;
	private UserProfile dto;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		List<BootstrapPrincipal> bootstrapPrincipals = Collections.emptyList();
		dao = new DBOUserProfileDAOImpl(mockBasicDao, mockUserGroupDAO, mockTransactionalMessenger,
				mockNamedJdbcTemplate, mockJdbcTemplate, bootstrapPrincipals);
		jdo = new DBOUserProfile();
		dto = new UserProfile();
		dto.setEtag("etag");
		dto.setOwnerId("123");
		UserProfileUtils.copyDtoToDbo(dto, jdo);
		Mockito.when(mockBasicDao.createNew(jdo)).thenReturn(jdo);
		Mockito.when(mockBasicDao.update(jdo)).thenReturn(true);
		Mockito.when(mockNamedJdbcTemplate.queryForObject(Mockito.anyString(),
				(SqlParameterSource) Mockito.any(), (RowMapper) Mockito.any())).thenReturn(jdo);
	}

	@Test
	public void testCreate() {
		dao.create(dto);
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(dto.getOwnerId(),
				ObjectType.PRINCIPAL, ChangeType.CREATE);
	}

	@Test
	public void testUpdate() {
		dao.update(dto);
		ArgumentCaptor<String> ownerIdArgCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectType> objectTypeArgCaptor = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<ChangeType> changeTypeCaptor = ArgumentCaptor.forClass(ChangeType.class);
		Mockito.verify(mockTransactionalMessenger).sendMessageAfterCommit(ownerIdArgCaptor.capture(),
				objectTypeArgCaptor.capture(), changeTypeCaptor.capture());
		assertEquals(ownerIdArgCaptor.getValue(), dto.getOwnerId());
		assertEquals(objectTypeArgCaptor.getValue(), ObjectType.PRINCIPAL);
		assertEquals(changeTypeCaptor.getValue(), ChangeType.UPDATE);
	}

}
