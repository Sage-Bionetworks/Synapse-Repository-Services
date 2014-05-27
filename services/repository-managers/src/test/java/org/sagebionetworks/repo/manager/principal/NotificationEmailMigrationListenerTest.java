package org.sagebionetworks.repo.manager.principal;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.principal.DBOPrincipalAlias;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class NotificationEmailMigrationListenerTest {

	private NotificationEmailMigrationListener neml;
	private NotificationEmailDAO mockNotificationEmailDAO;
	private List<DBOPrincipalAlias> delta;
	
	@Before
	public void setUp() throws Exception {
		neml = new NotificationEmailMigrationListener();
		mockNotificationEmailDAO = Mockito.mock(NotificationEmailDAO.class);
		ReflectionTestUtils.setField(neml, "notificationEmailDao", mockNotificationEmailDAO);
		DBOPrincipalAlias alias = new DBOPrincipalAlias();
		alias.setAliasDisplay("foo@bar.com");
		alias.setAliasType(AliasEnum.USER_EMAIL);
		alias.setPrincipalId(101L);
		alias.setId(876L);
		delta = Collections.singletonList(alias);
	}

	@Test
	public void testCreate() throws Exception {
		when(mockNotificationEmailDAO.getNotificationEmailForPrincipal(101L)).thenThrow(new NotFoundException());
		neml.afterCreateOrUpdate(MigrationType.PRINCIPAL_ALIAS, delta);
		verify(mockNotificationEmailDAO).getNotificationEmailForPrincipal(101L);
		verify(mockNotificationEmailDAO).create((PrincipalAlias)any());
	}

	@Test
	public void testSkip() throws Exception {
		when(mockNotificationEmailDAO.getNotificationEmailForPrincipal(101L)).thenReturn("foo@bar.com");
		neml.afterCreateOrUpdate(MigrationType.PRINCIPAL_ALIAS, delta);
		verify(mockNotificationEmailDAO).getNotificationEmailForPrincipal(101L);
		verify(mockNotificationEmailDAO, times(0)).create((PrincipalAlias)any());
	}

	@Test
	public void testNotEmail() throws Exception {
		DBOPrincipalAlias alias = new DBOPrincipalAlias();
		alias.setAliasDisplay("foobar");
		alias.setAliasType(AliasEnum.USER_NAME);
		alias.setPrincipalId(101L);
		alias.setId(876L);
		delta = Collections.singletonList(alias);
		when(mockNotificationEmailDAO.getNotificationEmailForPrincipal(101L)).thenThrow(new NotFoundException());
		neml.afterCreateOrUpdate(MigrationType.PRINCIPAL_ALIAS, delta);
		verify(mockNotificationEmailDAO, times(0)).getNotificationEmailForPrincipal(anyLong());
		verify(mockNotificationEmailDAO, times(0)).create((PrincipalAlias)any());
	}

}
