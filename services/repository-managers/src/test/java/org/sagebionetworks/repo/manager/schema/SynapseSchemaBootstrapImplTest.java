package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.NormalizedJsonSchema;
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
	private UserManager mockUserManager;

	@Mock
	SchemaTranslator mockTranslator;

	@InjectMocks
	private SynapseSchemaBootstrapImpl bootstrap;
	
	private SynapseSchemaBootstrapImpl bootstrapSpy;

	ObjectSchemaImpl objectSchema;

	String organizationName;
	String schemaName;
	String jsonSHA256Hex;
	JsonSchemaVersionInfo versionInfo;
	JsonSchema jsonSchema;
	UserInfo admin;

	@BeforeEach
	public void before() {
		bootstrapSpy = Mockito.spy(bootstrap);
		boolean isAdmin = true;
		admin = new UserInfo(isAdmin, 123L);
		organizationName = "org.sagebionetworks";
		schemaName = "repo.model.Test.json";
		
		jsonSchema = new JsonSchema();
		jsonSchema.set$id(organizationName + "/" + schemaName);
		jsonSchema.setDescription("A test schema");
		
		NormalizedJsonSchema normal = new NormalizedJsonSchema(jsonSchema);
		
		jsonSHA256Hex = normal.getSha256Hex();
		versionInfo = new JsonSchemaVersionInfo();
		versionInfo.setOrganizationName(organizationName);
		versionInfo.setSchemaName(schemaName);
		versionInfo.setJsonSHA256Hex(jsonSHA256Hex);
		versionInfo.setSemanticVersion("1.0.25");
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
				jsonSHA256Hex);
		assertTrue(patchNumberOptional.isPresent());
		assertEquals(new Long(0), patchNumberOptional.get());
		verify(mockJsonSchemaManager).getLatestVersion(organizationName, schemaName);
	}

	@Test
	public void testGetNextPatchNumberIfNeededWithVersionExistsAndHashMatches() {
		versionInfo.setJsonSHA256Hex(jsonSHA256Hex);
		versionInfo.setSemanticVersion("1.0.25");
		when(mockJsonSchemaManager.getLatestVersion(any(), any())).thenReturn(versionInfo);
		// call under test
		Optional<Long> patchNumberOptional = bootstrap.getNextPatchNumberIfNeeded(organizationName, schemaName,
				jsonSHA256Hex);
		assertFalse(patchNumberOptional.isPresent());
		verify(mockJsonSchemaManager).getLatestVersion(organizationName, schemaName);
	}

	@Test
	public void testGetNextPatchNumberIfNeededWithVersionExistsAndHashDoesNotMatch() {
		versionInfo.setJsonSHA256Hex("wrongHash");
		versionInfo.setSemanticVersion("1.0.25");
		when(mockJsonSchemaManager.getLatestVersion(any(), any())).thenReturn(versionInfo);
		// call under test
		Optional<Long> patchNumberOptional = bootstrap.getNextPatchNumberIfNeeded(organizationName, schemaName,
				jsonSHA256Hex);
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
		verify(bootstrapSpy).getNextPatchNumberIfNeeded(organizationName, schemaName, jsonSHA256Hex);
		// empty optional signals there is no work to do.
		verifyZeroInteractions(mockJsonSchemaManager);
	}
	
	@Test
	public void testRegisterSchemaIfDoesNotExistWithPresentOptional() throws RecoverableMessageException {
		// the patch number should be used in the ID to create the schema.
		doReturn(Optional.of(101L)).when(bootstrapSpy).getNextPatchNumberIfNeeded(any(), any(), any());
		// Call under test
		bootstrapSpy.registerSchemaIfDoesNotExist(admin, jsonSchema);
		verify(bootstrapSpy).getNextPatchNumberIfNeeded(organizationName, schemaName, jsonSHA256Hex);
		
		CreateSchemaRequest expected = new CreateSchemaRequest();
		jsonSchema.set$id("org.sagebionetworks/repo.model.Test.json/1.0.101");
		expected.setSchema(jsonSchema);
		verify(mockJsonSchemaManager).createJsonSchema(admin, expected);
	}

}
