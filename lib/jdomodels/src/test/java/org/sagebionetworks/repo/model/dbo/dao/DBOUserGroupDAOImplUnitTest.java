package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class DBOUserGroupDAOImplUnitTest {

	@Mock
	private DBOBasicDao mockBasicDAO;
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	@Mock
	private JdbcTemplate mockJdbcTemplate;

	private DBOUserGroupDAOImpl userGroupDAO;
	private UserGroup ug;
	private Long id = 1L;

	@BeforeEach
	public void setup() {
		List<BootstrapPrincipal> bootstrapPrincipals = Collections.emptyList();
		userGroupDAO = new DBOUserGroupDAOImpl(mockBasicDAO, mockIdGenerator, mockTransactionalMessenger,
				mockNamedJdbcTemplate, mockJdbcTemplate, bootstrapPrincipals);

		ug = new UserGroup();
		ug.setCreationDate(new Date());
		ug.setEtag("etag");
		ug.setId(id.toString());
		ug.setIsIndividual(false);
		ug.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
	}

	private void setupCreateMocks() {
		Mockito.when(mockIdGenerator.generateNewId(IdType.PRINCIPAL_ID)).thenReturn(id);
		doAnswer(new Answer<DBOUserGroup>(){
			@Override
			public DBOUserGroup answer(InvocationOnMock invocation) throws Throwable {
				return invocation.getArgument(0, DBOUserGroup.class);
			}}).when(mockBasicDAO).createNew(any(DBOUserGroup.class));
	}

	private void setupUpdateMocks() {
		// update() only calls basicDao.update(dbo) without checking the return value
		when(mockBasicDAO.update(any(DatabaseObject.class))).thenReturn(true);
	}

	@Test
	public void createSendMessageTest() {
		setupCreateMocks();
		userGroupDAO.create(ug);
		ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectType> objectTypeCapture = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<ChangeType> typeCapture = ArgumentCaptor.forClass(ChangeType.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(idCapture.capture(), objectTypeCapture.capture(), typeCapture.capture());
		assertEquals(id.toString(), idCapture.getValue());
		assertEquals(ObjectType.PRINCIPAL, objectTypeCapture.getValue());
		assertEquals(ChangeType.CREATE, typeCapture.getValue());
	}
	
	@Test
	public void updateSendMessageTest() {
		setupUpdateMocks();
		userGroupDAO.update(ug);
		ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectType> objectTypeCapture = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<ChangeType> typeCapture = ArgumentCaptor.forClass(ChangeType.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(idCapture.capture(), objectTypeCapture.capture(), typeCapture.capture());
		assertEquals(id.toString(), idCapture.getValue());
		assertEquals(ObjectType.PRINCIPAL, objectTypeCapture.getValue());
		assertEquals(ChangeType.UPDATE, typeCapture.getValue());
	}

}
