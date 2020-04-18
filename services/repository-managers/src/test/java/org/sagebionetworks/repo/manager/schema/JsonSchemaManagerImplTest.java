package org.sagebionetworks.repo.manager.schema;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import org.sagebionetworks.repo.model.dbo.schema.JsonSchemaDao;
import org.sagebionetworks.repo.model.dbo.schema.NewSchemaRequest;
import org.sagebionetworks.repo.model.dbo.schema.NewVersionRequest;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.SchemaInfo;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.schema.semantic.version.SemanticVersion;

@ExtendWith(MockitoExtension.class)
public class JsonSchemaManagerImplTest {

	@Mock
	OrganizationDao mockOrganizationDao;

	@Mock
	AccessControlListDAO mockAclDao;

	@Mock
	JsonSchemaDao mockSchemaDao;

	@Captor
	ArgumentCaptor<AccessControlList> aclCaptor;

	@InjectMocks
	JsonSchemaManagerImpl manager;
	JsonSchemaManagerImpl managerSpy;

	Date now;
	UserInfo user;
	UserInfo anonymousUser;
	String organizationName;
	CreateOrganizationRequest createOrganizationRequest;
	String schemaName;
	String semanticVersionString;
	JsonSchema schema;
	CreateSchemaRequest createSchemaRequest;
	String schemaJson;
	String schemaJsonSHA256Hex;
	String jsonBlobId;
	SchemaId schemaId;

	Organization organization;
	SchemaInfo schemaInfo;
	JsonSchemaVersionInfo versionInfo;

