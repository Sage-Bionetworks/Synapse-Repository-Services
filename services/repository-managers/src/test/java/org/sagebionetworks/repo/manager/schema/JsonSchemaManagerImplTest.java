package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;

@ExtendWith(MockitoExtension.class)
public class JsonSchemaManagerImplTest {

	@Mock
	OrganizationDao mockOrganizationDao;

	@Mock
	AccessControlListDAO mockAclDao;

	@Captor
	ArgumentCaptor<AccessControlList> aclCaptor;

	@InjectMocks
	JsonSchemaManagerImpl manager;

	UserInfo user;
	UserInfo anonymousUser;
	CreateOrganizationRequest request;

	Organization organization;

	AccessControlList acl;

	@BeforeEach
	public void before() {
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, 123L);

		anonymousUser = new UserInfo(isAdmin, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		request = new CreateOrganizationRequest();
		request.setOrganizationName("a.2.b.com");

		organization = new Organization();
		organization.setCreatedBy("" + user.getId());
		organization.setName("foo.bar");
		organization.setCreatedOn(new Date());
		organization.setId("4321");

		acl = AccessControlListUtil.createACL(organization.getId(), user, JsonSchemaManagerImpl.ADMIN_PERMISSIONS,
				new Date());
	}

	@Test
	public void testProcessAndValidateOrganizationName() {
		String inputName = " A.9.C.DEFG \n";
		String processedName = JsonSchemaManagerImpl.processAndValidateOrganizationName(inputName);
		assertEquals("a.9.c.defg", processedName);
	}

	@Test
	public void testProcessAndValidateOrganizationNameMaxLength() {
		String input = StringUtils.repeat("a", JsonSchemaManagerImpl.MAX_ORGANZIATION_NAME_CHARS);
		String processedName = JsonSchemaManagerImpl.processAndValidateOrganizationName(input);
		assertNotNull(processedName);
		assertEquals(JsonSchemaManagerImpl.MAX_ORGANZIATION_NAME_CHARS, processedName.length());
	}

