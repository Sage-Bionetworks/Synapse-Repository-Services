package org.sagebionetworks.javadoc.velocity.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;

/**
 * Tests for recursion handling in {@link SchemaUtils}.
 *
 * <p>Multi-type recursion: a parent type carries the {@code $recursiveAnchor} and contains an inline
 * named child type whose array items are {@code {"$recursiveRef":"#"}}, resolving back to the parent.
 * The doclet extracts the inline child and documents it as its own type, so the child's
 * {@code $recursiveRef} must resolve to the enclosing anchor rather than failing.</p>
 */
public class SchemaUtilsTest {

	private static final String PARENT_ID = "org.example.Parent";
	private static final String CHILD_ID = "org.example.Child";

	/** A {@code {"$recursiveRef":"#"}} schema node. */
	private static ObjectSchema recursiveRef() {
		ObjectSchema ref = new ObjectSchemaImpl();
		ref.set$recursiveRef("#");
		return ref;
	}

	/** An inline named child type whose {@code children} array recurses back to the anchor. It
	 *  carries no {@code $recursiveAnchor} of its own &mdash; the anchor is on the parent. */
	private static ObjectSchema childSchema() {
		ObjectSchema childrenArray = new ObjectSchemaImpl();
		childrenArray.setType(TYPE.ARRAY);
		childrenArray.setItems(recursiveRef());

		LinkedHashMap<String, ObjectSchema> props = new LinkedHashMap<>();
		props.put("children", childrenArray);

		ObjectSchema child = new ObjectSchemaImpl();
		child.setType(TYPE.OBJECT);
		child.setName("Child");
		child.setId(CHILD_ID);
		child.setProperties(props);
		return child;
	}

	/** The anchor type containing the inline child under its {@code child} slot. */
	private static ObjectSchema parentAnchorSchema() {
		LinkedHashMap<String, ObjectSchema> props = new LinkedHashMap<>();
		props.put("child", childSchema());

		ObjectSchema parent = new ObjectSchemaImpl();
		parent.setType(TYPE.OBJECT);
		parent.setName("Parent");
		parent.setId(PARENT_ID);
		parent.set$recursiveAnchor(Boolean.TRUE);
		parent.setProperties(props);
		return parent;
	}

	/** A type that inherits the parent's inline child (as a schema that extends the anchor does) but
	 *  not the parent's {@code $recursiveAnchor}. */
	private static ObjectSchema subtypeWithoutAnchorSchema() {
		LinkedHashMap<String, ObjectSchema> props = new LinkedHashMap<>();
		props.put("child", childSchema());

		ObjectSchema subtype = new ObjectSchemaImpl();
		subtype.setType(TYPE.OBJECT);
		subtype.setName("Subtype");
		subtype.setId("org.example.Subtype");
		subtype.setProperties(props);
		return subtype;
	}

	@Test
	public void testRecursiveAddTypesWithInlineTypeReachedWithoutAnchorFirst() {
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		Map<String, ObjectSchema> anchorMap = new HashMap<>();
		ObjectSchema parent = parentAnchorSchema();
		SchemaUtils.recursiveAddTypes(schemaMap, "org.example.Subtype", subtypeWithoutAnchorSchema(), anchorMap, null);

		// call under test
		SchemaUtils.recursiveAddTypes(schemaMap, PARENT_ID, parent, anchorMap, null);

		assertEquals(parent, anchorMap.get(CHILD_ID));
		assertEquals(Set.of(PARENT_ID, CHILD_ID, "org.example.Subtype"), schemaMap.keySet());
	}

	@Test
	public void testRecursiveAddTypesWithInlineTypeReachedWithAnchorFirst() {
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		Map<String, ObjectSchema> anchorMap = new HashMap<>();
		ObjectSchema parent = parentAnchorSchema();
		SchemaUtils.recursiveAddTypes(schemaMap, PARENT_ID, parent, anchorMap, null);

		// call under test
		SchemaUtils.recursiveAddTypes(schemaMap, "org.example.Subtype", subtypeWithoutAnchorSchema(), anchorMap, null);

		assertEquals(parent, anchorMap.get(CHILD_ID));
		assertEquals(Set.of(CHILD_ID), anchorMap.keySet());
	}

	@Test
	public void testTranslateToModelWithAnchorRootResolvesInlineChildLink() {
		ObjectSchema parent = parentAnchorSchema();

		// call under test
		ObjectSchemaModel model = SchemaUtils.translateToModel(parent, null);

		assertNotNull(model.getFields());
		SchemaFields childField = model.getFields().get(0);
		assertEquals("child", childField.getName());
		assertEquals("Child", childField.getType().getDisplay()[0]);
	}

	@Test
	public void testTranslateToModelWithInlineTypeResolvesRecursiveRefToEnclosingAnchor() {
		// The inline child documented standalone: its children[*] $recursiveRef must resolve to the
		// enclosing anchor (the parent), not throw for a missing anchor.
		ObjectSchema child = childSchema();

		// call under test
		ObjectSchemaModel model = SchemaUtils.translateToModel(child, null, parentAnchorSchema());

		assertNotNull(model.getFields());
		SchemaFields childrenField = model.getFields().get(0);
		assertEquals("children", childrenField.getName());
		assertEquals(true, childrenField.getType().getIsArray());
		assertEquals("Parent", childrenField.getType().getDisplay()[0]);
		assertEquals("${" + PARENT_ID + "}", childrenField.getType().getHref()[0]);
	}
}
