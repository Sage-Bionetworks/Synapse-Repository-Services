package org.sagebionetworks.repo.model.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
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
		for (JsonSchema sub : SubSchemaIterable.depthFirstIterable(schema)) {
			subSchemaIds.add(sub.get$id());
		}
		List<String> expected = Lists.newArrayList("items");
		assertEquals(expected, subSchemaIds);
	}

	@Test
	public void testIteratorWithItemsHierarchy() {
		JsonSchema schema = new JsonSchema();
		schema.setItems(createHierarchy("parent", "child"));
		List<String> subSchemaIds = new LinkedList<String>();
		// call under test
		for (JsonSchema sub : SubSchemaIterable.depthFirstIterable(schema)) {
			subSchemaIds.add(sub.get$id());
		}
		List<String> expected = Lists.newArrayList("child", "parent");
		assertEquals(expected, subSchemaIds);
	}
	
	@Test
	public void testIteratorWithListsOfStrings() {
		JsonSchema schema = new JsonSchema();
		schema.set_enum(Lists.newArrayList("one","two"));
		List<String> subSchemaIds = new LinkedList<String>();
		// call under test
		for (JsonSchema sub : SubSchemaIterable.depthFirstIterable(schema)) {
			subSchemaIds.add(sub.get$id());
		}
		List<String> expected = Collections.emptyList();
		assertEquals(expected, subSchemaIds);
	}

	@Test
	public void testIteratorWithLists() {
		JsonSchema schema = new JsonSchema();
		schema.setAllOf(Lists.newArrayList(createWithId("allOfOne"), createWithId("allOfTwo"), null));
		schema.setAnyOf(Lists.newArrayList(createWithId("anyOfOne"), createWithId("anyOfTwo"),
				createHierarchy("anyOfParent", "anyOfChild")));
		List<String> subSchemaIds = new LinkedList<String>();
		// call under test
		for (JsonSchema sub : SubSchemaIterable.depthFirstIterable(schema)) {
			subSchemaIds.add(sub.get$id());
		}
		List<String> expected = Lists.newArrayList("anyOfChild", "allOfOne", "allOfTwo", "anyOfOne", "anyOfTwo",
				"anyOfParent");
		assertEquals(expected, subSchemaIds);
	}

	@Test
	public void testIteratorWithMap() {
		JsonSchema schema = new JsonSchema();
		Map<String, JsonSchema> props = new LinkedHashMap<String, JsonSchema>();
		props.put("a", createWithId("mapOne"));
		props.put("b", createWithId("mapTwo"));
		props.put("withNull", null);
		props.put("withHierarchy", createHierarchy("hierarchyParentOne", "hierarchyChildOne"));
		props.put("withHierarchyTwo", createHierarchy("hierarchyParentTwo", "hierarchyChildTwo"));
		schema.setProperties(props);
		List<String> subSchemaIds = new LinkedList<String>();
		// call under test
		for (JsonSchema sub : SubSchemaIterable.depthFirstIterable(schema)) {
			subSchemaIds.add(sub.get$id());
		}
		List<String> expected = Lists.newArrayList("hierarchyChildOne", "hierarchyChildTwo", "mapOne", "mapTwo",
				"hierarchyParentOne", "hierarchyParentTwo");
		assertEquals(expected, subSchemaIds);
	}

	@Test
	public void testIteratorWithNullSchema() {
		JsonSchema schema = null;
		assertThrows(IllegalArgumentException.class, ()->{
			SubSchemaIterable.depthFirstIterable(schema);
		});
	}
	
	
	public JsonSchema createWithId(String id) {
		JsonSchema schema = new JsonSchema();
		schema.set$id(id);
		return schema;
	}

	/**
	 * Create a simple hierarchy
	 * 
	 * @param parentId
	 * @param childId
	 * @return
	 */
	public JsonSchema createHierarchy(String parentId, String childId) {
		JsonSchema parent = createWithId(parentId);
		parent.setItems(createWithId(childId));
		return parent;
	}

}
