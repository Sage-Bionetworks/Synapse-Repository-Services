package org.sagebionetworks.repo.model.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

public class SubSchemaIterableTest {

	@Test
	public void testIteratorWithItems() {
		JsonSchema schema = new JsonSchema();
		schema.setItems(createWithId("items"));
		List<String> subSchemaIds = new LinkedList<String>();
		// call under test
		for (JsonSchema sub : new SubSchemaIterable(schema)) {
			subSchemaIds.add(sub.get$id());
		}
		List<String> expected = Lists.newArrayList("items");
		assertEquals(expected, subSchemaIds);
	}

	@Test
	public void testIteratorWithLists() {
		JsonSchema schema = new JsonSchema();
		schema.setAllOf(Lists.newArrayList(createWithId("allOfOne"), createWithId("allOfTwo"), null));
		schema.setAnyOf(Lists.newArrayList(createWithId("anyOfOne"), createWithId("anyOfTwo")));
		List<String> subSchemaIds = new LinkedList<String>();
		// call under test
		for (JsonSchema sub : new SubSchemaIterable(schema)) {
			subSchemaIds.add(sub.get$id());
		}
		List<String> expected = Lists.newArrayList("allOfOne", "allOfTwo", "anyOfOne", "anyOfTwo");
		assertEquals(expected, subSchemaIds);
	}

	@Test
	public void testIteratorWithMap() {
		JsonSchema schema = new JsonSchema();
		Map<String, JsonSchema> props = new LinkedHashMap<String, JsonSchema>();
		props.put("a", createWithId("mapOne"));
		props.put("b", createWithId("mapTwo"));
		schema.setProperties(props);
		List<String> subSchemaIds = new LinkedList<String>();
		// call under test
		for (JsonSchema sub : new SubSchemaIterable(schema)) {
			subSchemaIds.add(sub.get$id());
		}
		List<String> expected = Lists.newArrayList("mapOne", "mapTwo");
		assertEquals(expected, subSchemaIds);
	}

	public JsonSchema createWithId(String id) {
		JsonSchema schema = new JsonSchema();
		schema.set$id(id);
		return schema;
	}
}