	AccessControlList acl;

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		managerSpy = Mockito.spy(manager);
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, 123L);

		anonymousUser = new UserInfo(isAdmin, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		now = new Date(1L);

		organizationName = "a.z2.b.com";
		createOrganizationRequest = new CreateOrganizationRequest();
		createOrganizationRequest.setOrganizationName(organizationName);

		organization = new Organization();
		organization.setCreatedBy(user.getId().toString());
		organization.setName(organizationName);
		organization.setCreatedOn(now);
		organization.setId("4321");

		acl = AccessControlListUtil.createACL(organization.getId(), user, JsonSchemaManagerImpl.ADMIN_PERMISSIONS,
				new Date());

		schemaName = "path.SomeSchema.json";
		semanticVersionString = "1.2.3";
		schema = new JsonSchema();
		schema.set$id(organization.getName() + "/" + schemaName + "/" + semanticVersionString);
		schemaId = SchemaIdParser.parseSchemaId(schema.get$id());

		createSchemaRequest = new CreateSchemaRequest();
		createSchemaRequest.setSchema(schema);

		schemaJson = EntityFactory.createJSONStringForEntity(schema);
		schemaJsonSHA256Hex = DigestUtils.sha256Hex(schemaJson);
		jsonBlobId = "987";

		schemaInfo = new SchemaInfo();
		schemaInfo.setCreatedBy(user.getId().toString());
		schemaInfo.setCreatedOn(now);
		schemaInfo.setName(schemaName);
		schemaInfo.setNumericId("432");
		schemaInfo.setOrganizationId(organization.getId());

		versionInfo = new JsonSchemaVersionInfo();
		versionInfo.setCreatedBy(user.getId().toString());
		versionInfo.setCreatedOn(now);
		versionInfo.setJsonSHA256Hex(schemaJsonSHA256Hex);
		versionInfo.setSemanticVersion(semanticVersionString);
		versionInfo.setVersionId("765");
	}

	@Test
	public void testProcessAndValidateOrganizationName() {
		String inputName = " A.b9.C.DEFG \n";
		String processedName = JsonSchemaManagerImpl.processAndValidateOrganizationName(inputName);
		assertEquals("A.b9.C.DEFG", processedName);
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
	public void testProcessAndValidateOrganizationNameSagebionetworksUpper() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("SageBionetwork");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.SAGEBIONETWORKS_RESERVED_MESSAGE, message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameStartWithDigit() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("1abcdefg");
		}).getMessage();
		assertTrue(message.startsWith("Invalid 'organizationName'"));
	}

	@Test
	public void testProcessAndValidateOrganizationNameStartWithDot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(".abcdefg");
		}).getMessage();
		assertTrue(message.startsWith("Invalid 'organizationName'"));
	}

	@Test
	public void testProcessAndValidateOrganizationNameEndWithDot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("abcdefg.");
		}).getMessage();
		assertTrue(message.startsWith("Invalid 'organizationName'"));
	}

	@Test
	public void testProcessAndValidateOrganizationNameContainsInvalidChars() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName("abc/defg");
		}).getMessage();
		assertTrue(message.startsWith("Invalid 'organizationName'"));
	}

	@Test
	public void testCreateOrganziation() {
		when(mockOrganizationDao.createOrganization(createOrganizationRequest.getOrganizationName(), user.getId()))
				.thenReturn(organization);
		// call under test
		Organization returned = manager.createOrganziation(user, createOrganizationRequest);
		assertNotNull(returned);
		assertEquals(organization, returned);

		verify(mockOrganizationDao).createOrganization(createOrganizationRequest.getOrganizationName(), user.getId());

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
		createOrganizationRequest.setOrganizationName("ALLCAPS");
		when(mockOrganizationDao.createOrganization(anyString(), anyLong())).thenReturn(organization);
		// call under test
		Organization returned = manager.createOrganziation(user, createOrganizationRequest);
		assertNotNull(returned);
		assertEquals(organization, returned);

		verify(mockOrganizationDao).createOrganization("ALLCAPS", user.getId());
	}

	@Test
	public void testCreateOrganziationNameWithWhiteSpace() {
		createOrganizationRequest.setOrganizationName(" needs.trimmed\n");
		when(mockOrganizationDao.createOrganization(anyString(), anyLong())).thenReturn(organization);
		// call under test
		Organization returned = manager.createOrganziation(user, createOrganizationRequest);
		assertNotNull(returned);
		assertEquals(organization, returned);

		verify(mockOrganizationDao).createOrganization("needs.trimmed", user.getId());
	}

	@Test
	public void testCreateOrganizationAnonymous() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createOrganziation(anonymousUser, createOrganizationRequest);
		});
	}

	@Test
	public void testCreateOrganizationNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrganziation(user, createOrganizationRequest);
		});
	}

	@Test
	public void testCreateOrganizationNullRequest() {
		createOrganizationRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrganziation(user, createOrganizationRequest);
		});
	}

	@Test
	public void testCreateOrganizationNullName() {
		createOrganizationRequest.setOrganizationName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrganziation(user, createOrganizationRequest);
		});
	}

	@Test
	public void testCreateOrganizationBadName() {
		createOrganizationRequest.setOrganizationName("endsWithDot.");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrganziation(user, createOrganizationRequest);
		}).getMessage();
		assertTrue(message.startsWith("Invalid 'organizationName'"));
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
		assertThrows(InvalidModelException.class, () -> {
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
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockOrganizationDao).deleteOrganization(organization.getId());
		verify(mockAclDao).delete(organization.getId(), ObjectType.ORGANIZATION);
	}

	@Test
	public void testDeleteOrganizationUnauthorized() {
		when(mockAclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationStatus.accessDenied("no way"));
		assertThrows(UnauthorizedException.class, () -> {
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
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.deleteOrganization(user, organization.getId());
		});
	}

	@Test
	public void testDeleteOrganizationNullId() {
		String id = null;
		assertThrows(IllegalArgumentException.class, () -> {
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

	@Test
	public void testCreateJsonBlobIfDoesNotExist() {
		when(mockSchemaDao.createJsonBlobIfDoesNotExist(any(), any())).thenReturn(jsonBlobId);
		// call under test
		String blobId = manager.createJsonBlobIfDoesNotExist(schema);
		assertEquals(jsonBlobId, blobId);
		verify(mockSchemaDao).createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSHA256Hex);
	}

	@Test
	public void testCreateJsonBlobIfDoesNotExistNullSchema() {
		schema = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createJsonBlobIfDoesNotExist(schema);
		});
	}

	@Test
	public void testGetAndValidateSemanticVersion() {
		SemanticVersion version = schemaId.getSemanticVersion();
		assertNotNull(version);
		// call under test
		String versionString = manager.getAndValidateSemanticVersion(version);
		assertEquals(semanticVersionString, versionString);
	}

	@Test
	public void testGetAndValidateSemanticVersionNull() {
		SemanticVersion version = null;
		// call under test
		String versionString = manager.getAndValidateSemanticVersion(version);
		assertNull(versionString);
	}

	@Test
	public void testGetAndValidateSemanticVersionAtLimit() throws ParseException {
		String prefix = "2.3.1+";
		String version = prefix
				+ StringUtils.repeat('a', JsonSchemaManagerImpl.MAX_SEMANTIC_VERSION_CHARS - prefix.length());
		assertEquals(JsonSchemaManagerImpl.MAX_SEMANTIC_VERSION_CHARS, version.length());
		SemanticVersion semanticVersion = new SchemaIdParser(version).semanticVersion();
		// call under test
		String versionString = manager.getAndValidateSemanticVersion(semanticVersion);
		assertEquals(version, versionString);
	}

	@Test
	public void testGetAndValidateSemanticVersionOverLimit() throws ParseException {
		String prefix = "2.3.1+";
		String version = prefix
				+ StringUtils.repeat('a', JsonSchemaManagerImpl.MAX_SEMANTIC_VERSION_CHARS - prefix.length() + 1);
		assertEquals(JsonSchemaManagerImpl.MAX_SEMANTIC_VERSION_CHARS + 1, version.length());
		SemanticVersion semanticVersion = new SchemaIdParser(version).semanticVersion();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getAndValidateSemanticVersion(semanticVersion);
		}).getMessage();
		assertEquals("Semantic version must be 250 characters or less", message);
	}

	@Test
	public void testCreateJsonSchema() {
		when(mockOrganizationDao.getOrganizationByName(any())).thenReturn(organization);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockSchemaDao.createSchemaIfDoesNotExist(any())).thenReturn(schemaInfo);
		when(mockSchemaDao.createJsonBlobIfDoesNotExist(any(), any())).thenReturn(jsonBlobId);
		when(mockSchemaDao.createNewVersion(any())).thenReturn(versionInfo);
		// call under test
		CreateSchemaResponse response = managerSpy.createJsonSchema(user, createSchemaRequest);
		assertNotNull(response);
		assertEquals(versionInfo, response.getNewVersionInfo());
		verify(managerSpy).getAndValidateSemanticVersion(schemaId.getSemanticVersion());
		assertEquals(versionInfo, response.getNewVersionInfo());
		verify(mockOrganizationDao).getOrganizationByName(organizationName);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE);
		NewSchemaRequest expectedNewSchemaRequest = new NewSchemaRequest().withOrganizationId(organization.getId())
				.withSchemaName(schemaName).withCreatedBy(user.getId());
		verify(mockSchemaDao).createSchemaIfDoesNotExist(expectedNewSchemaRequest);
		verify(managerSpy).createJsonBlobIfDoesNotExist(schema);
		verify(mockSchemaDao).createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSHA256Hex);
		NewVersionRequest expectedNewVersionRequest = new NewVersionRequest().withSchemaId(schemaInfo.getNumericId())
				.withCreatedBy(user.getId()).withBlobId(jsonBlobId).withSemanticVersion(semanticVersionString);
		verify(mockSchemaDao).createNewVersion(expectedNewVersionRequest);
	}
	
	@Test
	public void testCreateJsonSchemaAnonymous() {
		String message = assertThrows(UnauthorizedException.class, ()->{
			 manager.createJsonSchema(anonymousUser, createSchemaRequest);
		}).getMessage();
		assertEquals("Must login to perform this action", message);
	}
	
	@Test
	public void testCreateJsonSchemaUnauthorized() {
		when(mockOrganizationDao.getOrganizationByName(any())).thenReturn(organization);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.accessDenied("no"));
		String message = assertThrows(UnauthorizedException.class, ()->{
			manager.createJsonSchema(user, createSchemaRequest);
		}).getMessage();
		assertEquals("no", message);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE);
	}
	
	@Test
	public void testCreateJsonSchemaNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, ()->{
			manager.createJsonSchema(user, createSchemaRequest);
		});
	}
	
	@Test
	public void testCreateJsonSchemaNullRequest() {
		createSchemaRequest = null;
		assertThrows(IllegalArgumentException.class, ()->{
			manager.createJsonSchema(user, createSchemaRequest);
		});
	}
	
	@Test
	public void testCreateJsonSchemaNullSchema() {
		createSchemaRequest.setSchema(null);
		assertThrows(IllegalArgumentException.class, ()->{
			manager.createJsonSchema(user, createSchemaRequest);
		});
	}
}
