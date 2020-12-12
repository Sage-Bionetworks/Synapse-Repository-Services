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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
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
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.schema.BindSchemaRequest;
import org.sagebionetworks.repo.model.dbo.schema.JsonSchemaDao;
import org.sagebionetworks.repo.model.dbo.schema.NewSchemaVersionRequest;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.dbo.schema.SchemaDependency;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.SchemaIdParser;

import com.google.common.collect.Lists;

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
	UserInfo adminUser;
	String organizationName;
	CreateOrganizationRequest createOrganizationRequest;
	String schemaName;
	String semanticVersionString;
	JsonSchema schema;
	CreateSchemaRequest createSchemaRequest;
	String schemaJson;
	String schemaJsonSHA256Hex;
	String jsonBlobId;
	SchemaId parsed$Id;
	String versionId;
	String schemaId;

	Organization organization;
	JsonSchemaVersionInfo versionInfo;
	
	boolean isTopLevel;

	ListOrganizationsRequest listOrganizationsRequest;
	ListJsonSchemaInfoRequest listJsonSchemaInfoRequest;
	ListJsonSchemaVersionInfoRequest listJsonSchemaVersionInfoRequest;

	AccessControlList acl;
	JsonSchema validationSchema;

	Long objectId;
	BoundObjectType objectType;

	JsonSchemaObjectBinding jsonSchemaObjectBinding;

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		managerSpy = Mockito.spy(manager);
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, 123L);
		isAdmin = true;
		adminUser = new UserInfo(isAdmin, 456L);

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
		schema.set$id(organization.getName() + "-" + schemaName + "-" + semanticVersionString);
		parsed$Id = SchemaIdParser.parseSchemaId(schema.get$id());

		createSchemaRequest = new CreateSchemaRequest();
		createSchemaRequest.setSchema(schema);

		schemaJson = EntityFactory.createJSONStringForEntity(schema);
		schemaJsonSHA256Hex = DigestUtils.sha256Hex(schemaJson);
		jsonBlobId = "987";

		schemaId = "3333";
		versionId = "888";

		versionInfo = new JsonSchemaVersionInfo();
		versionInfo.setOrganizationId(organization.getId());
		versionInfo.setOrganizationName(organizationName);
		versionInfo.setCreatedBy(user.getId().toString());
		versionInfo.setCreatedOn(now);
		versionInfo.setJsonSHA256Hex(schemaJsonSHA256Hex);
		versionInfo.setSemanticVersion(semanticVersionString);
		versionInfo.setVersionId(versionId);
		versionInfo.setSchemaId(schemaId);

		listOrganizationsRequest = new ListOrganizationsRequest();

		listJsonSchemaInfoRequest = new ListJsonSchemaInfoRequest();
		listJsonSchemaInfoRequest.setOrganizationName(organizationName);

		listJsonSchemaVersionInfoRequest = new ListJsonSchemaVersionInfoRequest();
		listJsonSchemaVersionInfoRequest.setOrganizationName(organizationName);
		listJsonSchemaVersionInfoRequest.setSchemaName(schemaName);

		validationSchema = new JsonSchema();
		validationSchema.set$id(schema.get$id());
		validationSchema.setDescription("validation schema");

		objectId = 456L;
		objectType = BoundObjectType.entity;

		jsonSchemaObjectBinding = new JsonSchemaObjectBinding();
		jsonSchemaObjectBinding.setCreatedBy(adminUser.getId().toString());
		jsonSchemaObjectBinding.setCreatedOn(now);
		jsonSchemaObjectBinding.setObjectId(objectId);
		jsonSchemaObjectBinding.setObjectType(objectType);
		jsonSchemaObjectBinding.setJsonSchemaVersionInfo(versionInfo);
		isTopLevel = false;
	}

	@Test
	public void processAndValidateOrganizationNameWithNullUser() {
		String inputName = " A.b9.C.DEFG \n";
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, inputName);
		});
	}

	@Test
	public void processAndValidateOrganizationNameWithNullInputName() {
		String inputName = null;
		assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, inputName);
		});
	}

	@Test
	public void testProcessAndValidateOrganizationName() {
		String inputName = " A.b9.C.DEFG \n";
		String processedName = JsonSchemaManagerImpl.processAndValidateOrganizationName(user, inputName);
		assertEquals("A.b9.C.DEFG", processedName);
	}

	@Test
	public void testProcessAndValidateOrganizationNameMaxLength() {
		String input = StringUtils.repeat("a", JsonSchemaManagerImpl.MAX_ORGANZIATION_NAME_CHARS);
		String processedName = JsonSchemaManagerImpl.processAndValidateOrganizationName(user, input);
		assertNotNull(processedName);
		assertEquals(JsonSchemaManagerImpl.MAX_ORGANZIATION_NAME_CHARS, processedName.length());
	}

	@Test
	public void testProcessAndValidateOrganizationNameOverMaxLength() {
		String input = StringUtils.repeat("a", JsonSchemaManagerImpl.MAX_ORGANZIATION_NAME_CHARS + 1);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, input);
		}).getMessage();
		assertEquals("Organization name must be 250 characters or less", message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameMinLength() {
		String input = StringUtils.repeat("a", JsonSchemaManagerImpl.MIN_ORGANZIATION_NAME_CHARS);
		String processedName = JsonSchemaManagerImpl.processAndValidateOrganizationName(user, input);
		assertNotNull(processedName);
		assertEquals(JsonSchemaManagerImpl.MIN_ORGANZIATION_NAME_CHARS, processedName.length());
	}

	@Test
	public void testProcessAndValidateOrganizationNameUnderMinLength() {
		String input = StringUtils.repeat("a", JsonSchemaManagerImpl.MIN_ORGANZIATION_NAME_CHARS - 1);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, input);
		}).getMessage();
		assertEquals("Organization name must be at least 6 characters", message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameSagebionetworks() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, "sagebionetwork");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.SAGEBIONETWORKS_RESERVED_MESSAGE, message);
	}

	@Test
	public void testProcessAndValidateOrganizationWithNameSagebionetworksAdmin() {
		// call under test
		String name = JsonSchemaManagerImpl.processAndValidateOrganizationName(adminUser, "org.sagebionetworks");
		assertEquals("org.sagebionetworks", name);
	}

	@Test
	public void testProcessAndValidateOrganizationNameSagebionetworksUpper() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, "SageBionetwork");
		}).getMessage();
		assertEquals(JsonSchemaManagerImpl.SAGEBIONETWORKS_RESERVED_MESSAGE, message);
	}

	@Test
	public void testProcessAndValidateOrganizationNameStartWithDigit() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, "1abcdefg");
		}).getMessage();
		assertTrue(message.startsWith("Invalid 'organizationName'"));
	}

	@Test
	public void testProcessAndValidateOrganizationNameStartWithDot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, ".abcdefg");
		}).getMessage();
		assertTrue(message.startsWith("Invalid 'organizationName'"));
	}

	@Test
	public void testProcessAndValidateOrganizationNameEndWithDot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, "abcdefg.");
		}).getMessage();
		assertTrue(message.startsWith("Invalid 'organizationName'"));
	}

	@Test
	public void testProcessAndValidateOrganizationNameContainsInvalidChars() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JsonSchemaManagerImpl.processAndValidateOrganizationName(user, "abc/defg");
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
	public void testGetSchemaVersionId() {
		String versionId = "123";
		when(mockSchemaDao.getLatestVersionId(any(), any())).thenReturn(versionId);
		// call under test
		String id = manager.getSchemaVersionId("org-one");
		assertEquals(versionId, id);
		String organizationName = "org";
		String schemaName = "one";
		verify(mockSchemaDao).getLatestVersionId(organizationName, schemaName);
		verify(mockSchemaDao, never()).getVersionId(any(), any(), any());
	}

	@Test
	public void testGetSchemaVersionIdWithVersion() {
		String versionId = "123";
		when(mockSchemaDao.getVersionId(any(), any(), any())).thenReturn(versionId);
		// call under test
		String id = manager.getSchemaVersionId("org-one-1.0.1");
		assertEquals(versionId, id);
		String organizationName = "org";
		String schemaName = "one";
		String semanticVersion = "1.0.1";
		verify(mockSchemaDao, never()).getLatestVersionId(any(), any());
		verify(mockSchemaDao).getVersionId(organizationName, schemaName, semanticVersion);
	}

	@Test
	public void testGetSchemaVersionIdWithNullId() {
		String $id = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.getSchemaVersionId($id);
		});
	}

	@Test
	public void testFindAllDependencies() {
		JsonSchema one = new JsonSchema();
		one.set$id("org-one");

		JsonSchema refToOne = new JsonSchema();
		refToOne.set$ref(one.get$id());

		JsonSchema two = new JsonSchema();
		two.set$id("org-two");
		two.setItems(refToOne);

		String organizationName = "org";
		String schemaName = "one";
		String versionId = "123";
		when(mockSchemaDao.getLatestVersionId(any(), any())).thenReturn(versionId);
		JsonSchemaVersionInfo versionInfo = new JsonSchemaVersionInfo();
		versionInfo.setVersionId(versionId);
		versionInfo.setSchemaId("111");
		when(mockSchemaDao.getVersionInfo(any())).thenReturn(versionInfo);

		// call under test
		List<SchemaDependency> actual = manager.findAllDependencies(two);
		assertNotNull(actual);
		List<SchemaDependency> expected = Lists.newArrayList(new SchemaDependency().withDependsOnSchemaId("111"));
		assertEquals(expected, actual);
		verify(mockSchemaDao).getLatestVersionId(organizationName, schemaName);
		verify(mockSchemaDao, never()).getVersionId(any(), any(), any());
		verify(mockSchemaDao).getVersionInfo(versionId);
	}

	@Test
	public void testFindAllDependenciesWithVersion() {
		JsonSchema one = new JsonSchema();
		one.set$id("org-one-1.1.1");

		JsonSchema refToOne = new JsonSchema();
		refToOne.set$ref(one.get$id());

		JsonSchema two = new JsonSchema();
		two.set$id("org-two");
		two.setItems(refToOne);

		String organizationName = "org";
		String schemaName = "one";
		String semanticVersion = "1.1.1";
		String versionId = "123";
		when(mockSchemaDao.getVersionId(any(), any(), any())).thenReturn(versionId);
		JsonSchemaVersionInfo versionInfo = new JsonSchemaVersionInfo();
		versionInfo.setVersionId(versionId);
		versionInfo.setSchemaId("111");
		when(mockSchemaDao.getVersionInfo(any())).thenReturn(versionInfo);

		// call under test
		List<SchemaDependency> actual = manager.findAllDependencies(two);
		assertNotNull(actual);
		// depends on both the schemaId and versionId
		ArrayList<SchemaDependency> expected = Lists
				.newArrayList(new SchemaDependency().withDependsOnSchemaId("111").withDependsOnVersionId(versionId));
		assertEquals(expected, actual);
		verify(mockSchemaDao, never()).getLatestVersionId(any(), any());
		verify(mockSchemaDao).getVersionId(organizationName, schemaName, semanticVersion);
		verify(mockSchemaDao).getVersionInfo(versionId);
	}

	@Test
	public void testFindAllDependenciesWith$RefNotFound() {
		JsonSchema one = new JsonSchema();
		one.set$id("org-one-1.1.1");

		JsonSchema refToOne = new JsonSchema();
		refToOne.set$ref(one.get$id());

		JsonSchema two = new JsonSchema();
		two.set$id("org-two");
		two.setItems(refToOne);

		String organizationName = "org";
		String schemaName = "one";
		String semanticVersion = "1.1.1";
		String versionId = "123";
		when(mockSchemaDao.getVersionId(any(), any(), any())).thenThrow(new NotFoundException());

		assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.findAllDependencies(two);
		});
		verify(mockSchemaDao, never()).getLatestVersionId(any(), any());
		verify(mockSchemaDao).getVersionId(organizationName, schemaName, semanticVersion);
		verify(mockSchemaDao, never()).getVersionInfo(versionId);
	}

	@Test
	public void testFindAllDependenciesWithNo$Refs() {
		JsonSchema one = new JsonSchema();
		one.set$id("org-one-1.1.1");

		// two depends on one directly
		JsonSchema two = new JsonSchema();
		two.set$id("org-two");
		two.setItems(one);

		// call under test
		List<SchemaDependency> actual = manager.findAllDependencies(two);
		List<SchemaDependency> expected = new ArrayList<SchemaDependency>();
		assertEquals(expected, actual);

		verify(mockSchemaDao, never()).getLatestVersionId(any(), any());
		verify(mockSchemaDao, never()).getVersionId(any(), any(), any());
		verify(mockSchemaDao, never()).getVersionInfo(versionId);
	}

	@Test
	public void testFindAllDependenciesWithNullSchema() {
		JsonSchema nullSchema = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.findAllDependencies(nullSchema);
		});
	}

	@Test
	public void testFindAllDependenciesWithNoRefs() {
		JsonSchema one = new JsonSchema();
		one.set$id("org/one-1.1.1");
		// call under test
		List<SchemaDependency> actual = manager.findAllDependencies(one);
		assertNotNull(actual);
		// depends on both the schemaId and versionId
		ArrayList<SchemaDependency> expected = new ArrayList<SchemaDependency>();
		assertEquals(expected, actual);
		verify(mockSchemaDao, never()).getLatestVersionId(any(), any());
		verify(mockSchemaDao, never()).getVersionId(any(), any(), any());
		verify(mockSchemaDao, never()).getVersionInfo(any());
	}

	@Test
	public void testCreateJsonSchema() {
		when(mockOrganizationDao.getOrganizationByName(any())).thenReturn(organization);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockSchemaDao.createNewSchemaVersion(any())).thenReturn(versionInfo);
		doReturn(validationSchema).when(managerSpy).getValidationSchema(schema.get$id());
		doReturn(SchemaIdParser.parseSchemaId(schema.get$id())).when(managerSpy).validateSchema(any());
		// call under test
		CreateSchemaResponse response = managerSpy.createJsonSchema(user, createSchemaRequest);
		assertNotNull(response);
		assertEquals(versionInfo, response.getNewVersionInfo());
		assertEquals(validationSchema, response.getValidationSchema());
		verify(mockOrganizationDao).getOrganizationByName(organizationName);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE);
		NewSchemaVersionRequest expectedNewSchemaRequest = new NewSchemaVersionRequest()
				.withOrganizationId(organization.getId()).withSchemaName(schemaName).withCreatedBy(user.getId())
				.withJsonSchema(schema).withSemanticVersion(semanticVersionString)
				.withDependencies(new ArrayList<SchemaDependency>());
		verify(mockSchemaDao).createNewSchemaVersion(expectedNewSchemaRequest);
		verify(managerSpy).getValidationSchema(schema.get$id());
		verify(managerSpy).validateSchema(schema);
	}

	@Test
	public void testCreateJsonSchemaNullVersion() {
		when(mockOrganizationDao.getOrganizationByName(any())).thenReturn(organization);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockSchemaDao.createNewSchemaVersion(any())).thenReturn(versionInfo);
		schema.set$id(organizationName + "-" + schemaName);
		doReturn(validationSchema).when(managerSpy).getValidationSchema(schema.get$id());
		doReturn(SchemaIdParser.parseSchemaId(schema.get$id())).when(managerSpy).validateSchema(any());
		// call under test
		CreateSchemaResponse response = managerSpy.createJsonSchema(user, createSchemaRequest);
		assertNotNull(response);
		assertEquals(versionInfo, response.getNewVersionInfo());
		verify(mockOrganizationDao).getOrganizationByName(organizationName);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE);
		NewSchemaVersionRequest expectedNewSchemaRequest = new NewSchemaVersionRequest()
				.withOrganizationId(organization.getId()).withSchemaName(schemaName).withCreatedBy(user.getId())
				.withJsonSchema(schema).withSemanticVersion(null).withDependencies(new ArrayList<SchemaDependency>());
		verify(mockSchemaDao).createNewSchemaVersion(expectedNewSchemaRequest);
		verify(managerSpy).validateSchema(schema);
	}
	
	@Test
	public void testCreateJsonSchemaWithDryRun() {
		when(mockOrganizationDao.getOrganizationByName(any())).thenReturn(organization);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockSchemaDao.createNewSchemaVersion(any())).thenReturn(versionInfo);
		doReturn(validationSchema).when(managerSpy).getValidationSchema(schema.get$id());
		doReturn(SchemaIdParser.parseSchemaId(schema.get$id())).when(managerSpy).validateSchema(any());
		createSchemaRequest.setDryRun(true);
		// call under test
		CreateSchemaResponse response = managerSpy.createJsonSchema(user, createSchemaRequest);
		assertNotNull(response);
		assertEquals(versionInfo, response.getNewVersionInfo());
		assertEquals(validationSchema, response.getValidationSchema());
		verify(mockOrganizationDao).getOrganizationByName(organizationName);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE);
		NewSchemaVersionRequest expectedNewSchemaRequest = new NewSchemaVersionRequest()
				.withOrganizationId(organization.getId()).withSchemaName(schemaName).withCreatedBy(user.getId())
				.withJsonSchema(schema).withSemanticVersion(semanticVersionString)
				.withDependencies(new ArrayList<SchemaDependency>());
		verify(mockSchemaDao).createNewSchemaVersion(expectedNewSchemaRequest);
		verify(managerSpy).getValidationSchema(schema.get$id());
		verify(managerSpy).validateSchema(schema);
		verify(mockSchemaDao).deleteSchemaVersion(versionInfo.getVersionId());
	}
	
	@Test
	public void testCreateJsonSchemaWithDryRunNull() {
		when(mockOrganizationDao.getOrganizationByName(any())).thenReturn(organization);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockSchemaDao.createNewSchemaVersion(any())).thenReturn(versionInfo);
		doReturn(validationSchema).when(managerSpy).getValidationSchema(schema.get$id());
		doReturn(SchemaIdParser.parseSchemaId(schema.get$id())).when(managerSpy).validateSchema(any());
		createSchemaRequest.setDryRun(null);
		// call under test
		CreateSchemaResponse response = managerSpy.createJsonSchema(user, createSchemaRequest);
		assertNotNull(response);
		assertEquals(versionInfo, response.getNewVersionInfo());
		assertEquals(validationSchema, response.getValidationSchema());
		verify(mockOrganizationDao).getOrganizationByName(organizationName);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE);
		NewSchemaVersionRequest expectedNewSchemaRequest = new NewSchemaVersionRequest()
				.withOrganizationId(organization.getId()).withSchemaName(schemaName).withCreatedBy(user.getId())
				.withJsonSchema(schema).withSemanticVersion(semanticVersionString)
				.withDependencies(new ArrayList<SchemaDependency>());
		verify(mockSchemaDao).createNewSchemaVersion(expectedNewSchemaRequest);
		verify(managerSpy).getValidationSchema(schema.get$id());
		verify(managerSpy).validateSchema(schema);
		verify(mockSchemaDao, never()).deleteSchemaVersion(any());
	}
	
	@Test
	public void testCreateJsonSchemaWithDryRunFalse() {
		when(mockOrganizationDao.getOrganizationByName(any())).thenReturn(organization);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockSchemaDao.createNewSchemaVersion(any())).thenReturn(versionInfo);
		doReturn(validationSchema).when(managerSpy).getValidationSchema(schema.get$id());
		doReturn(SchemaIdParser.parseSchemaId(schema.get$id())).when(managerSpy).validateSchema(any());
		createSchemaRequest.setDryRun(false);
		// call under test
		CreateSchemaResponse response = managerSpy.createJsonSchema(user, createSchemaRequest);
		assertNotNull(response);
		assertEquals(versionInfo, response.getNewVersionInfo());
		assertEquals(validationSchema, response.getValidationSchema());
		verify(mockOrganizationDao).getOrganizationByName(organizationName);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE);
		NewSchemaVersionRequest expectedNewSchemaRequest = new NewSchemaVersionRequest()
				.withOrganizationId(organization.getId()).withSchemaName(schemaName).withCreatedBy(user.getId())
				.withJsonSchema(schema).withSemanticVersion(semanticVersionString)
				.withDependencies(new ArrayList<SchemaDependency>());
		verify(mockSchemaDao).createNewSchemaVersion(expectedNewSchemaRequest);
		verify(managerSpy).getValidationSchema(schema.get$id());
		verify(managerSpy).validateSchema(schema);
		verify(mockSchemaDao, never()).deleteSchemaVersion(any());
	}
	
	@Test
	public void testValidateSchemaWithNoSubSchema() {
		// call under test
		SchemaId id = manager.validateSchema(schema);
		assertEquals(SchemaIdParser.parseSchemaId(schema.get$id()), id);
	}
	
	@Test
	public void testValidateSchemaWithSubWihout$Ref() {
		JsonSchema subSchema = new JsonSchema();
		subSchema.set$ref(null);
		schema.setAllOf(Lists.newArrayList(subSchema));
		// call under test
		SchemaId id = manager.validateSchema(schema);
		assertEquals(SchemaIdParser.parseSchemaId(schema.get$id()), id);
	}
	
	@Test
	public void testValidateSchemaWith$IdWithoutVersion$RefWithoutVersion() {
		schema.set$id("org-name");
		JsonSchema subSchema = new JsonSchema();
		subSchema.set$ref("org-other");
		schema.setAllOf(Lists.newArrayList(subSchema));
		// call under test
		SchemaId id = manager.validateSchema(schema);
		assertEquals(SchemaIdParser.parseSchemaId(schema.get$id()), id);
	}
	
	@Test
	public void testValidateSchemaWith$IdWithoutVersion$RefWithVersion() {
		schema.set$id("org-name");
		JsonSchema subSchema = new JsonSchema();
		subSchema.set$ref("org-other-2.0.4");
		schema.setAllOf(Lists.newArrayList(subSchema));
		// call under test
		SchemaId id = manager.validateSchema(schema);
		assertEquals(SchemaIdParser.parseSchemaId(schema.get$id()), id);
	}
	
	@Test
	public void testValidateSchemaWith$IdWithVersion$RefWithVersion() {
		schema.set$id("org-name-1.0.1");
		JsonSchema subSchema = new JsonSchema();
		subSchema.set$ref("org-other-2.0.4");
		schema.setAllOf(Lists.newArrayList(subSchema));
		// call under test
		SchemaId id = manager.validateSchema(schema);
		assertEquals(SchemaIdParser.parseSchemaId(schema.get$id()), id);
	}
	
	@Test
	public void testValidateSchemaWith$IdWithVersion$RefWithoutVersion() {
		schema.set$id("org-name-1.0.1");
		JsonSchema subSchema = new JsonSchema();
		subSchema.set$ref("org-other");
		schema.setAllOf(Lists.newArrayList(subSchema));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateSchema(schema);
		}).getMessage();
		assertEquals(
				"The schema $id includes a semantic version, therefore all sub-schema"
				+ " references ($ref) must also include a semantic version."
				+ "  The following $ref does not include a semantic version: 'org-other'",
				message);
	}
	
	@Test
	public void testValidateSchemaWithNullSchema() {
		schema = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateSchema(schema);
		}).getMessage();
		assertEquals("schema is required.", message);
	}
	
	@Test
	public void testValidateSchemaWithNull$Id() {
		schema.set$id(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateSchema(schema);
		}).getMessage();
		assertEquals("schema.$id is required.", message);
	}

	@Test
	public void testCreateJsonSchemaAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			manager.createJsonSchema(anonymousUser, createSchemaRequest);
		}).getMessage();
		assertEquals("Must login to perform this action", message);
	}

	@Test
	public void testCreateJsonSchemaUnauthorized() {
		when(mockOrganizationDao.getOrganizationByName(any())).thenReturn(organization);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.accessDenied("no"));
		String message = assertThrows(UnauthorizedException.class, () -> {
			manager.createJsonSchema(user, createSchemaRequest);
		}).getMessage();
		assertEquals("no", message);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE);
	}

	@Test
	public void testCreateJsonSchemaNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createJsonSchema(user, createSchemaRequest);
		});
	}

	@Test
	public void testCreateJsonSchemaNullRequest() {
		createSchemaRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createJsonSchema(user, createSchemaRequest);
		});
	}

	@Test
	public void testCreateJsonSchemaNullSchema() {
		createSchemaRequest.setSchema(null);
		assertThrows(IllegalArgumentException.class, () -> {
			manager.createJsonSchema(user, createSchemaRequest);
		});
	}

	@Test
	public void testGetSchemaWithVersion() {
		when(mockSchemaDao.getVersionId(any(), any(), any())).thenReturn(versionId);
		when(mockSchemaDao.getSchema(any())).thenReturn(schema);
		String $id = organizationName + "-" + schemaName + "-" + semanticVersionString;
		// call under test
		JsonSchema result = manager.getSchema($id, isTopLevel);
		assertEquals(schema, result);
		assertEquals($id, result.get$id());
		verify(mockSchemaDao).getVersionId(organizationName, schemaName, semanticVersionString);
		verify(mockSchemaDao, never()).getLatestVersionId(any(), any());
		verify(mockSchemaDao).getSchema(versionId);
	}

	/**
	 * For the get schema call, it is expected that the $id of the returned schema
	 * exactly matches the $id from the request. Therefore, if the requested $id
	 * excludes a semantic version, but the latest version of that schema includes a
	 * semantic version, then the $id of the resulting schema must be changed to
	 * match the request. Basically, the semantic version must be removed from the
	 * $id of the returned schema, when the request does not include the semantic
	 * version.
	 */
	@Test
	public void testGetSchemaNullVersion() {
		when(mockSchemaDao.getLatestVersionId(any(), any())).thenReturn(versionId);
		when(mockSchemaDao.getSchema(any())).thenReturn(schema);
		String $id = organizationName + "-" + schemaName;
		// call under test
		JsonSchema result = manager.getSchema($id, isTopLevel);
		assertEquals(schema, result);
		assertEquals($id, result.get$id());
		verify(mockSchemaDao, never()).getVersionId(any(), any(), any());
		verify(mockSchemaDao).getLatestVersionId(organizationName, schemaName);
		verify(mockSchemaDao).getSchema(versionId);
	}

	@Test
	public void testGetSchemaNull$id() {
		String $id = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.getSchema($id, isTopLevel);
		});
	}
	
	@Test
	public void testGetSchemaWithIsTopLevelFalse() {
		isTopLevel = false;
		when(mockSchemaDao.getLatestVersionId(any(), any())).thenReturn(versionId);
		when(mockSchemaDao.getSchema(any())).thenReturn(schema);
		String $id = organizationName + "-" + schemaName;
		// call under test
		JsonSchema result = manager.getSchema($id, isTopLevel);
		assertEquals(schema, result);
		assertEquals($id, result.get$id());
	}

	@Test
	public void testGetSchemaWithIsTopLevelTrue() {
		isTopLevel = true;
		when(mockSchemaDao.getLatestVersionId(any(), any())).thenReturn(versionId);
		when(mockSchemaDao.getSchema(any())).thenReturn(schema);
		String $id = organizationName + "-" + schemaName;
		// call under test
		JsonSchema result = manager.getSchema($id, isTopLevel);
		assertEquals(schema, result);
		// should be the absolute $id
		assertEquals(JsonSchemaManager.createAbsolute$id("a.z2.b.com-path.SomeSchema.json"), result.get$id());
	}
	
	@Test
	public void testDeleteSchemaByIdWithoutVersion() {
		when(mockSchemaDao.getVersionLatestInfo(any(), any())).thenReturn(versionInfo);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		String $id = organizationName + "-" + schemaName;
		// call under test
		manager.deleteSchemaById(user, $id);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE);
		verify(mockSchemaDao).deleteSchema(versionInfo.getSchemaId());
	}

	@Test
	public void testDeleteSchemaByIdWithoutVersionUnauthorized() {
		when(mockSchemaDao.getVersionLatestInfo(any(), any())).thenReturn(versionInfo);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.accessDenied("naw"));
		String $id = organizationName + "-" + schemaName;
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.deleteSchemaById(user, $id);
		});
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE);
		verify(mockSchemaDao, never()).deleteSchema(any());
		verify(mockSchemaDao, never()).deleteSchema(any());
	}

	@Test
	public void testDeleteSchemaByIdWithoutVersionAsAdmin() {
		when(mockSchemaDao.getVersionLatestInfo(any(), any())).thenReturn(versionInfo);
		String $id = organizationName + "-" + schemaName;
		// call under test
		manager.deleteSchemaById(adminUser, $id);
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), any(), any(), any());
		verify(mockSchemaDao).deleteSchema(versionInfo.getSchemaId());
	}

	@Test
	public void testDeleteSchemaByIdWithVersion() {
		when(mockSchemaDao.getVersionInfo(organizationName, schemaName, semanticVersionString)).thenReturn(versionInfo);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		String $id = organizationName + "-" + schemaName + "-" + semanticVersionString;
		// call under test
		manager.deleteSchemaById(user, $id);
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE);
		verify(mockSchemaDao).deleteSchemaVersion(versionInfo.getVersionId());
	}

	@Test
	public void testDeleteSchemaByIdWithVersionUnauthorized() {
		when(mockSchemaDao.getVersionInfo(organizationName, schemaName, semanticVersionString)).thenReturn(versionInfo);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any()))
				.thenReturn(AuthorizationStatus.accessDenied("no"));
		String $id = organizationName + "-" + schemaName + "-" + semanticVersionString;
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.deleteSchemaById(user, $id);
		});
		verify(mockAclDao).canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE);
		verify(mockSchemaDao, never()).deleteSchemaVersion(any());
		verify(mockSchemaDao, never()).deleteSchema(any());
	}

	@Test
	public void testDeleteSchemaByIdWithVersionAsAdmin() {
		when(mockSchemaDao.getVersionInfo(organizationName, schemaName, semanticVersionString)).thenReturn(versionInfo);
		String $id = organizationName + "-" + schemaName + "-" + semanticVersionString;
		// call under test
		manager.deleteSchemaById(adminUser, $id);
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), any(), any(), any());
		verify(mockSchemaDao).deleteSchemaVersion(versionInfo.getVersionId());
	}

	@Test
	public void testDeleteSchemaByIdWithoutNullUser() {
		user = null;
		String $id = organizationName + "-" + schemaName;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.deleteSchemaById(user, $id);
		});
	}

	@Test
	public void testDeleteSchemaByIdWithoutNull$id() {
		String $id = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.deleteSchemaById(user, $id);
		});
	}

	@Test
	public void testListOrganizations() {
		List<Organization> results = new ArrayList<Organization>();
		for (int i = 0; i < NextPageToken.MAX_LIMIT + 1; i++) {
			results.add(new Organization());
		}
		when(mockOrganizationDao.listOrganizations(anyLong(), anyLong())).thenReturn(results);
		// call under test
		ListOrganizationsResponse response = manager.listOrganizations(listOrganizationsRequest);
		assertNotNull(response);
		assertNotNull(response.getPage());
		assertEquals("50a50", response.getNextPageToken());
		long expectedLimit = 51;
		long expectedOffset = 0;
		verify(mockOrganizationDao).listOrganizations(expectedLimit, expectedOffset);
	}

	@Test
	public void testListOrganizationsWithNullRequest() {
		listOrganizationsRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.listOrganizations(listOrganizationsRequest);
		});
	}

	@Test
	public void testListSchemas() {
		List<JsonSchemaInfo> results = new ArrayList<JsonSchemaInfo>();
		for (int i = 0; i < NextPageToken.MAX_LIMIT + 1; i++) {
			results.add(new JsonSchemaInfo());
		}
		when(mockSchemaDao.listSchemas(any(), anyLong(), anyLong())).thenReturn(results);
		// call under test
		ListJsonSchemaInfoResponse response = manager.listSchemas(listJsonSchemaInfoRequest);
		assertNotNull(response);
		assertNotNull(response.getPage());
		assertEquals("50a50", response.getNextPageToken());
		long expectedLimit = 51;
		long expectedOffset = 0;
		verify(mockSchemaDao).listSchemas(organizationName, expectedLimit, expectedOffset);
	}

	@Test
	public void testListSchemasWithNullRequest() {
		listJsonSchemaInfoRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.listSchemas(listJsonSchemaInfoRequest);
		});
	}

	@Test
	public void testListSchemasWithNullOrganizationName() {
		listJsonSchemaInfoRequest.setOrganizationName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			manager.listSchemas(listJsonSchemaInfoRequest);
		});
	}

	@Test
	public void testListSchemaVersions() {
		List<JsonSchemaVersionInfo> results = new ArrayList<JsonSchemaVersionInfo>();
		for (int i = 0; i < NextPageToken.MAX_LIMIT + 1; i++) {
			results.add(new JsonSchemaVersionInfo());
		}
		when(mockSchemaDao.listSchemaVersions(any(), any(), anyLong(), anyLong())).thenReturn(results);
		// call under test
		ListJsonSchemaVersionInfoResponse response = manager.listSchemaVersions(listJsonSchemaVersionInfoRequest);
		assertNotNull(response);
		assertNotNull(response.getPage());
		assertEquals("50a50", response.getNextPageToken());
		long expectedLimit = 51;
		long expectedOffset = 0;
		verify(mockSchemaDao).listSchemaVersions(organizationName, schemaName, expectedLimit, expectedOffset);
	}

	@Test
	public void testListSchemaVersionsWithNullRequest() {
		listJsonSchemaVersionInfoRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.listSchemaVersions(listJsonSchemaVersionInfoRequest);
		});
	}

	@Test
	public void testListSchemaVersionsWithNullOrganizationName() {
		listJsonSchemaVersionInfoRequest.setOrganizationName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			manager.listSchemaVersions(listJsonSchemaVersionInfoRequest);
		});
	}

	@Test
	public void testListSchemaVersionsWithNullSchemaName() {
		listJsonSchemaVersionInfoRequest.setSchemaName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			manager.listSchemaVersions(listJsonSchemaVersionInfoRequest);
		});
	}

	@Test
	public void testGetLatestVersion() {
		String organizationName = "org";
		String schemaName = "one";
		String versionId = "111";
		when(mockSchemaDao.getLatestVersionId(any(), any())).thenReturn(versionId);
		JsonSchemaVersionInfo expected = new JsonSchemaVersionInfo();
		expected.setSchemaId("222");
		expected.setVersionId(versionId);
		when(mockSchemaDao.getVersionInfo(any())).thenReturn(expected);
		// call under test
		JsonSchemaVersionInfo actual = manager.getLatestVersion(organizationName, schemaName);
		assertEquals(expected, actual);
		verify(mockSchemaDao).getLatestVersionId(organizationName, schemaName);
		verify(mockSchemaDao).getVersionInfo(versionId);
	}

	@Test
	public void testGetLatestVersionWithNullOrganization() {
		String organizationName = null;
		String schemaName = "one";
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getLatestVersion(organizationName, schemaName);
		});
		verify(mockSchemaDao, never()).getLatestVersionId(any(), any());
		verify(mockSchemaDao, never()).getVersionInfo(any());
	}

	@Test
	public void testGetLatestVersionWithNullSchemaName() {
		String organizationName = "org";
		String schemaName = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getLatestVersion(organizationName, schemaName);
		});
		verify(mockSchemaDao, never()).getLatestVersionId(any(), any());
		verify(mockSchemaDao, never()).getVersionInfo(any());
	}

	@Test
	public void testGetValidationSchema() throws JSONObjectAdapterException {
		JsonSchema one = createSchema("one");
		one.set$schema("http://json-schema.org/draft-07/schema");
		one.setDescription("about one");
		Map<String, JsonSchema> oneDefintions = new HashMap<String, JsonSchema>();
		JsonSchema aString = new JsonSchema();
		aString.setType(Type.string);
		oneDefintions.put("foo-bar", aString);
		one.setDefinitions(oneDefintions);
		JsonSchema refToOne = create$RefSchema(one);
		JsonSchema two = createSchema("two");
		two.setItems(refToOne);
		JsonSchema refToTwo = create$RefSchema(two);
		JsonSchema three = createSchema("three");
		three.setItems(refToTwo);

		Mockito.doReturn(cloneSchema(one)).when(managerSpy).getSchema(one.get$id(), false);
		Mockito.doReturn(cloneSchema(two)).when(managerSpy).getSchema(two.get$id(), false);
		Mockito.doReturn(cloneSchema(three)).when(managerSpy).getSchema(three.get$id(), true);

		// call under test
		JsonSchema validationSchema = managerSpy.getValidationSchema(three.get$id());
		assertNotNull(validationSchema);
		Map<String, JsonSchema> validationDefinitions = validationSchema.getDefinitions();
		assertNotNull(validationDefinitions);
		assertEquals("three", validationSchema.get$id());
		assertNotNull(validationSchema.getItems());
		assertEquals("#/definitions/two", validationSchema.getItems().get$ref());
		// two fetched from three's Definitions
		JsonSchema twoFromDefinitions = validationDefinitions.get("two");
		assertNotNull(twoFromDefinitions);
		assertNotNull(twoFromDefinitions.getItems());
		// the $id of each schema in the definitions should be null. see PLFM-6366.
		assertNull(twoFromDefinitions.get$id());
		assertEquals("#/definitions/one", twoFromDefinitions.getItems().get$ref());
		assertNull(twoFromDefinitions.getDefinitions());
		// one fetched from three's Definitions
		JsonSchema oneFromDefinitions = validationDefinitions.get("one");
		assertNotNull(oneFromDefinitions);
		// the $id of each schema in the definitions should be null. see PLFM-6366.
		assertNull(oneFromDefinitions.get$id());
		assertEquals(one.get$schema(), oneFromDefinitions.get$schema());
		assertEquals(one.getDescription(), oneFromDefinitions.getDescription());
		assertNull(oneFromDefinitions.getDefinitions());
		// Definitions from one fetched form three's Definitions
		JsonSchema fooBar = validationDefinitions.get("foo-bar");
		assertNotNull(fooBar);
		assertEquals(Type.string, fooBar.getType());

		verify(managerSpy).getSchema(one.get$id(), false);
		verify(managerSpy).getSchema(two.get$id(), false);
		verify(managerSpy).getSchema(three.get$id(), true);
	}

	@Test
	public void testGetValidationSchemaWithLeafWithNullDefinitions() throws JSONObjectAdapterException {
		JsonSchema one = createSchema("one");
		one.setDescription("about one");
		one.setDefinitions(null);
		JsonSchema refToOne = create$RefSchema(one);
		JsonSchema two = createSchema("two");
		two.setItems(refToOne);

		Mockito.doReturn(cloneSchema(one)).when(managerSpy).getSchema(one.get$id(), false);
		Mockito.doReturn(cloneSchema(two)).when(managerSpy).getSchema(two.get$id(), true);

		// call under test
		JsonSchema validationSchema = managerSpy.getValidationSchema(two.get$id());
		assertNotNull(validationSchema);
		Map<String, JsonSchema> validationDefinitions = validationSchema.getDefinitions();
		assertNotNull(validationDefinitions);
		assertEquals("two", validationSchema.get$id());
		assertNotNull(validationSchema.getItems());
		assertEquals("#/definitions/one", validationSchema.getItems().get$ref());

		// one fetched from three's Definitions
		JsonSchema oneFromDefinitions = validationDefinitions.get("one");
		assertNotNull(oneFromDefinitions);
		assertEquals(one.getDescription(), oneFromDefinitions.getDescription());
		assertNull(oneFromDefinitions.getDefinitions());

		verify(managerSpy).getSchema(one.get$id(), false);
		verify(managerSpy).getSchema(two.get$id(), true);
	}

	@Test
	public void testGetValidationSchemaWithDuplicates() throws JSONObjectAdapterException {
		// one
		JsonSchema one = createSchema("one");
		one.setDescription("about one");
		one.setDefinitions(null);
		JsonSchema refToOne = create$RefSchema(one);
		// two
		JsonSchema two = createSchema("two");
		two.setItems(refToOne);
		JsonSchema refToTwo = create$RefSchema(two);
		// three
		JsonSchema three = createSchema("three");
		three.setItems(refToOne);
		three.setProperties(new LinkedHashMap<String, JsonSchema>());
		three.getProperties().put("threeRefToTwo", refToTwo);

		Mockito.doReturn(cloneSchema(one)).when(managerSpy).getSchema(one.get$id(), false);
		Mockito.doReturn(cloneSchema(two)).when(managerSpy).getSchema(two.get$id(), false);
		Mockito.doReturn(cloneSchema(three)).when(managerSpy).getSchema(three.get$id(), true);

		// call under test
		JsonSchema validationSchema = managerSpy.getValidationSchema(three.get$id());
		assertNotNull(validationSchema);
		Map<String, JsonSchema> validationDefinitions = validationSchema.getDefinitions();
		assertNotNull(validationDefinitions);
		assertEquals("three", validationSchema.get$id());
		assertNotNull(validationSchema.getItems());
		assertEquals("#/definitions/one", validationSchema.getItems().get$ref());
		assertNotNull(validationSchema.getProperties());
		assertEquals(1, validationSchema.getProperties().size());
		assertEquals("#/definitions/two", validationSchema.getProperties().get("threeRefToTwo").get$ref());
	}
	
	@Test
	public void testGetValidationSchemaWithNullId() {
		String $id = null;
		assertThrows(IllegalArgumentException.class, ()->{
			manager.getValidationSchema($id);
		});
	}

	@Test
	public void testGetValidationSchemaWithCycles() throws JSONObjectAdapterException {
		JsonSchema one = createSchema("one");
		one.setDescription("about one");
		JsonSchema refToOne = create$RefSchema(one);
		JsonSchema two = createSchema("two");
		two.setItems(refToOne);
		JsonSchema refToTwo = create$RefSchema(two);
		JsonSchema three = createSchema("three");
		three.setItems(refToTwo);
		JsonSchema refToThree = create$RefSchema(three);
		// one refs to three creates a cycle
		one.setItems(refToThree);

		Mockito.doReturn(cloneSchema(one)).when(managerSpy).getSchema(one.get$id(), false);
		Mockito.doReturn(cloneSchema(two)).when(managerSpy).getSchema(two.get$id(), false);
		Mockito.doReturn(cloneSchema(three)).when(managerSpy).getSchema(three.get$id(), true);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			managerSpy.getValidationSchema(three.get$id());
		}).getMessage();

		assertEquals("Schema $id: 'three' has a circular dependency", message);
	}

	@Test
	public void testGetValidationWithSchemaNoReferences() throws JSONObjectAdapterException {
		JsonSchema one = createSchema("one");
		one.setDescription("about one");

		Mockito.doReturn(cloneSchema(one)).when(managerSpy).getSchema(one.get$id(), true);

		// call under test
		JsonSchema validationSchema = managerSpy.getValidationSchema(one.get$id());
		assertNotNull(validationSchema);
		assertNull(validationSchema.getDefinitions());
		assertEquals("one", validationSchema.get$id());

		verify(managerSpy).getSchema(one.get$id(), true);
	}

	@Test
	public void testBindSchemaToObjectWithSemanticVersion() {
		String $id = organizationName + "-" + schemaName + "-" + semanticVersionString;
		when(mockSchemaDao.getSchemaId(any(), any())).thenReturn(schemaId);
		when(mockSchemaDao.getVersionId(any(), any(), any())).thenReturn(versionId);
		when(mockSchemaDao.bindSchemaToObject(any())).thenReturn(jsonSchemaObjectBinding);
		// call under test
		JsonSchemaObjectBinding binding = manager.bindSchemaToObject(adminUser.getId(), $id, objectId, objectType);
		assertNotNull(binding);
		assertEquals(jsonSchemaObjectBinding, binding);
		verify(mockSchemaDao).getSchemaId(organizationName, schemaName);
		verify(mockSchemaDao).getVersionId(organizationName, schemaName, semanticVersionString);
		verify(mockSchemaDao).bindSchemaToObject(new BindSchemaRequest().withCreatedBy(adminUser.getId())
				.withObjectId(objectId).withObjectType(objectType).withSchemaId(schemaId).withVersionId(versionId));
	}

	@Test
	public void testBindSchemaToObjectWithoutSemanticVersion() {
		String $id = organizationName + "-" + schemaName;
		when(mockSchemaDao.getSchemaId(any(), any())).thenReturn(schemaId);
		when(mockSchemaDao.bindSchemaToObject(any())).thenReturn(jsonSchemaObjectBinding);
		// call under test
		JsonSchemaObjectBinding binding = manager.bindSchemaToObject(adminUser.getId(), $id, objectId, objectType);
		assertNotNull(binding);
		assertEquals(jsonSchemaObjectBinding, binding);
		verify(mockSchemaDao).getSchemaId(organizationName, schemaName);
		verify(mockSchemaDao, never()).getVersionId(any(), any(), any());
		verify(mockSchemaDao).bindSchemaToObject(new BindSchemaRequest().withCreatedBy(adminUser.getId())
				.withObjectId(objectId).withObjectType(objectType).withSchemaId(schemaId).withVersionId(null));
	}

	@Test
	public void testBindSchemaToObjectWithNullUserId() {
		String $id = organizationName + "-" + schemaName;
		Long userId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.bindSchemaToObject(userId, $id, objectId, objectType);
		});
	}

	@Test
	public void testBindSchemaToObjectWithNull$id() {
		String $id = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.bindSchemaToObject(adminUser.getId(), $id, objectId, objectType);
		});
	}

	@Test
	public void testBindSchemaToObjectWithNullObjectId() {
		String $id = organizationName + "-" + schemaName;
		objectId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.bindSchemaToObject(adminUser.getId(), $id, objectId, objectType);
		});
	}

	@Test
	public void testBindSchemaToObjectWithNullObjectType() {
		String $id = organizationName + "-" + schemaName;
		objectType = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.bindSchemaToObject(adminUser.getId(), $id, objectId, objectType);
		});
	}

	/**
	 * Helper to create a schema with the given $id.
	 * 
	 * @param $id
	 * @return
	 */
	public JsonSchema createSchema(String $id) {
		JsonSchema schema = new JsonSchema();
		schema.set$id($id);
		return schema;
	}

	/**
	 * Helper to create a $ref to the given schema
	 * 
	 * @param toRef
	 * @return
	 */
	public JsonSchema create$RefSchema(JsonSchema toRef) {
		JsonSchema schema = new JsonSchema();
		schema.set$ref(toRef.get$id());
		return schema;
	}

	/**
	 * Helper to clone a JsonSchema
	 * 
	 * @param toClone
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public JsonSchema cloneSchema(JsonSchema toClone) throws JSONObjectAdapterException {
		String json = EntityFactory.createJSONStringForEntity(toClone);
		return EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
	}

	/**
	 * Convert a JsonSchema to json.
	 * 
	 * @param schema
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public static String toJson(JsonSchema schema) throws JSONObjectAdapterException {
		String json = EntityFactory.createJSONStringForEntity(schema);
		JSONObject object = new JSONObject(json);
		return object.toString(5);
	}
}
