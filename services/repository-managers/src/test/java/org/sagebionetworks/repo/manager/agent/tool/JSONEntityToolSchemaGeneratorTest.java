package org.sagebionetworks.repo.manager.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.query.FilterInstanceFactory;
import org.sagebionetworks.repo.model.grid.query.QueryRequest;
import org.sagebionetworks.repo.model.grid.query.SelectItemInstanceFactory;

public class JSONEntityToolSchemaGeneratorTest {

	private JSONObject generateQuerySchema() {
		String schema = JSONEntityToolSchemaGenerator.generateSchema(QueryRequest.class.getName(),
				List.of(FilterInstanceFactory.singleton().getKeySetIterator(),
						SelectItemInstanceFactory.singleton().getKeySetIterator()));
		return new JSONObject(schema);
	}

	@Test
	public void testGenerateSchemaInlinesRootAtTopLevel() {
		JSONObject schema = generateQuerySchema();
		
		System.out.println(schema.toString(3));

		// The root request schema is hoisted to the top level (not nested under a keyed entry), so the
		// document is directly consumable as a JSON Schema for the request type.
		assertTrue(schema.has("properties"), "root properties should be inlined at the top level");
		assertTrue(schema.getJSONObject("properties").has("query"));
		// The root type is not duplicated as a $defs entry.
		assertFalse(schema.getJSONObject("$defs").has(QueryRequest.class.getName()),
				"root type should be inlined, not also placed under $defs");
	}

	@Test
	public void testGenerateSchemaRefsResolveToDefs() {
		JSONObject schema = generateQuerySchema();
		JSONObject defs = schema.getJSONObject("$defs");

		// Every $ref must use the standard $defs prefix and resolve to a $defs key. The OpenAPI
		// components prefix is only valid inside a full spec, which this standalone schema is not.
		forEachRef(schema, ref -> {
			assertFalse(ref.startsWith("#/components/schemas/"),
					"Ref should not use the OpenAPI components prefix: " + ref);
			assertTrue(ref.startsWith("#/$defs/"), "Ref should target $defs: " + ref);
			String target = ref.substring("#/$defs/".length());
			assertTrue(defs.has(target), "Ref target must exist as a $defs key: " + ref);
		});
	}

	@Test
	public void testGenerateSchemaRetainsPolymorphicUnions() {
		JSONObject schema = generateQuerySchema();
		JSONObject defs = schema.getJSONObject("$defs");

		// The interface unions and their concrete implementers survive under $defs.
		assertTrue(defs.has("org.sagebionetworks.repo.model.grid.query.Filter"));
		assertTrue(defs.has("org.sagebionetworks.repo.model.grid.query.CellValueFilter"));
		assertTrue(defs.has("org.sagebionetworks.repo.model.grid.query.SelectAll"));

		// The Filter union still lists its implementers under oneOf.
		JSONArray oneOf = defs.getJSONObject("org.sagebionetworks.repo.model.grid.query.Filter").getJSONArray("oneOf");
		assertTrue(oneOf.length() > 0);
	}

	@Test
	public void testGenerateSchemaInlinesEnumsWithoutOrphanDefs() {
		JSONObject schema = generateQuerySchema();
		JSONObject defs = schema.getJSONObject("$defs");

		// Enum-valued properties (e.g. CellValueFilter.operator) are inlined at each use site, so a
		// standalone enum definition would be a never-referenced orphan. Reachability drops these.
		assertFalse(defs.has("org.sagebionetworks.repo.model.grid.query.CellValueOperator"),
				"Inlined enum should not also appear as a standalone $defs entry");
		assertFalse(defs.has("org.sagebionetworks.repo.model.grid.query.ValidationOperator"),
				"Inlined enum should not also appear as a standalone $defs entry");

		// The enum values still reach the model, inlined on the property that uses them.
		JSONObject operator = defs.getJSONObject("org.sagebionetworks.repo.model.grid.query.CellValueFilter")
				.getJSONObject("properties").getJSONObject("operator");
		assertTrue(operator.getJSONArray("enum").toList().contains("GREATER_THAN"));
	}

	@Test
	public void testGenerateSchemaAppliesParamDescriptionToRoot() {
		String schema = JSONEntityToolSchemaGenerator.generateSchema(QueryRequest.class.getName(),
				List.of(FilterInstanceFactory.singleton().getKeySetIterator(),
						SelectItemInstanceFactory.singleton().getKeySetIterator()),
				"Query the grid — this is NOT SQL.");

		// The request POJO is itself the tool argument, so the param description lands on the root node.
		assertEquals("Query the grid — this is NOT SQL.", new JSONObject(schema).getString("description"));
	}

