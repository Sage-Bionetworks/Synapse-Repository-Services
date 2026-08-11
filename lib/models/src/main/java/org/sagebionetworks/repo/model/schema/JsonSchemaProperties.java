package org.sagebionetworks.repo.model.schema;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects the set of names that could appear as a top-level property of an
 * instance validated by a {@link JsonSchema}. The walk follows schema
 * combination keywords ({@code $ref}, {@code allOf}, {@code anyOf},
 * {@code oneOf}, {@code if}, {@code then}, {@code else}) so that properties
 * declared in referenced or composed sub-schemas are included. It does NOT
 * descend into a property's own value, {@code items}, {@code contains},
 * {@code additionalProperties} or {@code not}, so the nested properties of an
 * {@code object} or the elements of an {@code array} are never surfaced as
 * top-level properties.
 */
public class JsonSchemaProperties {

	private static final String DEFINITIONS_PREFIX = "#/definitions/";

	/**
	 * Collect the top-level properties described by the given schema, keyed by
	 * property name. The map preserves the order in which properties were first
	 * encountered and the first occurrence of a name wins. Each value is the
	 * {@code $ref}-resolved schema for that property.
	 *
	 * @param root the schema to walk, may be null
	 * @return an ordered map of property name to resolved property schema, never
	 *         null
	 */
	public static Map<String, JsonSchema> collectTopLevelProperties(JsonSchema root) {
		Map<String, JsonSchema> result = new LinkedHashMap<String, JsonSchema>();
		if (root == null) {
			return result;
		}
		Map<String, JsonSchema> definitions = root.getDefinitions();
		collect(root, definitions, new HashSet<>(), result);
		return result;
	}

	/**
	 * Recursively gather the top-level property names reachable from the given
	 * node through combination keywords.
	 */
	private static void collect(JsonSchema node, Map<String, JsonSchema> definitions, Set<String> visitedRefs,
			Map<String, JsonSchema> result) {
		if (node == null) {
			return;
		}
		// Follow a combination $ref to the schema it references.
		if (node.get$ref() != null) {
			if (!visitedRefs.add(node.get$ref())) {
				return;
			}
			collect(resolveRef(node.get$ref(), definitions), definitions, visitedRefs, result);
			return;
		}
		if (node.getProperties() != null) {
			for (Map.Entry<String, JsonSchema> entry : node.getProperties().entrySet()) {
				if (!result.containsKey(entry.getKey())) {
					result.put(entry.getKey(), resolveProperty(entry.getValue(), definitions));
				}
			}
		}
		// Recurse through combination keywords in a fixed, deterministic order.
		collectAll(node.getAllOf(), definitions, visitedRefs, result);
		collectAll(node.getAnyOf(), definitions, visitedRefs, result);
		collectAll(node.getOneOf(), definitions, visitedRefs, result);
		collect(node.get_if(), definitions, visitedRefs, result);
		collect(node.getThen(), definitions, visitedRefs, result);
		collect(node.get_else(), definitions, visitedRefs, result);
	}

	private static void collectAll(List<JsonSchema> nodes, Map<String, JsonSchema> definitions,
			Set<String> visitedRefs, Map<String, JsonSchema> result) {
		if (nodes == null) {
			return;
		}
		for (JsonSchema node : nodes) {
			collect(node, definitions, visitedRefs, result);
		}
	}

	/**
	 * Resolve a property value: if it is a {@code $ref} it is replaced with the
	 * referenced schema. A dangling or unresolvable {@code $ref} returns the raw
	 * node so the property name is preserved with an unknown type.
	 */
	private static JsonSchema resolveProperty(JsonSchema property, Map<String, JsonSchema> definitions) {
		if (property == null || property.get$ref() == null) {
			return property;
		}
		JsonSchema resolved = resolveRef(property.get$ref(), definitions);
		return resolved == null ? property : resolved;
	}

	/**
	 * Resolve a local {@code #/definitions/...} reference against the root
	 * definitions map. Returns null when the reference cannot be resolved.
	 */
	private static JsonSchema resolveRef(String ref, Map<String, JsonSchema> definitions) {
		if (definitions == null) {
			return null;
		}
		return definitions.get(getRelativeRef(ref));
	}

	/**
	 * Given a full {@code #/definitions/X} reference, return the relative key
	 * ({@code X}) used in the definitions map.
	 */
	private static String getRelativeRef(String ref) {
		if (ref.startsWith(DEFINITIONS_PREFIX)) {
			return ref.substring(DEFINITIONS_PREFIX.length());
		}
		return ref;
	}
}
