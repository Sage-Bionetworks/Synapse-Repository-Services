package org.sagebionetworks.repo.model.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class JsonSchemaPropertiesTest {

	@Test
	public void testCollectTopLevelPropertiesWithFlatProperties() {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		JsonSchema a = new JsonSchema().setType(Type.integer);
		JsonSchema b = new JsonSchema().setType(Type._boolean);
		properties.put("a", a);
		properties.put("b", b);
		JsonSchema schema = new JsonSchema().setProperties(properties);

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("a", a);
		expected.put("b", b);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesWithTopLevelRef() {
		JsonSchema a = new JsonSchema().setType(Type.integer);
		Map<String, JsonSchema> defProperties = new LinkedHashMap<>();
		defProperties.put("a", a);
		JsonSchema definition = new JsonSchema().setProperties(defProperties);

		Map<String, JsonSchema> definitions = new LinkedHashMap<>();
		definitions.put("X", definition);

		JsonSchema schema = new JsonSchema().set$ref("#/definitions/X").setDefinitions(definitions);

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("a", a);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesWithCombinationKeywords() {
		// own property
		JsonSchema own = new JsonSchema().setType(Type.string);
		Map<String, JsonSchema> rootProps = new LinkedHashMap<>();
		rootProps.put("own", own);

		// allOf member referenced via $ref
		JsonSchema allOfProp = new JsonSchema().setType(Type.integer);
		Map<String, JsonSchema> allOfDefProps = new LinkedHashMap<>();
		allOfDefProps.put("fromAllOf", allOfProp);
		JsonSchema allOfDefinition = new JsonSchema().setProperties(allOfDefProps);
		Map<String, JsonSchema> definitions = new LinkedHashMap<>();
		definitions.put("X", allOfDefinition);
		JsonSchema allOfMember = new JsonSchema().set$ref("#/definitions/X");

		// anyOf member declaring a property inline
		JsonSchema anyOfProp = new JsonSchema().setType(Type._boolean);
		Map<String, JsonSchema> anyOfProps = new LinkedHashMap<>();
		anyOfProps.put("fromAnyOf", anyOfProp);
		JsonSchema anyOfMember = new JsonSchema().setProperties(anyOfProps);

		// oneOf member declaring a property inline
		JsonSchema oneOfProp = new JsonSchema().setType(Type.number);
		Map<String, JsonSchema> oneOfProps = new LinkedHashMap<>();
		oneOfProps.put("fromOneOf", oneOfProp);
		JsonSchema oneOfMember = new JsonSchema().setProperties(oneOfProps);

		JsonSchema schema = new JsonSchema()
				.setProperties(rootProps)
				.setDefinitions(definitions)
				.setAllOf(java.util.Arrays.asList(allOfMember))
				.setAnyOf(java.util.Arrays.asList(anyOfMember))
				.setOneOf(java.util.Arrays.asList(oneOfMember));

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("own", own);
		expected.put("fromAllOf", allOfProp);
		expected.put("fromAnyOf", anyOfProp);
		expected.put("fromOneOf", oneOfProp);
		assertEquals(expected, result);
		// order is deterministic: own properties first, then allOf, anyOf, oneOf
		assertEquals(java.util.Arrays.asList("own", "fromAllOf", "fromAnyOf", "fromOneOf"),
                new java.util.ArrayList<>(result.keySet()));
	}

	@Test
	public void testCollectTopLevelPropertiesWithIfThenElse() {
		JsonSchema ifProp = new JsonSchema().setType(Type.string);
		Map<String, JsonSchema> ifProps = new LinkedHashMap<>();
		ifProps.put("fromIf", ifProp);

		JsonSchema thenProp = new JsonSchema().setType(Type.integer);
		Map<String, JsonSchema> thenProps = new LinkedHashMap<>();
		thenProps.put("fromThen", thenProp);

		JsonSchema elseProp = new JsonSchema().setType(Type._boolean);
		Map<String, JsonSchema> elseProps = new LinkedHashMap<>();
		elseProps.put("fromElse", elseProp);

		JsonSchema schema = new JsonSchema()
				.set_if(new JsonSchema().setProperties(ifProps))
				.setThen(new JsonSchema().setProperties(thenProps))
				.set_else(new JsonSchema().setProperties(elseProps));

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("fromIf", ifProp);
		expected.put("fromThen", thenProp);
		expected.put("fromElse", elseProp);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesDoesNotExpandNestedObject() {
		// a property whose value is an object with its own properties
		Map<String, JsonSchema> nestedProps = new LinkedHashMap<>();
		nestedProps.put("nested", new JsonSchema().setType(Type.string));
		JsonSchema objectProperty = new JsonSchema().setType(Type.object).setProperties(nestedProps);

		Map<String, JsonSchema> rootProps = new LinkedHashMap<>();
		rootProps.put("obj", objectProperty);
		JsonSchema schema = new JsonSchema().setProperties(rootProps);

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("obj", objectProperty);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesDoesNotExpandArrayItems() {
		// a property whose value is an array of objects with their own properties
		Map<String, JsonSchema> itemProps = new LinkedHashMap<>();
		itemProps.put("itemField", new JsonSchema().setType(Type.string));
		JsonSchema arrayProperty = new JsonSchema().setType(Type.array)
				.setItems(new JsonSchema().setType(Type.object).setProperties(itemProps));

		Map<String, JsonSchema> rootProps = new LinkedHashMap<>();
		rootProps.put("arr", arrayProperty);
		JsonSchema schema = new JsonSchema().setProperties(rootProps);

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("arr", arrayProperty);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesResolvesPropertyValueRef() {
		JsonSchema resolvedTarget = new JsonSchema().setType(Type.array)
				.setItems(new JsonSchema().setType(Type.string));
		Map<String, JsonSchema> definitions = new LinkedHashMap<>();
		definitions.put("Y", resolvedTarget);

		Map<String, JsonSchema> rootProps = new LinkedHashMap<>();
		rootProps.put("foo", new JsonSchema().set$ref("#/definitions/Y"));
		JsonSchema schema = new JsonSchema().setProperties(rootProps).setDefinitions(definitions);

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("foo", resolvedTarget);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesKeepsDanglingPropertyValueRef() {
		JsonSchema dangling = new JsonSchema().set$ref("#/definitions/Missing");
		Map<String, JsonSchema> rootProps = new LinkedHashMap<>();
		rootProps.put("bar", dangling);
		JsonSchema schema = new JsonSchema().setProperties(rootProps)
				.setDefinitions(new LinkedHashMap<>());

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("bar", dangling);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesExcludesNotContainsAdditionalProperties() {
		Map<String, JsonSchema> notProps = new LinkedHashMap<>();
		notProps.put("fromNot", new JsonSchema().setType(Type.string));

		Map<String, JsonSchema> containsProps = new LinkedHashMap<>();
		containsProps.put("fromContains", new JsonSchema().setType(Type.string));

		Map<String, JsonSchema> additionalProps = new LinkedHashMap<>();
		additionalProps.put("fromAdditional", new JsonSchema().setType(Type.string));

		JsonSchema keep = new JsonSchema().setType(Type.integer);
		Map<String, JsonSchema> rootProps = new LinkedHashMap<>();
		rootProps.put("keep", keep);

		JsonSchema schema = new JsonSchema()
				.setProperties(rootProps)
				.setNot(new JsonSchema().setProperties(notProps))
				.setContains(new JsonSchema().setProperties(containsProps))
				.setAdditionalProperties(new JsonSchema().setProperties(additionalProps));

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("keep", keep);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesFirstOccurrenceWins() {
		JsonSchema rootDup = new JsonSchema().setType(Type.integer);
		Map<String, JsonSchema> rootProps = new LinkedHashMap<>();
		rootProps.put("dup", rootDup);

		JsonSchema allOfDup = new JsonSchema().setType(Type.string);
		Map<String, JsonSchema> allOfProps = new LinkedHashMap<>();
		allOfProps.put("dup", allOfDup);

		JsonSchema schema = new JsonSchema().setProperties(rootProps)
				.setAllOf(java.util.Arrays.asList(new JsonSchema().setProperties(allOfProps)));

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		// the root's own property is encountered first and wins
		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("dup", rootDup);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesTerminatesOnDefinitionCycle() {
		// X references Y and Y references X
		JsonSchema xProp = new JsonSchema().setType(Type.integer);
		Map<String, JsonSchema> xProps = new LinkedHashMap<>();
		xProps.put("x", xProp);

		JsonSchema yProp = new JsonSchema().setType(Type.string);
		Map<String, JsonSchema> yProps = new LinkedHashMap<>();
		yProps.put("y", yProp);

		Map<String, JsonSchema> definitions = new LinkedHashMap<>();
		JsonSchema x = new JsonSchema().setProperties(xProps)
				.setAllOf(java.util.Arrays.asList(new JsonSchema().set$ref("#/definitions/Y")));
		JsonSchema y = new JsonSchema().setProperties(yProps)
				.setAllOf(java.util.Arrays.asList(new JsonSchema().set$ref("#/definitions/X")));
		definitions.put("X", x);
		definitions.put("Y", y);

		JsonSchema schema = new JsonSchema().set$ref("#/definitions/X").setDefinitions(definitions);

		// call under test (must terminate)
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("x", xProp);
		expected.put("y", yProp);
		assertEquals(expected, result);
	}

	@Test
	public void testCollectTopLevelPropertiesWithNullRoot() {
		// call under test
		assertTrue(JsonSchemaProperties.collectTopLevelProperties(null).isEmpty());
	}

	@Test
	public void testCollectTopLevelPropertiesWithNoProperties() {
		// call under test
		assertTrue(JsonSchemaProperties.collectTopLevelProperties(new JsonSchema()).isEmpty());
	}

	@Test
	public void testCollectTopLevelPropertiesWithRefButNoDefinitions() {
		// a top-level $ref but the schema carries no definitions map
		JsonSchema schema = new JsonSchema().set$ref("#/definitions/X");

		// call under test
		assertTrue(JsonSchemaProperties.collectTopLevelProperties(schema).isEmpty());
	}

	@Test
	public void testCollectTopLevelPropertiesWithRefWithoutDefinitionsPrefix() {
		// a $ref that is a bare definition key rather than a #/definitions/ path
		JsonSchema target = new JsonSchema().setType(Type.integer);
		Map<String, JsonSchema> defProps = new LinkedHashMap<>();
		defProps.put("a", target);
		Map<String, JsonSchema> definitions = new LinkedHashMap<>();
		definitions.put("Z", new JsonSchema().setProperties(defProps));

		JsonSchema schema = new JsonSchema().set$ref("Z").setDefinitions(definitions);

		// call under test
		Map<String, JsonSchema> result = JsonSchemaProperties.collectTopLevelProperties(schema);

		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put("a", target);
		assertEquals(expected, result);
	}
}