	@Test
	public void testGenerateSchemaBlankParamDescriptionDoesNotClobber() {
		String withBlank = JSONEntityToolSchemaGenerator.generateSchema(QueryRequest.class.getName(),
				List.of(FilterInstanceFactory.singleton().getKeySetIterator(),
						SelectItemInstanceFactory.singleton().getKeySetIterator()),
				"   ");
		String withNone = JSONEntityToolSchemaGenerator.generateSchema(QueryRequest.class.getName(),
				List.of(FilterInstanceFactory.singleton().getKeySetIterator(),
						SelectItemInstanceFactory.singleton().getKeySetIterator()));

		// A blank param description is ignored — it neither overwrites the root type's own description
		// nor otherwise alters the document produced without any override.
		assertEquals(withNone, withBlank);
	}

	@Test
	public void testGenerateSchemaHasNoUnreferencedDefs() {
		JSONObject schema = generateQuerySchema();
		JSONObject defs = schema.getJSONObject("$defs");

		// Every $defs entry must be reachable by some $ref in the document — no dead definitions.
		java.util.Set<String> referenced = new java.util.HashSet<>();
		forEachRef(schema, ref -> referenced.add(ref.substring("#/$defs/".length())));
		for (String key : defs.keySet()) {
			assertTrue(referenced.contains(key), "Unreferenced $defs entry: " + key);
		}
	}

	@Test
	public void testGenerateScalarSchema() {
		String schema = JSONEntityToolSchemaGenerator.generateScalarSchema(List.of(
				new JSONEntityToolSchemaGenerator.ScalarParameter("count", Long.class, "how many", true),
				new JSONEntityToolSchemaGenerator.ScalarParameter("label", String.class, "an optional label", false),
				new JSONEntityToolSchemaGenerator.ScalarParameter("enabled", Boolean.class, "a flag", false),
				new JSONEntityToolSchemaGenerator.ScalarParameter("ratio", Double.class, "a ratio", false)),
				"scalar args");
		JSONObject object = new JSONObject(schema);

		assertEquals("object", object.getString("type"));
		assertEquals("scalar args", object.getString("description"));
		JSONObject properties = object.getJSONObject("properties");
		assertEquals("integer", properties.getJSONObject("count").getString("type"));
		assertEquals("how many", properties.getJSONObject("count").getString("description"));
		assertEquals("string", properties.getJSONObject("label").getString("type"));
		assertEquals("boolean", properties.getJSONObject("enabled").getString("type"));
		assertEquals("number", properties.getJSONObject("ratio").getString("type"));

		// Only the required parameter is listed.
		assertEquals(List.of("count"), object.getJSONArray("required").toList());
	}

	@Test
	public void testGenerateScalarSchemaWithNoParameters() {
		String schema = JSONEntityToolSchemaGenerator.generateScalarSchema(List.of(), null);
		JSONObject object = new JSONObject(schema);

		// A no-argument tool still advertises a valid object schema with no properties and no required list.
		assertEquals("object", object.getString("type"));
		assertEquals(0, object.getJSONObject("properties").length());
		assertFalse(object.has("required"));
		assertFalse(object.has("description"));
	}

	@Test
	public void testGenerateScalarSchemaRejectsUnsupportedType() {
		// A non-scalar argument belongs in a JSONEntity request POJO, not as a top-level scalar property.
		assertThrows(IllegalStateException.class, () -> JSONEntityToolSchemaGenerator.generateScalarSchema(
				List.of(new JSONEntityToolSchemaGenerator.ScalarParameter("when", java.util.Date.class, "a date", true)),
				null));
	}

	/**
	 * Recursively visit every {@code $ref} value anywhere in the schema document.
	 */
	private void forEachRef(Object node, java.util.function.Consumer<String> consumer) {
		if (node instanceof JSONObject object) {
			for (String key : object.keySet()) {
				if ("$ref".equals(key)) {
					consumer.accept(object.getString(key));
				} else {
					forEachRef(object.get(key), consumer);
				}
			}
		} else if (node instanceof JSONArray array) {
			for (int i = 0; i < array.length(); i++) {
				forEachRef(array.get(i), consumer);
			}
		}
	}
}
