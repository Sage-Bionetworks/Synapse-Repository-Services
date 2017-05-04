package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER;
import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.test.util.ReflectionTestUtils;
public class ACLMigrationListenerTest {
	
	@Mock
	private AccessControlListDAO aclDAO;
	
	private ACLMigrationListener listener;
	
	private List<DBOAccessControlList> delta;
	
	private static final Long OWNER_ID = 101L;
	
	private static final Set<ACCESS_TYPE> READ_ONLY = new HashSet<ACCESS_TYPE>(Arrays.asList(READ));
	private static final Set<ACCESS_TYPE> READ_DOWNLOAD = new HashSet<ACCESS_TYPE>(Arrays.asList(READ, DOWNLOAD));

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		listener = new ACLMigrationListener();

		ReflectionTestUtils.setField(listener, "aclDAO", aclDAO);
		
		delta = new ArrayList<DBOAccessControlList>();
		delta.add(newDbo(OWNER_ID, ObjectType.ENTITY));

	}
	
	private static DBOAccessControlList newDbo(long ownerId, ObjectType ownerType) {
		DBOAccessControlList result = new DBOAccessControlList();
		result.setOwnerId(ownerId);
		result.setOwnerType(ownerType.name());
		return result;
	}
	
	private static AccessControlList newDto(String id) {
		AccessControlList result = new AccessControlList();
		result.setId(id);
		result.setResourceAccess(new HashSet<ResourceAccess>());
		return result;
	}
	
	private static AccessControlList add(AccessControlList acl, long principalId, Set<ACCESS_TYPE> permissions) {
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		ra.setAccessType(permissions);
		acl.getResourceAccess().add(ra);
		return acl;
	}
	
	/*
	 * returns the permissions for the given principal in the given acl.  Also validates
	 * that the given principal only occurs once in the ACL.
	 */
	private static Set<ACCESS_TYPE> permissions(AccessControlList acl, long principalId) {
		Set<ACCESS_TYPE> result = null;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra.getPrincipalId().equals(principalId)) {
				if (result==null) {
					result = ra.getAccessType();
				} else {
					throw new RuntimeException("acl has two entries for principal "+principalId);
				}
			}
		}
		return result;
	}
	
	@Test
	public void testMigrationListenerAddDownloadToRead() {
		long principalId = 999L;
		AccessControlList dto = add(newDto(OWNER_ID.toString()), principalId, READ_ONLY);
		when(aclDAO.get(OWNER_ID.toString(), ObjectType.ENTITY)).thenReturn(dto);
		
		// method under test
		listener.afterCreateOrUpdate(MigrationType.ACL, delta);
		
		verify(aclDAO).update(dto, ObjectType.ENTITY);
		
		assertEquals(READ_DOWNLOAD, permissions(dto, principalId));
	}
	
	@Test
	public void testMigrationListenerPublicRead() {
		AccessControlList dto = add(newDto(OWNER_ID.toString()), ANONYMOUS_USER.getPrincipalId(), READ_ONLY);
		when(aclDAO.get(OWNER_ID.toString(), ObjectType.ENTITY)).thenReturn(dto);
		
		// method under test
		listener.afterCreateOrUpdate(MigrationType.ACL, delta);
		
		verify(aclDAO).update(dto, ObjectType.ENTITY);
		
		// there are now two entries in the ACL.
		assertEquals(2, dto.getResourceAccess().size());
		// public still has read only
		assertEquals(READ_ONLY, permissions(dto, ANONYMOUS_USER.getPrincipalId()));
		// authenticated users has read + download
		assertEquals(READ_DOWNLOAD, permissions(dto, AUTHENTICATED_USERS_GROUP.getPrincipalId()));
	}
	
	@Test
	public void testMigrationListenerPublicReadAuthUserAlreadyInACL() {
		AccessControlList dto = add(newDto(OWNER_ID.toString()), ANONYMOUS_USER.getPrincipalId(), READ_ONLY);
		dto = add(dto, AUTHENTICATED_USERS_GROUP.getPrincipalId(), new HashSet<ACCESS_TYPE>(Arrays.asList(UPDATE)));
		when(aclDAO.get(OWNER_ID.toString(), ObjectType.ENTITY)).thenReturn(dto);
		
		// method under test
		listener.afterCreateOrUpdate(MigrationType.ACL, delta);
		
		verify(aclDAO).update(dto, ObjectType.ENTITY);
		
		// there are now two entries in the ACL.
		assertEquals(2, dto.getResourceAccess().size());
		// public still has read only
		assertEquals(READ_ONLY, permissions(dto, ANONYMOUS_USER.getPrincipalId()));
		// authenticated users has read + download
		assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(DOWNLOAD, READ, UPDATE)), 
				permissions(dto, AUTHENTICATED_USERS_GROUP.getPrincipalId()));
	}
	
	@Test
	public void testMigrationListenerRemovePublicDownload() {
		AccessControlList dto = add(newDto(OWNER_ID.toString()), ANONYMOUS_USER.getPrincipalId(), READ_DOWNLOAD);
		when(aclDAO.get(OWNER_ID.toString(), ObjectType.ENTITY)).thenReturn(dto);
		
		// method under test
		listener.afterCreateOrUpdate(MigrationType.ACL, delta);
		
		verify(aclDAO).update(dto, ObjectType.ENTITY);
		
		// there are now two entries in the ACL.
		assertEquals(2, dto.getResourceAccess().size());
		// public still has read only
		assertEquals(READ_ONLY, permissions(dto, ANONYMOUS_USER.getPrincipalId()));
		// authenticated users has read + download
		assertEquals(READ_DOWNLOAD, permissions(dto, AUTHENTICATED_USERS_GROUP.getPrincipalId()));
	}
	
	@Test
	public void testMigrationListenerNoChange() {
		long principalId = 999L;
		AccessControlList dto = add(newDto(OWNER_ID.toString()), principalId, new HashSet<ACCESS_TYPE>(Arrays.asList(UPDATE)));
		when(aclDAO.get(OWNER_ID.toString(), ObjectType.ENTITY)).thenReturn(dto);
		
		// method under test
		listener.afterCreateOrUpdate(MigrationType.ACL, delta);
		
		verify(aclDAO).get(OWNER_ID.toString(), ObjectType.ENTITY);
		verify(aclDAO, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}
	
	@Test
	public void testMigrationListenerNOTACL() {
		// method under test
		listener.afterCreateOrUpdate(MigrationType.NODE, delta);
		
		verify(aclDAO, never()).get(anyString(), any(ObjectType.class));
		verify(aclDAO, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}


	@Test
	public void testMigrationListenerNOTEntityACL() {
		delta.clear();
		delta.add(newDbo(OWNER_ID, ObjectType.TEAM));
		
		AccessControlList dto = add(newDto(OWNER_ID.toString()), AUTHENTICATED_USERS_GROUP.getPrincipalId(), READ_ONLY);
		when(aclDAO.get(OWNER_ID.toString(), ObjectType.ENTITY)).thenReturn(dto);

		// method under test
		listener.afterCreateOrUpdate(MigrationType.ACL, delta);
		
		verify(aclDAO, never()).get(anyString(), any(ObjectType.class));
		verify(aclDAO, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}

}