	@Test
	public void testProcessAndValidateOrganizationNameOverMaxLength() {
		String input = StringUtils.repeat("a", JsonSchemaManagerImpl.MAX_ORGANZIATION_NAME_CHARS + 1);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(input);
		}).getMessage();
		assertEquals("Organization name must be 250 characters or less", message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameMinLength() {
		String input = StringUtils.repeat("a", JsonSchemaManagerImpl.MIN_ORGANZIATION_NAME_CHARS);
		String processedName = JsonSchemaManagerImpl.processAndValidateOrganizationName(input);
		assertNotNull(processedName);
		assertEquals(JsonSchemaManagerImpl.MIN_ORGANZIATION_NAME_CHARS, processedName.length());
	}

	@Test
	public void testProcessAndValidateOrganizationNameUnderMinLength() {
		String input = StringUtils.repeat("a", JsonSchemaManagerImpl.MIN_ORGANZIATION_NAME_CHARS - 1);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(input);
		}).getMessage();
		assertEquals("Organization name must be at least 6 characters", message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameSagebionetworks() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("sagebionetwork");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.SAGEBIONETWORKS_RESERVED_MESSAGE, message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameStartWithDigit() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("1abcdefg");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.BAD_ORGANIZATION_NAME_MESSAGE, message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameEndWithDigit() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("abcdefg9");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.BAD_ORGANIZATION_NAME_MESSAGE, message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameStartWithDot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(".abcdefg");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.BAD_ORGANIZATION_NAME_MESSAGE, message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameEndWithDot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("abcdefg.");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.BAD_ORGANIZATION_NAME_MESSAGE, message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameContainsInvalidChars() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("abc/defg");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.BAD_ORGANIZATION_NAME_MESSAGE, message);
	}

	@Test
	public void testCreateOrganziation() {
		when(mockOrganizationDao.createOrganization(request.getOrganizationName(), user.getId()))
				.thenReturn(organization);
		// call under test
		Organization returned = manager.createOrganziation(user, request);
		assertNotNull(returned);
		assertEquals(organization, returned);

		verify(mockOrganizationDao).createOrganization(request.getOrganizationName(), user.getId());

		verify(mockAclDao).create(aclCaptor.capture(), eq(ObjectType.ORGANIZATION));
		AccessControlList acl = aclCaptor.getValue();
		assertNotNull(acl);
		assertEquals(returned.getId(), acl.getId());
		AccessControlList expectedAcl = AccessControlListUtil.createACL(returned.getId(), user,
				JsonSchemaManagerImpl.ADMIN_PERMISSIONS, new Date());
		assertEquals(expectedAcl.getResourceAccess(), acl.getResourceAccess());
	}

	@Test
	public void testCreateOrganziationNameWithUpperCase() {
		request.setOrganizationName("ALLCAPS");
		when(mockOrganizationDao.createOrganization(anyString(), anyLong())).thenReturn(organization);
		// call under test
		Organization returned = manager.createOrganziation(user, request);
		assertNotNull(returned);
		assertEquals(organization, returned);

		verify(mockOrganizationDao).createOrganization("allcaps", user.getId());
	}

	@Test
	public void testCreateOrganziationNameWithWhiteSpace() {
		request.setOrganizationName(" needs.trimmed\n");
		when(mockOrganizationDao.createOrganization(anyString(), anyLong())).thenReturn(organization);
		// call under test
		Organization returned = manager.createOrganziation(user, request);
		assertNotNull(returned);
		assertEquals(organization, returned);

		verify(mockOrganizationDao).createOrganization("needs.trimmed", user.getId());
	}

	@Test
	public void testCreateOrganizationAnonymous() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createOrganziation(anonymousUser, request);
		});
	}

	@Test
	public void testCreateOrganizationNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrganziation(user, request);
		});
	}

	@Test
	public void testCreateOrganizationNullRequest() {
		request = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrganziation(user, request);
		});
	}

	@Test
	public void testCreateOrganizationNullName() {
		request.setOrganizationName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrganziation(user, request);
		});
	}

	@Test
	public void testCreateOrganizationBadName() {
		request.setOrganizationName("endsWithDot.");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrganziation(user, request);
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.BAD_ORGANIZATION_NAME_MESSAGE, message);
	}

	@Test
	public void testGetOrganizationAcl() {
		when(mockAclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAclDao.get(organization.getId(), ObjectType.ORGANIZATION)).thenReturn(acl);
		// call under test
		AccessControlList result = manager.getOrganizationAcl(user, organization.getId());
		assertEquals(acl, result);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.READ);
	}

	@Test
	public void testGetOrganizationAclNoRead() {
		when(mockAclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied("nope"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.getOrganizationAcl(user, organization.getId());
		});
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.READ);
	}

	@Test
	public void testGetOrganziationAclNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getOrganizationAcl(user, organization.getId());
		});
	}

	@Test
	public void testGetOrganziationAclNullId() {
		organization.setId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getOrganizationAcl(user, organization.getId());
		});
	}

	@Test
	public void testUpdateOrganizationAcl() {
		when(mockAclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CHANGE_PERMISSIONS))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAclDao.get(organization.getId(), ObjectType.ORGANIZATION)).thenReturn(acl);
		// call under test
		AccessControlList result = manager.updateOrganizationAcl(user, organization.getId(), acl);
		assertEquals(acl, result);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION,
				ACCESS_TYPE.CHANGE_PERMISSIONS);
		verify(mockAclDao).update(acl, ObjectType.ORGANIZATION);
	}
	
	@Test
	public void testUpdateOrganizationAclRevokeOwnAccess() {
		// try to revoke all access to the organization
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		assertThrows(InvalidModelException.class, ()->{
			// call under test
			manager.updateOrganizationAcl(user, organization.getId(), acl);
		});
		verify(mockAclDao, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}

	@Test
	public void testUpdateOrganizationAclPassedIdDoesNotMatchAclId() {
		String passedId = "456";
		// ID in the ACL does not match the passed ID
		acl.setId("123");

		when(mockAclDao.canAccess(user, passedId, ObjectType.ORGANIZATION, ACCESS_TYPE.CHANGE_PERMISSIONS))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAclDao.get(passedId, ObjectType.ORGANIZATION)).thenReturn(acl);

		// call under test
		AccessControlList result = manager.updateOrganizationAcl(user, passedId, acl);
		assertEquals(acl, result);
		// the
		verify(mockAclDao).canAccess(user, passedId, ObjectType.ORGANIZATION, ACCESS_TYPE.CHANGE_PERMISSIONS);
		verify(mockAclDao).update(aclCaptor.capture(), eq(ObjectType.ORGANIZATION));
		// passed ACL should have the ID from the paths
		AccessControlList capturedAcl = aclCaptor.getValue();
		assertEquals(passedId, capturedAcl.getId());
		verify(mockAclDao).get(passedId, ObjectType.ORGANIZATION);
	}

	@Test
	public void testUpdateOrganizationAclUnauthorized() {
		when(mockAclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CHANGE_PERMISSIONS))
				.thenReturn(AuthorizationStatus.accessDenied("not allowed"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.updateOrganizationAcl(user, organization.getId(), acl);
		});
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION,
				ACCESS_TYPE.CHANGE_PERMISSIONS);
	}

	@Test
	public void testUpdateOrganizationAclNulUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateOrganizationAcl(user, organization.getId(), acl);
		});
	}

	@Test
	public void testUpdateOrganizationAclNulId() {
		String id = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateOrganizationAcl(user, id, acl);
		});
	}

	@Test
	public void testUpdateOrganizationAclIdNotNumber() {
		String id = "not a number";
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateOrganizationAcl(user, id, acl);
		});
	}

	@Test
	public void testUpdateOrganizationAclNulAcl() {
		acl = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateOrganizationAcl(user, organization.getId(), acl);
		});
	}

	@Test
	public void testDeleteOrganization() {
		when(mockAclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		manager.deleteOrganization(user, organization.getId());
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE);
		verify(mockOrganizationDao).deleteOrganization(organization.getId());
		verify(mockAclDao).delete(organization.getId(), ObjectType.ORGANIZATION);
	}
	
	@Test
	public void testDeleteOrganizationAsAdmin() {
		boolean isAdmin = true;
		UserInfo admin = new UserInfo(isAdmin, 123L);
		// call under test
		manager.deleteOrganization(admin, organization.getId());
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class));
		verify(mockOrganizationDao).deleteOrganization(organization.getId());
		verify(mockAclDao).delete(organization.getId(), ObjectType.ORGANIZATION);
	}
	
	@Test
	public void testDeleteOrganizationUnauthorized() {
		when(mockAclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationStatus.accessDenied("no way"));
		assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.deleteOrganization(user, organization.getId());
		});
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE);
		verify(mockOrganizationDao, never()).deleteOrganization(anyString());
		verify(mockAclDao, never()).delete(anyString(), any(ObjectType.class));
	}
	
	@Test
	public void testDeleteOrganizationNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.deleteOrganization(user, organization.getId());
		});
	}
	
	@Test
	public void testDeleteOrganizationNullId() {
		String id = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.deleteOrganization(user, id);
		});
	}
	
	@Test
	public void testGetOrganizationByName() {
		String name = "org.org";
		when(mockOrganizationDao.getOrganizationByName(name)).thenReturn(organization);
		// call under test
		Organization result = manager.getOrganizationByName(user, name);
		assertEquals(result, organization);
		verify(mockOrganizationDao).getOrganizationByName(name);
	}
	
	@Test
	public void testGetOrganizationByNameTrim() {
		String name = " org.org\n";
		String trimmedName = "org.org";
		when(mockOrganizationDao.getOrganizationByName(trimmedName)).thenReturn(organization);
		// call under test
		Organization result = manager.getOrganizationByName(user, name);
		assertEquals(result, organization);
		verify(mockOrganizationDao).getOrganizationByName(trimmedName);
	}
}
