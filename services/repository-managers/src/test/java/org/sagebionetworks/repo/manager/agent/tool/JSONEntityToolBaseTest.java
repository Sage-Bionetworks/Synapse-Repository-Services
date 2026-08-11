package org.sagebionetworks.repo.manager.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class JSONEntityToolBaseTest {

	private ExampleJSONEntityTool tool;

	@BeforeEach
	public void before() {
		tool = new ExampleJSONEntityTool();
	}

	private ToolCallback callback(String name) {
		return tool.getToolCallbacks().stream().filter(c -> name.equals(c.getToolDefinition().name())).findFirst()
				.orElseThrow();
	}

	@Test
	public void testGetToolCallbacks() {
		assertNotNull(tool.getToolCallbacks());
		assertEquals(5, tool.getToolCallbacks().size());
	}

	@Test
	public void testToolDefinitionUsesAnnotationName() {
		// The @JSONEntityTool name() overrides the method name when provided.
		ToolCallback callback = callback("get_entity_file_handle");
		assertEquals("This is the method description", callback.getToolDefinition().description());
		assertTrue(callback.getToolMetadata().returnDirect());
	}

	@Test
	public void testToolDefinitionFallsBackToMethodName() {
		// When name() is blank, the method name is used and returnDirect defaults to false.
		ToolCallback callback = callback("getRawPayload");
		assertNotNull(callback);
		assertFalse(callback.getToolMetadata().returnDirect());
	}

	@Test
	public void testInputSchemaGeneratedFromParameterType() {
		String inputSchema = callback("get_entity_file_handle").getToolDefinition().inputSchema();
		JSONObject schema = new JSONObject(inputSchema);

		// The schema is generated from the declared Entity parameter type, seeded with its concrete
		// implementers, so the polymorphic union appears as oneOf under $defs.
		assertTrue(schema.has("$defs"));
		assertTrue(inputSchema.contains("oneOf"));
		assertTrue(schema.getJSONObject("$defs").has("org.sagebionetworks.repo.model.Folder"));
		// The @JSONEntityToolParam description is surfaced on the root schema node the model reads.
		assertEquals("this is the parameter description", schema.getString("description"));
	}

	@Test
	public void testCallDeserializesTypedArgumentAndInjectsContext() {
		Folder folder = new Folder().setId("syn123").setName("my-folder");
		String toolInput = JDOSecondaryPropertyUtils.createJSONFromObject(folder);
		ToolContext context = new ToolContext(Map.of("key", "value"));

		// call under test
		String result = callback("get_entity_file_handle").call(toolInput, context);

		// The concreteType-aware path reconstructs the correct Entity subtype.
		assertEquals(folder, tool.getEntity());
		assertEquals(context, tool.getContext());
		// The ToolResponse is serialized via the JSONEntity path.
		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(new S3FileHandle().setId("123"))),
				result);
	}

	@Test
	public void testCallWithRawStringSchemaTypePassesPayloadThrough() {
		Folder folder = new Folder().setId("syn999").setName("raw");
		String toolInput = JDOSecondaryPropertyUtils.createJSONFromObject(folder);

		// call under test — the raw payload reaches the tool untouched (not round-tripped through a POJO).
		String result = callback("getRawPayload").call(toolInput, null);

		assertEquals(toolInput, tool.getRawPayload());
		assertNull(tool.getEntity());
		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(new S3FileHandle().setId("456"))),
				result);
	}

	@Test
	public void testRawStringSchemaTypeInputSchemaFromSchemaType() {
		// Even though the parameter is a raw String, the input schema is generated from schemaType=Entity.
		String inputSchema = callback("getRawPayload").getToolDefinition().inputSchema();
		JSONObject schema = new JSONObject(inputSchema);
		assertTrue(schema.has("$defs"));
		assertTrue(schema.getJSONObject("$defs").has("org.sagebionetworks.repo.model.Folder"));
	}

	@Test
	public void testCallWithJSONObjectParamParsesButPreservesRawTree() {
		// An omitted property must stay absent on the parsed JSONObject — distinct from an explicit null,
		// which a round-tripped POJO would collapse. Here 'name' is omitted entirely.
		String toolInput = "{\"concreteType\":\"org.sagebionetworks.repo.model.Folder\",\"id\":\"syn321\"}";

		// call under test — the base parses the payload into a JSONObject and passes the raw tree through.
		String result = callback("getRawObject").call(toolInput, null);

		assertEquals("syn321", tool.getRawObject().getString("id"));
		assertFalse(tool.getRawObject().has("name"), "An omitted property must remain absent on the raw tree");
		assertNull(tool.getEntity());
		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(new S3FileHandle().setId("789"))),
				result);
	}

	@Test
	public void testCallWithJSONObjectParamAndMalformedJsonReturnsErrorString() {
		// A JSONObject parameter is validated by the base, so malformed input yields the same corrective
		// error string as a typed parameter — the tool body never runs.
		String result = callback("getRawObject").call("{ not valid json", null);

		assertTrue(result.contains("was not valid JSON for its input schema"));
		assertTrue(result.contains("Resubmit the call with a corrected argument."));
		assertNull(tool.getRawObject());
	}

	@Test
	public void testCallWithMalformedJsonReturnsErrorString() {
		// A typed parameter that cannot be parsed yields a corrective error string fed back to the
		// model — the tool method is never invoked and no exception escapes call(...).
		String result = callback("get_entity_file_handle").call("{ not valid json", new ToolContext(Map.of()));

		assertTrue(result.contains("was not valid JSON"), result);
		assertNull(tool.getEntity());
	}

	@Test
	public void testScalarInputSchemaFromParameters() {
		String inputSchema = callback("sumScalars").getToolDefinition().inputSchema();
		JSONObject schema = new JSONObject(inputSchema);

		// Each scalar parameter is a typed top-level property, named by the parameter (build enables
		// -parameters), with only the required ones listed under "required".
		assertEquals("object", schema.getString("type"));
		assertEquals("integer", schema.getJSONObject("properties").getJSONObject("count").getString("type"));
		assertEquals("string", schema.getJSONObject("properties").getJSONObject("label").getString("type"));
		assertEquals("how many", schema.getJSONObject("properties").getJSONObject("count").getString("description"));
		assertEquals(1, schema.getJSONArray("required").length());
		assertEquals("count", schema.getJSONArray("required").getString(0));
	}

	@Test
	public void testNoArgumentInputSchemaIsEmptyObject() {
		JSONObject schema = new JSONObject(callback("ping").getToolDefinition().inputSchema());
		assertEquals("object", schema.getString("type"));
		assertEquals(0, schema.getJSONObject("properties").length());
		assertFalse(schema.has("required"));
	}

	@Test
	public void testCallBindsScalarsByNameAndCoercesTypes() {
		ToolContext context = new ToolContext(Map.of("key", "value"));

		// call under test — named JSON properties bind to the method parameters, coerced to their types.
		String result = callback("sumScalars").call("{\"count\": 7, \"label\": \"hi\"}", context);

		assertEquals(Long.valueOf(7L), tool.getCount());
		assertEquals("hi", tool.getLabel());
		assertEquals(context, tool.getContext());
		assertEquals(
				JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(new S3FileHandle().setId("count-7"))),
				result);
	}

	@Test
	public void testCallBindsScalarWithRawControlCharacter() {
		// A model often emits free text with a raw (unescaped) newline inside a string value, which the
		// strict org.json parser rejects outright. The base normalizes it through the lenient mapper first,
		// so the newline is preserved in the bound value rather than costing a corrective round trip.
		String rawNewlineInput = "{\"count\": 1, \"label\": \"line1\nline2\"}";

		// call under test
		String result = callback("sumScalars").call(rawNewlineInput, null);

		assertEquals("line1\nline2", tool.getLabel());
		assertEquals(Long.valueOf(1L), tool.getCount());
		assertEquals(
				JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(new S3FileHandle().setId("count-1"))),
				result);
	}

	@Test
	public void testCallOmitsOptionalScalar() {
		// call under test — an omitted optional scalar binds to null; only the required one is supplied.
		callback("sumScalars").call("{\"count\": 3}", null);

		assertEquals(Long.valueOf(3L), tool.getCount());
		assertNull(tool.getLabel());
	}

	@Test
	public void testCallWithMissingRequiredScalarReturnsErrorString() {
		// call under test — a missing required scalar is fed back as corrective guidance; the body never runs.
		String result = callback("sumScalars").call("{\"label\": \"hi\"}", null);

		assertTrue(result.contains("missing required argument 'count'"), result);
		assertNull(tool.getCount());
	}

	@Test
	public void testCallNoArgumentToolWithEmptyObject() {
		// call under test — a no-argument tool is invoked with an empty argument object.
		String result = callback("ping").call("{}", null);

		assertTrue(tool.isNoArgCalled());
		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(new S3FileHandle().setId("pong"))),
				result);
	}

	@Test
	public void testCallNoArgumentToolWithBlankInput() {
		// call under test — a blank argument string is treated as an empty argument object.
		callback("ping").call("", null);

		assertTrue(tool.isNoArgCalled());
	}

	@Test
	public void testCallWithoutContext() {
		Folder folder = new Folder().setId("syn123").setName("my-folder");
		String toolInput = JDOSecondaryPropertyUtils.createJSONFromObject(folder);

		// call under test — the single-argument overload supplies a null context.
		String result = callback("get_entity_file_handle").call(toolInput);

		assertEquals(folder, tool.getEntity());
		assertNull(tool.getContext());
		assertNotNull(result);
	}
}
