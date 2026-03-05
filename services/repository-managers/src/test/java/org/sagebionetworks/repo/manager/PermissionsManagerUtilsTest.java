package org.sagebionetworks.repo.manager;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_REALM_ID;

public class PermissionsManagerUtilsTest {
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private UserInfo otherUserInfo;
	private static Long ownerId;
	private Set<String> realmIds;

	@BeforeEach
	public void setUp(){
		ownerId = 1234L;
		userInfo = UserInfoTestHelper.createUserInfo(false, ownerId);

		otherUserInfo = UserInfoTestHelper.createUserInfo(false, 56789L);
		otherUserInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());

		adminUserInfo = UserInfoTestHelper.createUserInfo(true, 1L);
		realmIds = Set.of(DEFAULT_REALM_ID);
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
		PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, ownerId);
	}

	@Test
	public void testValidateACLContent_UserMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");

		assertThrows(InvalidModelException.class, ()-> {
			// Should fail, since user is not included with proper permissions in ACL
			PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, realmIds, ObjectType.ENTITY, ownerId);
		});
	}


	@Test
	public void testValidateACLContent_AdminMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");

		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, adminUserInfo, realmIds, ObjectType.ENTITY, ownerId);
	}

	@Test
	public void testValidateACLContent_OwnerMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");

		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, ownerId);
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
			PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, realmIds, ObjectType.EVALUATION, ownerId);
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

		PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, realmIds, ObjectType.ORGANIZATION, ownerId);

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
			PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, ownerId);
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
			PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, ownerId);
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
		userInfo.setCertified(true);
		PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, ownerId);
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
			PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, ownerId);
		});

		assertEquals("Cannot assign permissions to anonymous. To share resources with anonymous users, use the PUBLIC group id (" + AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId() + ")", ex.getMessage());
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
		PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds,ObjectType.ENTITY, ownerId);

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
			PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds,ObjectType.ENTITY, ownerId);
		});
		
		assertEquals("Only READ permissions can be assigned to the public group", ex.getMessage());
		
	}

	@Test
	public void testValidateACLContentForDifferentRealACLPrincipal() {
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
		Set<String> realmIdSet = Set.of(DEFAULT_REALM_ID, "1"); // add a different realm to the set of realms

		// Should not throw any exceptions
		InvalidModelException ex = assertThrows(InvalidModelException.class, () -> {
			// Call under test
			PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIdSet, ObjectType.ENTITY, ownerId);
		});

		assertEquals("All principals in the ACL must be from the same realm.", ex.getMessage());
	}

	@Test
	public void testValidateACLContentForACLPrincipalRealmIsDifferentThenCaller() {
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
		Set<String> realmIdSet = Set.of( "1");

		// Should not throw any exceptions caller is in default realm and ACl principal in different realm
		InvalidModelException ex = assertThrows(InvalidModelException.class, () -> {
			// Call under test
			PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIdSet, ObjectType.ENTITY, ownerId);
		});

		assertEquals("All principals in the ACL must be from the same realm as the caller principal.", ex.getMessage());

		// Admin caller can not change ACL principal in different realm
		InvalidModelException exception = assertThrows(InvalidModelException.class, () -> {
			PermissionsManagerUtils.validateACLContent(acl, adminUserInfo, realmIdSet, ObjectType.ENTITY, ownerId);
		});

		assertEquals("All principals in the ACL must be from the same realm as the caller principal.", exception.getMessage());
	}

	@Test
	public void testValidateACLContentForEmptyRealSet() {
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
		Set<String> realmIdSet = Collections.emptySet();

		// Call under test
		PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIdSet, ObjectType.ENTITY, ownerId);

	}

	@Test
	public void testValidateACLContentWithNullOwnerId() {
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

		// call under test. Null is passed a ownerId, which mean caller is not Owner, condition will fail and throw exception.
		InvalidModelException exception = assertThrows(InvalidModelException.class, () -> {
			PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, null);
		});

		assertEquals("Caller is trying to revoke their own ACL editing permissions.", exception.getMessage());

		// caller is owner, so no exception should be thrown.
		PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, ownerId);
	}

	@Test
	public void testValidateACLContentNoValidationAccessRequirement() {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, userInfo, realmIds, ObjectType.ENTITY, ownerId);
	}

	@ParameterizedTest
	@EnumSource(value = ObjectType.class, names = {"ENTITY", "TEAM", "EVALUATION", "FORM_GROUP", "OAUTH_CLIENT", "ORGANIZATION", "PORTAL"})
	public void testValidateACLContentForAllObjectTypes(ObjectType objectType) {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(otherUserInfo.getId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		if (objectType.equals(ObjectType.TEAM)) {
			ats.add(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		} else {
			ats.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		}
		userRA.setAccessType(ats);

		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);

		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);

		PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, realmIds, objectType, ownerId);
	}

}