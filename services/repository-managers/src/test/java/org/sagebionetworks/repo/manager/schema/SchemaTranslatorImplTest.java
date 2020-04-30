package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@ExtendWith(MockitoExtension.class)
public class SchemaTranslatorImplTest {

	@InjectMocks
	SchemaTranslatorImp translator;
	
	@Test
	public void testLoadSchemaFromClasspath() throws IOException, JSONObjectAdapterException {
		String fileEntityId = FileEntity.class.getName();
		// call under test
		ObjectSchemaImpl schema = translator.loadSchemaFromClasspath(fileEntityId);
		assertNotNull(schema);
		assertEquals(fileEntityId, schema.getId());
	}
	
	@Test
	public void testLoadSchemaFromClasspathWithNotFound() throws IOException, JSONObjectAdapterException {
		String id = "does.not.exist";
		String message = assertThrows(NotFoundException.class, () -> {
			translator.loadSchemaFromClasspath(id);
		}).getMessage();
		assertEquals("Cannot find: 'schema/does/not/exist.json' on the classpath", message);
	}

	@Test
	public void testLoadSchemaFromClasspathWithNullId() {
		String id = null;
		assertThrows(IllegalArgumentException.class, () -> {
			translator.loadSchemaFromClasspath(id);
		});
	}

	@Test
	public void testConvertFromInternalIdToExternalId() {
		String internalId = "org.sagebionetworks.repo.model.FileEntity";
		// call under test
		String externalId = translator.convertFromInternalIdToExternalId(internalId);
		assertEquals("org.sagebionetworks/repo.model.FileEntity", externalId);
	}

	@Test
	public void testConvertFromInternalIdToExternalIdWithUknownOrganization() {
		String internalId = "org.unknown.repo.model.FileEntity";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			translator.convertFromInternalIdToExternalId(internalId);
		}).getMessage();
		assertEquals("Id has an unknown organization name: 'org.unknown.repo.model.FileEntity'", message);
	}

	@Test
	public void testConvertFromInternalIdToExternalIdWithNullId() {
		String internalId = null;
		// call under test
		String externalId = translator.convertFromInternalIdToExternalId(internalId);
		assertNull(externalId);
	}
	
	@Test
	public void testTranslateSchemaArray() {
		ObjectSchemaImpl one = new ObjectSchemaImpl();
		one.setId("org.sagebionetworks.one");
		ObjectSchemaImpl two = new ObjectSchemaImpl();
		two.setId("org.sagebionetworks.two");
		ObjectSchema[] array = new ObjectSchema[] {one, two};
		// Call under test
		List<JsonSchema> result = translator.translateSchema(array);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("org.sagebionetworks/one", result.get(0).get$id());
		assertEquals("org.sagebionetworks/two", result.get(1).get$id());
	}
	
	@Test
	public void testTranslateSchemaArrayWithNullArray() {
		ObjectSchema[] array = null;
		// Call under test
		List<JsonSchema> result = translator.translateSchema(array);
		assertNull(result);
	}

	@Test
	public void testTranslateSchema() throws IOException, JSONObjectAdapterException {
		ObjectSchemaImpl fileEntityObjecSchema = translator.loadSchemaFromClasspath("org.sagebionetworks.repo.model.FileEntity");
		// Call under test
		JsonSchema resultSchema = translator.translate(fileEntityObjecSchema);
		assertNotNull(resultSchema);
		assertEquals("org.sagebionetworks/repo.model.FileEntity", resultSchema.get$id());
	}
	
	@Test
	public void testTranslateSchemaWith$ref() throws IOException, JSONObjectAdapterException {
		ObjectSchemaImpl objectSchema = new ObjectSchemaImpl();
		objectSchema.setRef("org.sagebionetworks.repo.model.FileEntity");
		// Call under test
		JsonSchema resultSchema = translator.translate(objectSchema);
		assertNotNull(resultSchema);
		assertEquals("org.sagebionetworks/repo.model.FileEntity", resultSchema.get$ref());
	}
	
	@Test
	public void testTranslateSchemaWithImplments() throws IOException, JSONObjectAdapterException {
		ObjectSchemaImpl objectSchema = new ObjectSchemaImpl();
		objectSchema.setId("org.sagebionetworks.repo.model.FileEntity");
		ObjectSchemaImpl imp = new ObjectSchemaImpl();
		imp.setRef("org.sagebionetworks.repo.model.Versionable");
		objectSchema.setImplements(new ObjectSchema[] {imp});
		
		// Call under test
		JsonSchema resultSchema = translator.translate(objectSchema);
		assertNotNull(resultSchema);
		assertEquals("org.sagebionetworks/repo.model.FileEntity", resultSchema.get$id());
		assertNotNull(resultSchema.getAllOf());
		assertEquals(1, resultSchema.getAllOf().size());
		assertEquals("org.sagebionetworks/repo.model.Versionable", resultSchema.getAllOf().get(0).get$ref());
	}
	

}
