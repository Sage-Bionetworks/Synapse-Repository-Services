package org.sagebionetworks.repo.manager.backup.migration;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;

public class NodeOwnerMigratorImplTest {

	@Test
	public void testmigrateOneStep() throws Exception {
		UserGroupDAO ugDAO = Mockito.mock(UserGroupDAO.class);
		UserGroup admin = new UserGroup();
		admin.setId("100");
		admin.setName("admin@foo.com");
		admin.setIndividual(true);
		UserGroup nonAdmin = new UserGroup();
		nonAdmin.setId("101");
		nonAdmin.setName("nonAdmin@foo.com");
		nonAdmin.setIndividual(true);
		Collection<UserGroup> principals = Arrays.asList(new UserGroup[]{nonAdmin, admin});
		when(ugDAO.getAll(true)).thenReturn(principals);
		final Map<String,UserGroup> adminMap = new HashMap<String,UserGroup>();  adminMap.put(admin.getName(), admin);
		when(ugDAO.getGroupsByNames(Arrays.asList(new String[]{admin.getName()}))).thenReturn(adminMap);
		final Map<String,UserGroup> nonAdminMap = new HashMap<String,UserGroup>();  nonAdminMap.put(nonAdmin.getName(), nonAdmin);
		when(ugDAO.getGroupsByNames(Arrays.asList(new String[]{nonAdmin.getName()}))).thenReturn(nonAdminMap);
		
		UserManager um = Mockito.mock(UserManager.class);
		UserInfo adminUserInfo = new UserInfo(/*isAdmin*/true);
		when(um.getUserInfo(admin.getName())).thenReturn(adminUserInfo);
		UserInfo nonAdminUserInfo = new UserInfo(/*isAdmin*/false);
		when(um.getUserInfo(nonAdmin.getName())).thenReturn(nonAdminUserInfo);
		NodeOwnerMigratorImpl nom = new NodeOwnerMigratorImpl(ugDAO, um);
		NodeRevisionBackup toMigrate = new NodeRevisionBackup();
		
		// if 'modifiedBy' is null or non found, then an admin's ID is used
		toMigrate.setModifiedBy(null);
		EntityType entityType = EntityType.values()[0];
		EntityType et = nom.migrateOneStep(toMigrate, entityType);
		assertEquals(entityType, et);
		assertEquals(admin.getId(), toMigrate.getModifiedByPrincipalId().toString());
		
		// if a known user name is given, then that user's principal ID is used
		toMigrate.setModifiedBy(nonAdmin.getName());
		toMigrate.setModifiedByPrincipalId(null);
		et = nom.migrateOneStep(toMigrate, entityType);
		assertEquals(entityType, et);
		assertEquals(nonAdmin.getId(), toMigrate.getModifiedByPrincipalId().toString());
		
	}

}
