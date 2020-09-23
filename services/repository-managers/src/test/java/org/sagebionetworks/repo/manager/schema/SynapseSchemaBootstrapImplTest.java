package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.NormalizedJsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class SynapseSchemaBootstrapImplTest {

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

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
		admin = new UserInfo(isAdmin, 123L);
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
		verifyZeroInteractions(mockJsonSchemaManager);
	}
	
	@Test
	public void testRegisterSchemaIfDoesNotExistWithPresentOptional() throws RecoverableMessageException {
		// the patch number should be used in the ID to create the schema.
		doReturn(Optional.of(101L)).when(bootstrapSpy).getNextPatchNumberIfNeeded(any(), any(), any());
		// Call under test
		bootstrapSpy.registerSchemaIfDoesNotExist(admin, jsonSchema);
		verify(bootstrapSpy).getNextPatchNumberIfNeeded(organizationName, schemaName, jsonSchema);
		
		CreateSchemaRequest expected = new CreateSchemaRequest();
		JsonSchema clone = cloneJsonSchema(jsonSchema);
		clone.set$id("org.sagebionetworks-repo.model.Test.json-1.0.101");
		expected.setSchema(clone);
		verify(mockJsonSchemaManager).createJsonSchema(admin, expected);
	}
	
	@Test
	public void testCreateOrganizationIfDoesNotExist() {
		when(mockJsonSchemaManager.getOrganizationByName(any(), any())).thenReturn(organziation);
		// call under test
		bootstrap.createOrganizationIfDoesNotExist(admin);
		verify(mockJsonSchemaManager).getOrganizationByName(admin, organizationName);
		verifyZeroInteractions(mockJsonSchemaManager);
	}
	
	@Test
	public void testCreateOrganizationIfDoesNotExistWithNotFound() {
		NotFoundException notFound = new NotFoundException("does not exist");
		when(mockJsonSchemaManager.getOrganizationByName(any(), any())).thenThrow(notFound);
		// call under test
		bootstrap.createOrganizationIfDoesNotExist(admin);
		verify(mockJsonSchemaManager).getOrganizationByName(admin, organizationName);
		CreateOrganizationRequest expectedRequest = new CreateOrganizationRequest();
		expectedRequest.setOrganizationName(organizationName);
		verify(mockJsonSchemaManager).createOrganziation(admin, expectedRequest);
	}
	
	@Test
	public void testBootstrapSynapseSchemas() throws RecoverableMessageException {
		when(mockUserManager.getUserInfo(any())).thenReturn(admin);
		doReturn(objectSchemas).when(bootstrapSpy).loadAllSchemasAndReferences(any());
		when(mockTranslator.translate(any())).thenReturn(jsonSchema, jsonSchemaTwo);
		doNothing().when(bootstrapSpy).registerSchemaIfDoesNotExist(any(),any());
		doNothing().when(bootstrapSpy).createOrganizationIfDoesNotExist(any());
		doNothing().when(bootstrapSpy).replaceReferencesWithLatestVersion(any());
		// call under test
		bootstrapSpy.bootstrapSynapseSchemas();
		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		verify(bootstrapSpy).createOrganizationIfDoesNotExist(admin);
		verify(mockTranslator).translate(objectSchemaOne);
		verify(mockTranslator).translate(objectSchemaTwo);
		verify(bootstrapSpy).registerSchemaIfDoesNotExist(admin, jsonSchema);
		verify(bootstrapSpy).registerSchemaIfDoesNotExist(admin, jsonSchemaTwo);
		verify(bootstrapSpy).replaceReferencesWithLatestVersion(jsonSchema);
		verify(bootstrapSpy).replaceReferencesWithLatestVersion(jsonSchemaTwo);
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

	public JsonSchema cloneJsonSchema(JsonSchema schema) {
		try {
			String json = EntityFactory.createJSONStringForEntity(schema);
			return EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
}
