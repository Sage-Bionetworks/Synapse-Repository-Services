package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;

import com.google.common.collect.ImmutableSet;

public class PermissionsManagerUtilsTest {
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private UserInfo otherUserInfo;
	private static Long ownerId;

	@BeforeEach
	public void setUp(){
		ownerId = 1234L;
		userInfo = new UserInfo(false, ownerId);
		otherUserInfo = new UserInfo(false, 56789L);
		adminUserInfo = new UserInfo(true, 1L);
		otherUserInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
	}

	@Test
	public void testValidateACLContent() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(userInfo.getId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		userRA.setAccessType(ats);

		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);

		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);

		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
	}

	@Test
	public void testValidateACLContent_UserMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");

		assertThrows(InvalidModelException.class, ()-> {
			// Should fail, since user is not included with proper permissions in ACL
			PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, ownerId);
		});
	}


	@Test
	public void testValidateACLContent_AdminMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");

		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, adminUserInfo, ownerId);
	}

	@Test
	public void testValidateACLContent_OwnerMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");

		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
	}

	@Test
	public void testValidateACLContent_UserInsufficientPermissions() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(userInfo.getId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.READ);
		userRA.setAccessType(ats);

		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);

		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);

		assertThrows(InvalidModelException.class, ()-> {
			// Should fail since user does not have permission editing rights in ACL
			PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, ownerId);
		});
	}

	@Test
	public void testValidateACLContent_indirectMembership() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		// 'other user' should be a member of 'authenticated users'
		Long groupId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();
		assertTrue(otherUserInfo.getGroups().contains(groupId));
		// giving 'authenticated users' change_permissions access should fulfill the requirement
		// that the editor of the ACL does not remove their own access
		userRA.setPrincipalId(groupId);
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		userRA.setAccessType(ats);

		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);

		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);

		PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, ownerId);

	}

	@Test
	public void testValidateACLContentAnonDownload() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.DOWNLOAD);
		userRA.setAccessType(ats);

		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);

		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);

		assertThrows(InvalidModelException.class, ()-> {
			PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		});
	}

	/*
	 * PLFM-3632s
	 */
	@Test
	public void testValidateACLContentNonCertifiedUserMakeACLPublic() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.DOWNLOAD);
		userRA.setAccessType(ats);
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);
		assertThrows(UserCertificationRequiredException.class, ()-> {
			// userInfo is not certified
			PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		});
	}

	@Test
	public void testValidateACLContentCertifiedUserMakeACLPublic() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.DOWNLOAD);
		userRA.setAccessType(ats);
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);
		// certify userInfo
		userInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
	}
	
	@Test
	public void testValidateACLContentWithAnonymousUser() {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		Set<ACCESS_TYPE> ats = ImmutableSet.of(ACCESS_TYPE.READ);
		userRA.setAccessType(ats);

		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ImmutableSet.of(userRA));

		InvalidModelException ex = assertThrows(InvalidModelException.class, () -> {
			// Call under test
			PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		});

		assertEquals("Cannot assign permissions to anonymous. To share resources with anonymous users, use the PUBLIC group id (" + BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId() + ")", ex.getMessage());
	}

	@Test
	public void testValidateACLContentWithREADOnPublicGroup() {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		Set<ACCESS_TYPE> ats = ImmutableSet.of(ACCESS_TYPE.READ);
		userRA.setAccessType(ats);

		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ImmutableSet.of(userRA));
		
		// Call under test
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);

	}
	
	@Test
	public void testValidateACLContentWithPublicGroupAndDifferentThanREAD() {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		
		Set<ACCESS_TYPE> ats = Arrays.asList(ACCESS_TYPE.values()).stream().filter( type -> !ACCESS_TYPE.READ.equals(type)).collect(Collectors.toSet());
		
		userRA.setAccessType(ats);

		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ImmutableSet.of(userRA));
		
		InvalidModelException ex = assertThrows(InvalidModelException.class, () -> {
			// Call under test
			PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		});
		
		assertEquals("Only READ permissions can be assigned to the public group", ex.getMessage());
		
	}

}