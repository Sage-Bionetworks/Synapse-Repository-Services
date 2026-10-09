package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrapImpl.OBJECTS_TO_BOOTSTRAP;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AccessControlListManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.NormalizedJsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class SynapseSchemaBootstrapImplTest {

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@Mock
	private AccessControlListManager mockAclManager;

	@Mock
	private UserManager mockUserManager;

	@Mock
	SchemaTranslator mockTranslator;

	@InjectMocks
	private SynapseSchemaBootstrapImpl bootstrap;
	
	private SynapseSchemaBootstrapImpl bootstrapSpy;

	ObjectSchemaImpl objectSchema;

	String organizationName;
	Organization organziation;
	String schemaName;
	String jsonSHA256Hex;
	JsonSchemaVersionInfo versionInfo;
	JsonSchema jsonSchema;
	JsonSchema jsonSchemaTwo;
	UserInfo admin;
	
	ObjectSchema objectSchemaOne;
	ObjectSchema objectSchemaTwo;
	List<ObjectSchema> objectSchemas;

	@BeforeEach
	public void before() {
		bootstrapSpy = Mockito.spy(bootstrap);
		boolean isAdmin = true;
		admin = new UserInfo(isAdmin, 123L, AuthorizationConstants.DEFAULT_REALM_ID);
		organizationName = "org.sagebionetworks";
		
		organziation = new Organization();
		organziation.setName(organizationName);
		
		schemaName = "repo.model.Test.json";
		
		jsonSchema = new JsonSchema();
		jsonSchema.set$id(organizationName + "-" + schemaName);
		jsonSchema.setDescription("A test schema");
		
		NormalizedJsonSchema normal = new NormalizedJsonSchema(jsonSchema);
		
		jsonSHA256Hex = normal.getSha256Hex();
		versionInfo = new JsonSchemaVersionInfo();
		versionInfo.setOrganizationName(organizationName);
		versionInfo.setSchemaName(schemaName);
		versionInfo.setJsonSHA256Hex(jsonSHA256Hex);
		versionInfo.setSemanticVersion("1.0.25");
		
		objectSchemaOne = new ObjectSchemaImpl(TYPE.OBJECT);
		objectSchemaOne.setId("one");
		objectSchemaTwo = new ObjectSchemaImpl(TYPE.OBJECT);
		objectSchemaTwo.setId("two");
		
		objectSchemas = Lists.newArrayList(objectSchemaOne, objectSchemaTwo);
		jsonSchemaTwo = new JsonSchema();
		jsonSchemaTwo.set$id("two");
	}

	@Test
	public void testLoadAllSchemasAndReferences() {
		// One is a leaf
		ObjectSchemaImpl one = new ObjectSchemaImpl(TYPE.STRING);
		one.setId("one");

		ObjectSchemaImpl refToOne = new ObjectSchemaImpl();
		refToOne.setRef(one.getId());

		// two depends on one
		ObjectSchemaImpl two = new ObjectSchemaImpl(TYPE.ARRAY);
		two.setId("two");
		two.setItems(refToOne);

		ObjectSchemaImpl refToTwo = new ObjectSchemaImpl();
		refToTwo.setRef(two.getId());

		// Three depends on two
		ObjectSchemaImpl three = new ObjectSchemaImpl(TYPE.OBJECT);
		three.setId("three");
		three.setImplements(new ObjectSchemaImpl[] { refToTwo });

		// For depends on one
		ObjectSchemaImpl four = new ObjectSchemaImpl(TYPE.OBJECT);
		four.setId("four");
		four.setImplements(new ObjectSchemaImpl[] { refToOne });

		when(mockTranslator.loadSchemaFromClasspath(one.getId())).thenReturn(one);
		when(mockTranslator.loadSchemaFromClasspath(two.getId())).thenReturn(two);
		when(mockTranslator.loadSchemaFromClasspath(three.getId())).thenReturn(three);
		when(mockTranslator.loadSchemaFromClasspath(four.getId())).thenReturn(four);

		// loading three and four should trigger the load of all four.
		List<String> rootIds = Lists.newArrayList(three.getId(), four.getId());
		// call under test
		List<ObjectSchema> results = bootstrap.loadAllSchemasAndReferences(rootIds);
		assertNotNull(results);
		assertEquals(4, results.size());
		// Each dependency must come before the schema which depends on it.
		assertEquals(one, results.get(0));
		assertEquals(two, results.get(1));
		assertEquals(three, results.get(2));
		assertEquals(four, results.get(3));
	}

	/**
	 * Case of a dependency loop.
	 */
	@Test
	public void testLoadAllSchemasAndReferencesLoop() {
		// One is a leaf
		ObjectSchemaImpl one = new ObjectSchemaImpl(TYPE.STRING);
		one.setId("one");

		ObjectSchemaImpl refToOne = new ObjectSchemaImpl();
		refToOne.setRef(one.getId());

		// two depends on one
		ObjectSchemaImpl two = new ObjectSchemaImpl(TYPE.ARRAY);
		two.setId("two");
		two.setItems(refToOne);

		ObjectSchemaImpl refToTwo = new ObjectSchemaImpl();
		refToTwo.setRef(two.getId());

		// Three depends on two
		ObjectSchemaImpl three = new ObjectSchemaImpl(TYPE.OBJECT);
		three.setId("three");
		three.setImplements(new ObjectSchemaImpl[] { refToTwo });

		// One also depends on three creating a loop
		ObjectSchemaImpl refToThree = new ObjectSchemaImpl();
		refToThree.setRef(one.getId());
		one.setItems(refToThree);

		when(mockTranslator.loadSchemaFromClasspath(one.getId())).thenReturn(one);
		when(mockTranslator.loadSchemaFromClasspath(two.getId())).thenReturn(two);
		when(mockTranslator.loadSchemaFromClasspath(three.getId())).thenReturn(three);

		// loading three and four should trigger the load of all four.
		List<String> rootIds = Lists.newArrayList(three.getId());
		// call under test
		List<ObjectSchema> results = bootstrap.loadAllSchemasAndReferences(rootIds);
		assertNotNull(results);
		assertEquals(3, results.size());
		// Each dependency must come before the schema which depends on it.
		assertEquals(one, results.get(0));
		assertEquals(two, results.get(1));
		assertEquals(three, results.get(2));
	}

	/**
	 * When no version of a schema exist then the patch number is zero.
	 */
	@Test
	public void testGetNextPatchNumberIfNeededWithNotFound() {
		when(mockJsonSchemaManager.getLatestVersion(any(), any())).thenThrow(new NotFoundException("does not exist"));
		// call under test
		Optional<Long> patchNumberOptional = bootstrap.getNextPatchNumberIfNeeded(organizationName, schemaName,
				jsonSchema);
		assertTrue(patchNumberOptional.isPresent());
		assertEquals(new Long(0), patchNumberOptional.get());
		verify(mockJsonSchemaManager).getLatestVersion(organizationName, schemaName);
	}

	@Test
	public void testGetNextPatchNumberIfNeededWithVersionExistsAndHashMatches() {
		versionInfo.setJsonSHA256Hex(jsonSHA256Hex);
		versionInfo.setSemanticVersion("1.0.25");
		versionInfo.set$id(jsonSchema.get$id());
		when(mockJsonSchemaManager.getLatestVersion(any(), any())).thenReturn(versionInfo);
		// call under test
		Optional<Long> patchNumberOptional = bootstrap.getNextPatchNumberIfNeeded(organizationName, schemaName,
				jsonSchema);
		assertFalse(patchNumberOptional.isPresent());
		verify(mockJsonSchemaManager).getLatestVersion(organizationName, schemaName);
	}

	@Test
	public void testGetNextPatchNumberIfNeededWithVersionExistsAndHashDoesNotMatch() {
		versionInfo.setJsonSHA256Hex("wrongHash");
		versionInfo.setSemanticVersion("1.0.25");
		versionInfo.set$id(jsonSchema.get$id());
		when(mockJsonSchemaManager.getLatestVersion(any(), any())).thenReturn(versionInfo);
		// call under test
		Optional<Long> patchNumberOptional = bootstrap.getNextPatchNumberIfNeeded(organizationName, schemaName,
				jsonSchema);
		assertTrue(patchNumberOptional.isPresent());
		// patch number should be bumped by one.
		assertEquals(new Long(26), patchNumberOptional.get());
		verify(mockJsonSchemaManager).getLatestVersion(organizationName, schemaName);
	}

	@Test
	public void testRegisterSchemaIfDoesNotExistWithEmptyOptional() throws RecoverableMessageException {
		doReturn(Optional.empty()).when(bootstrapSpy).getNextPatchNumberIfNeeded(any(), any(), any());
		// Call under test
		bootstrapSpy.registerSchemaIfDoesNotExist(admin, jsonSchema);
		verify(bootstrapSpy).getNextPatchNumberIfNeeded(organizationName, schemaName, jsonSchema);
		// empty optional signals there is no work to do.
		verifyNoMoreInteractions(mockJsonSchemaManager);
	}
	
	@Test
	public void testRegisterSchemaIfDoesNotExistWithPresentOptional() throws RecoverableMessageException {
		// the patch number should be used in the ID to create the schema.
		doReturn(Optional.of(101L)).when(bootstrapSpy).getNextPatchNumberIfNeeded(any(), any(), any());
		// Call under test
		bootstrapSpy.registerSchemaIfDoesNotExist(admin, jsonSchema);
		verify(bootstrapSpy).getNextPatchNumberIfNeeded(organizationName, schemaName, jsonSchema);
		
		CreateSchemaRequest expected = new CreateSchemaRequest();
		JsonSchema clone = SchemaTestUtils.cloneJsonSchema(jsonSchema);
		clone.set$id("org.sagebionetworks-repo.model.Test.json-1.0.101");
		expected.setSchema(clone);
		verify(mockJsonSchemaManager).createJsonSchema(admin, expected);
	}
	
	@Test
	public void testCreateOrganizationIfDoesNotExist() {
		when(mockJsonSchemaManager.getOrganizationByName(any(), any())).thenReturn(organziation);
		doNothing().when(bootstrapSpy).grantAdminPermissions(any(), any(), any());

		// call under test
		assertEquals(organziation, bootstrapSpy.createOrganizationIfDoesNotExist(admin));

		verify(mockJsonSchemaManager).getOrganizationByName(admin, organizationName);
		// Finding the organization is not enough, because it can outlive its ACL and registering
		// the bootstrapped schemas needs CREATE on it.
		verify(bootstrapSpy).grantAdminPermissions(admin, organziation, admin.getId());
		verifyNoMoreInteractions(mockJsonSchemaManager);
	}

	@Test
	public void testCreateOrganizationIfDoesNotExistWithNotFound() {
		NotFoundException notFound = new NotFoundException("does not exist");
		when(mockJsonSchemaManager.getOrganizationByName(any(), any())).thenThrow(notFound);
		when(mockJsonSchemaManager.createOrganziation(any(), any(), any())).thenReturn(organziation);

		// call under test
		assertEquals(organziation, bootstrapSpy.createOrganizationIfDoesNotExist(admin));

		verify(mockJsonSchemaManager).getOrganizationByName(admin, organizationName);
		CreateOrganizationRequest expectedRequest = new CreateOrganizationRequest();
		expectedRequest.setOrganizationName(organizationName);
		verify(mockJsonSchemaManager).createOrganziation(admin, expectedRequest, SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ID);
		// Creating the organization also creates its ACL, so there is nothing to repair.
		verify(bootstrapSpy, never()).grantAdminPermissions(any(), any(), any());
	}

	@Test
	public void testCreateActOrganizationIfDoesNotExist() {
		Organization actOrganization = actOrganization();
		when(mockJsonSchemaManager.getOrganizationByName(any(), any())).thenReturn(actOrganization);
		doNothing().when(bootstrapSpy).grantAdminPermissions(any(), any(), any());

		// call under test
		assertEquals(actOrganization, bootstrapSpy.createActOrganizationIfDoesNotExist(admin));

		verify(mockJsonSchemaManager).getOrganizationByName(admin,
				SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT);
		verify(bootstrapSpy).grantActTeamAccess(admin, actOrganization);
		verify(bootstrapSpy).grantAdminPermissions(admin, actOrganization, TeamConstants.ACT_TEAM_ID);
		verify(bootstrapSpy).grantAdminPermissions(admin, actOrganization, admin.getId());
		verifyNoMoreInteractions(mockJsonSchemaManager);
	}

	@Test
	public void testCreateActOrganizationIfDoesNotExistWithNotFound() {
		Organization actOrganization = actOrganization();
		when(mockJsonSchemaManager.getOrganizationByName(any(), any()))
				.thenThrow(new NotFoundException("does not exist"));
		when(mockJsonSchemaManager.createOrganziation(any(), any(), any())).thenReturn(actOrganization);
		doNothing().when(bootstrapSpy).grantAdminPermissions(any(), any(), any());

		// call under test
		assertEquals(actOrganization, bootstrapSpy.createActOrganizationIfDoesNotExist(admin));

		CreateOrganizationRequest expectedRequest = new CreateOrganizationRequest()
				.setOrganizationName(SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT);
		// The id is pinned so that the organization lands on the same row on every stack.
		verify(mockJsonSchemaManager).createOrganziation(admin, expectedRequest,
				SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT_ID);
		verify(bootstrapSpy).grantActTeamAccess(admin, actOrganization);
		verify(bootstrapSpy).grantAdminPermissions(admin, actOrganization, TeamConstants.ACT_TEAM_ID);
		verify(bootstrapSpy, never()).grantAdminPermissions(admin, actOrganization, admin.getId());
	}

	@Test
	public void testGrantActTeamAccess() {
		AccessControlList acl = new AccessControlList().setId("8").setResourceAccess(new HashSet<>(
				Set.of(AccessControlListUtil.createResourceAccess(admin.getId(), ACCESS_TYPE.READ))));
		when(mockAclManager.getAcl(any(), any())).thenReturn(Optional.of(acl));

		// call under test
		bootstrap.grantActTeamAccess(admin, actOrganization());

		AccessControlList expected = new AccessControlList().setId("8").setResourceAccess(Set.of(
				AccessControlListUtil.createResourceAccess(admin.getId(), ACCESS_TYPE.READ),
				actAdminAccess()));
		verify(mockAclManager).getAcl("8", ObjectType.ORGANIZATION);
		verify(mockAclManager).update(admin, expected, ObjectType.ORGANIZATION, admin.getId());
	}

	@Test
	public void testGrantActTeamAccessWithAccessAlreadyGranted() {
		AccessControlList acl = new AccessControlList().setId("8").setResourceAccess(new HashSet<>(Set.of(
				AccessControlListUtil.createResourceAccess(admin.getId(), ACCESS_TYPE.READ),
				actAdminAccess())));
		when(mockAclManager.getAcl(any(), any())).thenReturn(Optional.of(acl));

		// call under test
		bootstrap.grantActTeamAccess(admin, actOrganization());

		// Running the bootstrap again must not rotate the etag of an ACL that is already correct.
		verify(mockAclManager, never()).update(any(), any(), any(), any());
		verify(mockAclManager, never()).create(any(), any(), any(), any());
	}

	@Test
	public void testGrantActTeamAccessWithNarrowerAccessGranted() {
		AccessControlList acl = new AccessControlList().setId("8").setResourceAccess(new HashSet<>(Set.of(
				AccessControlListUtil.createResourceAccess(TeamConstants.ACT_TEAM_ID, ACCESS_TYPE.READ))));
		when(mockAclManager.getAcl(any(), any())).thenReturn(Optional.of(acl));

		// call under test
		bootstrap.grantActTeamAccess(admin, actOrganization());

		AccessControlList expected = new AccessControlList().setId("8")
				.setResourceAccess(Set.of(actAdminAccess()));
		verify(mockAclManager).update(admin, expected, ObjectType.ORGANIZATION, admin.getId());
	}

	@Test
	public void testGrantActTeamAccessWithoutAcl() {
		when(mockAclManager.getAcl(any(), any())).thenReturn(Optional.empty());

		// call under test
		bootstrap.grantActTeamAccess(admin, actOrganization());

		// The bootstrap cannot read the ACL through a permission check, so an organization left
		// without an ACL has to be repaired here rather than aborting the start of the stack.
		ArgumentCaptor<AccessControlList> captor = ArgumentCaptor.forClass(AccessControlList.class);
		verify(mockAclManager).create(eq(admin), captor.capture(), eq(ObjectType.ORGANIZATION),
				eq(admin.getId()));
		verify(mockAclManager, never()).update(any(), any(), any(), any());
		assertEquals("8", captor.getValue().getId());
		assertEquals(Set.of(AccessControlListUtil.createResourceAccess(admin.getId(),
				JsonSchemaManagerImpl.ADMIN_PERMISSIONS.toArray(new ACCESS_TYPE[0])), actAdminAccess()),
				captor.getValue().getResourceAccess());
	}

	@Test
	public void testGrantAdminPermissionsWithAclMissingAdminEntry() {
		AccessControlList acl = new AccessControlList().setId("7").setResourceAccess(new HashSet<>(Set.of(
				AccessControlListUtil.createResourceAccess(TeamConstants.ACT_TEAM_ID, ACCESS_TYPE.READ))));
		when(mockAclManager.getAcl(any(), any())).thenReturn(Optional.of(acl));
		Organization organization = new Organization().setId("7").setCreatedBy(admin.getId().toString())
				.setName(SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS);

		// call under test
		bootstrap.grantAdminPermissions(admin, organization, admin.getId());

		// Registering the bootstrapped schemas needs CREATE on the organization, so an ACL that
		// lost the admin's entry has to be repaired rather than failing the start of the stack.
		AccessControlList expected = new AccessControlList().setId("7").setResourceAccess(Set.of(
				AccessControlListUtil.createResourceAccess(TeamConstants.ACT_TEAM_ID, ACCESS_TYPE.READ),
				AccessControlListUtil.createResourceAccess(admin.getId(),
						JsonSchemaManagerImpl.ADMIN_PERMISSIONS.toArray(new ACCESS_TYPE[0]))));
		verify(mockAclManager).getAcl("7", ObjectType.ORGANIZATION);
		verify(mockAclManager).update(admin, expected, ObjectType.ORGANIZATION, admin.getId());
	}

	/**
	 * The ACT organization as the bootstrap finds it, created by the admin that runs the bootstrap.
	 */
	private Organization actOrganization() {
		return new Organization().setId("8").setCreatedBy(admin.getId().toString())
				.setName(SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT);
	}

	@Test
	public void testLoadAccessRequirementBaseSchema() {
		JsonSchema baseSchema = new JsonSchema()
				.set$id(JsonSchemaConstants.ACCESS_REQUIREMENT_BASE_SCHEMA_ID);
		when(mockTranslator.loadJsonSchemaFromClasspath(
				SynapseSchemaBootstrapImpl.ACCESS_REQUIREMENT_BASE_SCHEMA_FILE)).thenReturn(baseSchema);

		// call under test
		JsonSchema loaded = bootstrap.loadAccessRequirementBaseSchema();

		assertEquals(baseSchema, loaded);
	}

	@Test
	public void testBootstrapAccessRequirementBaseSchema() {
		JsonSchema baseSchema = new JsonSchema()
				.set$id(JsonSchemaConstants.ACCESS_REQUIREMENT_BASE_SCHEMA_ID);
		doReturn(new Organization()).when(bootstrapSpy).createOrganizationIfDoesNotExist(any());
		doReturn(new Organization()).when(bootstrapSpy).createActOrganizationIfDoesNotExist(any());
		doReturn(baseSchema).when(bootstrapSpy).loadAccessRequirementBaseSchema();
		doNothing().when(bootstrapSpy).registerSchemaIfDoesNotExist(any(), any());

		// call under test
		bootstrapSpy.bootstrapAccessRequirementBaseSchema(admin);

		// The base schema belongs to 'org.sagebionetworks', so its organization has to be created
		// here too: nothing orders this bootstrapper after the one that otherwise creates it.
		InOrder order = Mockito.inOrder(bootstrapSpy);
		order.verify(bootstrapSpy).createOrganizationIfDoesNotExist(admin);
		order.verify(bootstrapSpy).registerSchemaIfDoesNotExist(admin, baseSchema);
		verify(bootstrapSpy).createActOrganizationIfDoesNotExist(admin);
	}

	@Test
	public void testBootstrapSynapseSchemas() throws RecoverableMessageException {
		JsonSchema baseSchema = new JsonSchema()
				.set$id(JsonSchemaConstants.ACCESS_REQUIREMENT_BASE_SCHEMA_ID);
		when(mockUserManager.getUserInfo(any())).thenReturn(admin);
		doReturn(objectSchemas).when(bootstrapSpy).loadAllSchemasAndReferences(any());
		when(mockTranslator.translate(any())).thenReturn(jsonSchema, jsonSchemaTwo);
		doNothing().when(bootstrapSpy).registerSchemaIfDoesNotExist(any(),any());
		doReturn(new Organization()).when(bootstrapSpy).createOrganizationIfDoesNotExist(any());
		doReturn(new Organization()).when(bootstrapSpy).createActOrganizationIfDoesNotExist(any());
		doReturn(baseSchema).when(bootstrapSpy).loadAccessRequirementBaseSchema();
		doNothing().when(bootstrapSpy).replaceReferencesWithLatestVersion(any());
		// call under test
		bootstrapSpy.bootstrapSynapseSchemas();
		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// Creating the organization is idempotent, and both this method and the base schema
		// bootstrap need it in place, so each asks for it independently.
		verify(bootstrapSpy, atLeastOnce()).createOrganizationIfDoesNotExist(admin);
		verify(bootstrapSpy).createActOrganizationIfDoesNotExist(admin);
		verify(mockTranslator).translate(objectSchemaOne);
		verify(mockTranslator).translate(objectSchemaTwo);
		verify(bootstrapSpy).registerSchemaIfDoesNotExist(admin, jsonSchema);
		verify(bootstrapSpy).registerSchemaIfDoesNotExist(admin, jsonSchemaTwo);
		verify(bootstrapSpy).registerSchemaIfDoesNotExist(admin, baseSchema);
		verify(bootstrapSpy).replaceReferencesWithLatestVersion(jsonSchema);
		verify(bootstrapSpy).replaceReferencesWithLatestVersion(jsonSchemaTwo);
		// The base schema is hand written rather than translated, so it is registered as it was loaded.
		verify(bootstrapSpy, never()).replaceReferencesWithLatestVersion(baseSchema);
	}

	@Test
	public void testReplaceReferencesWithNoSubSchema() {
		//call under test
		bootstrap.replaceReferencesWithLatestVersion(jsonSchema);
		verify(mockJsonSchemaManager, never()).getLatestVersion(any(), any());
	}
	
	@Test
	public void testReplaceReferencesWithSubSchemaWithoutReferences() {
		JsonSchema subSchema = new JsonSchema();
		subSchema.set$ref(null);
		subSchema.setDescription("not a $ref");
		// add a sub schema
		jsonSchema.setAllOf(Lists.newArrayList(subSchema));
		//call under test
		bootstrap.replaceReferencesWithLatestVersion(jsonSchema);
		verify(mockJsonSchemaManager, never()).getLatestVersion(any(), any());;
	}
	
	@Test
	public void testReplaceReferencesWithLatestVersionWithSubSchemaWithRefWithVersion() {
		String sub$ref = "org-sub.name-1.0.1";
		JsonSchema subSchema = new JsonSchema();
		subSchema.set$ref(sub$ref);
		// add a sub schema
		jsonSchema.setAllOf(Lists.newArrayList(subSchema));
		//call under test
		bootstrap.replaceReferencesWithLatestVersion(jsonSchema);
		verify(mockJsonSchemaManager, never()).getLatestVersion(any(), any());;
	}
	
	@Test
	public void testReplaceReferencesWithLatestVersion() {
		String sub$ref = "org-sub.name";
		JsonSchema subSchema = new JsonSchema();
		subSchema.set$ref(sub$ref);
		JsonSchemaVersionInfo versionInfo = new JsonSchemaVersionInfo();
		String latestSub$id = sub$ref+"-1.0.1";
		versionInfo.set$id(latestSub$id);
		when(mockJsonSchemaManager.getLatestVersion(any(), any())).thenReturn(versionInfo);
		// add a sub schema
		jsonSchema.setAllOf(Lists.newArrayList(subSchema));
		//call under test
		bootstrap.replaceReferencesWithLatestVersion(jsonSchema);
		verify(mockJsonSchemaManager).getLatestVersion("org", "sub.name");
		// the $ref should be change to match the latest version
		assertEquals(jsonSchema.getAllOf().get(0).get$ref(), versionInfo.get$id());
	}


	/**
	 * The entry the bootstrap must leave on the ACL of the ACT organization.
	 */
	private static ResourceAccess actAdminAccess() {
		return new ResourceAccess().setPrincipalId(TeamConstants.ACT_TEAM_ID)
				.setAccessType(new HashSet<>(JsonSchemaManagerImpl.ADMIN_PERMISSIONS));
	}

	@Test
	public void testBootstrapSchemasForAllEntityTypes() {
		EntityType[] entityTypes = EntityType.values();
		for(EntityType type: entityTypes) {
			Class<? extends Entity> clazz = EntityTypeUtils.getClassForType(type);
			// Make sure the entity type has a schema in the bootstrap list
			assertTrue(OBJECTS_TO_BOOTSTRAP.contains(clazz.getName()));
		}
	}

}
