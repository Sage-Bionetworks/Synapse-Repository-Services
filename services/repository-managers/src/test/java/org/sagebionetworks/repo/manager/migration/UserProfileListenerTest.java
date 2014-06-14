package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;

public class UserProfileListenerTest {
	private PrincipalAliasDAO principalAliasDAO;
	
	private UserProfileDAO userProfileDAO;
	
	private UserProfileListener userProfileListener;
	
	private static final Long PRINCIPAL_ID = 101L;
	private static final String userName = "foo";

	private List<DBOUserProfile> delta;
	
	@Before
	public void before() throws Exception {
		principalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		userProfileDAO = Mockito.mock(UserProfileDAO.class);
		userProfileListener = new UserProfileListener(principalAliasDAO, userProfileDAO);

		DBOUserProfile dbo = new DBOUserProfile();
		dbo.setOwnerId(PRINCIPAL_ID);
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(PRINCIPAL_ID.toString());
		userProfile.setUserName(userName);
		dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(userProfile));
		delta = Collections.singletonList(dbo);
	}
	
	@Test
	public void testAfterCreateOrUpdateNoPrincipalAlias() throws Exception {
		// alias does not exist
		when(principalAliasDAO.listPrincipalAliases(PRINCIPAL_ID, AliasType.USER_NAME)).
			thenReturn(new ArrayList<PrincipalAlias>());
		// call the listener
		userProfileListener.afterCreateOrUpdate(MigrationType.USER_PROFILE, delta);
		// check that alias was created
		PrincipalAlias expectedAlias = new PrincipalAlias();
		expectedAlias.setAlias(userName);
		expectedAlias.setPrincipalId(PRINCIPAL_ID);
		expectedAlias.setType(AliasType.USER_NAME);
		verify(principalAliasDAO).bindAliasToPrincipal(eq(expectedAlias));
		// check that user name was scrubbed from profile
		ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
		verify(userProfileDAO).update(profileCaptor.capture());
		assertNull(profileCaptor.getValue().getUserName());
	}

	@Test
	public void testAfterCreateOrUpdateHasAlias() throws Exception {
		// alias already exists
		PrincipalAlias expectedAlias = new PrincipalAlias();
		expectedAlias.setAlias(userName);
		expectedAlias.setPrincipalId(PRINCIPAL_ID);
		expectedAlias.setType(AliasType.USER_NAME);
		when(principalAliasDAO.listPrincipalAliases(PRINCIPAL_ID, AliasType.USER_NAME)).
			thenReturn(Collections.singletonList(expectedAlias));
		// call the listener
		userProfileListener.afterCreateOrUpdate(MigrationType.USER_PROFILE, delta);
		// in this case we DON'T set the alias
		verify(principalAliasDAO, never()).bindAliasToPrincipal(eq(expectedAlias));
		// check that user name was scrubbed from profile
		ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
		verify(userProfileDAO).update(profileCaptor.capture());
		assertNull(profileCaptor.getValue().getUserName());
	}

}
