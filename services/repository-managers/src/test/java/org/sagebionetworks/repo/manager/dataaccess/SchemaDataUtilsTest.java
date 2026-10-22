package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;

public class SchemaDataUtilsTest {

	private static JsonSchema fileHandle() {
		return new JsonSchema().setType(Type.integer).setFormat(FormTemplateValidator.FILE_HANDLE_FORMAT);
	}

	private static JsonSchema object(Map<String, JsonSchema> properties) {
		return new JsonSchema().setType(Type.object).setProperties(properties);
	}

	@Test
	public void testFindFileHandleIdsWithTopLevelAndNestedProperties() {
		JsonSchema schema = object(Map.of("signedDuc", fileHandle(), "yearsOfAccess",
				new JsonSchema().setType(Type.integer), "irb", object(Map.of("approval", fileHandle()))));
		JSONObject document = new JSONObject("{\"signedDuc\":11,\"yearsOfAccess\":2,\"irb\":{\"approval\":12}}");

		// call under test
		List<String> ids = SchemaDataUtils.findFileHandleIds(schema, document);

		// A number the schema does not declare a file handle is not one
		assertEquals(List.of("11", "12"), sorted(ids));
	}

	@Test
	public void testFindFileHandleIdsWithArrayItems() {
		JsonSchema schema = object(Map.of("attachments", new JsonSchema().setType(Type.array).setItems(fileHandle())));
		JSONObject document = new JSONObject("{\"attachments\":[21,22,21]}");

		// call under test
		List<String> ids = SchemaDataUtils.findFileHandleIds(schema, document);

		assertEquals(List.of("21", "22"), ids);
	}

	@Test
	public void testFindFileHandleIdsWithDefinitionReference() {
		JsonSchema schema = object(Map.of("document", new JsonSchema().set$ref("#/definitions/org.example-Document")))
				.setDefinitions(Map.of("org.example-Document", object(Map.of("fileHandleId", fileHandle()))));
		JSONObject document = new JSONObject("{\"document\":{\"fileHandleId\":31}}");

		// call under test
		List<String> ids = SchemaDataUtils.findFileHandleIds(schema, document);

		assertEquals(List.of("31"), ids);
	}

	@Test
	public void testFindFileHandleIdsWithConditionalBranches() {
		// Every branch is followed whether or not the document satisfies it, so that a client can not
		// avoid the access check by choosing which branch its answers take
		JsonSchema schema = object(Map.of("renewalLetter", new JsonSchema().setType(Type.integer)))
				.set_if(object(Map.of("kind", new JsonSchema().set_const("renewal"))))
				.setThen(object(Map.of("renewalLetter", fileHandle())))
				.setAllOf(List.of(object(Map.of("other", fileHandle()))));
		JSONObject document = new JSONObject("{\"kind\":\"request\",\"renewalLetter\":41,\"other\":42}");

		// call under test
		List<String> ids = SchemaDataUtils.findFileHandleIds(schema, document);

		assertEquals(List.of("41", "42"), sorted(ids));
	}

	@Test
	public void testFindFileHandleIdsWithAdditionalProperties() {
		JsonSchema schema = object(Map.of("known", new JsonSchema().setType(Type.string)))
				.setAdditionalProperties(fileHandle());
		JSONObject document = new JSONObject("{\"known\":\"51\",\"extra\":52}");

		// call under test
		List<String> ids = SchemaDataUtils.findFileHandleIds(schema, document);

		assertEquals(List.of("52"), ids);
	}

	@Test
	public void testFindFileHandleIdsWithValuesThatCanNotNameAFileHandle() {
		// A draft is not validated, so a file handle property can hold anything
		JsonSchema schema = object(Map.of("a", fileHandle(), "b", fileHandle(), "c", fileHandle(), "d", fileHandle(),
				"e", fileHandle(), "f", fileHandle(), "g", fileHandle()));
		JSONObject document = new JSONObject(
				"{\"a\":\"abc\",\"b\":1.5,\"c\":-3,\"d\":true,\"e\":null,\"f\":\"61\",\"g\":62.0}");

		// call under test
		List<String> ids = SchemaDataUtils.findFileHandleIds(schema, document);

		assertEquals(List.of("61", "62"), sorted(ids));
	}

	@Test
	public void testFindFileHandleIdsWithReferenceCycle() {
		// A definition that refers back to itself for the same value must not recurse forever
		JsonSchema schema = new JsonSchema().set$ref("#/definitions/loop")
				.setDefinitions(Map.of("loop", new JsonSchema().setAllOf(List.of(
						new JsonSchema().set$ref("#/definitions/loop"), object(Map.of("file", fileHandle()))))));
		JSONObject document = new JSONObject("{\"file\":71}");

		// call under test
		List<String> ids = SchemaDataUtils.findFileHandleIds(schema, document);

		assertEquals(List.of("71"), ids);
	}

	private static List<String> sorted(List<String> ids) {
		return ids.stream().sorted().toList();
	}
}
