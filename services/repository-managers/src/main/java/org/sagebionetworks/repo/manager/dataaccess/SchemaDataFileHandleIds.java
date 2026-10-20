package org.sagebionetworks.repo.manager.dataaccess;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.schema.JsonSchema;

/**
 * Finds the file handle IDs that a JSON document references through the properties its schema
 * declares with the {@link FormTemplateValidator#FILE_HANDLE_FORMAT} format.
 * <p>
 * The document and the schema are walked together through {@code properties},
 * {@code additionalProperties}, {@code items}, {@code contains} and the combination keywords
 * ({@code $ref}, {@code allOf}, {@code anyOf}, {@code oneOf}, {@code if}, {@code then},
 * {@code else}). Every branch of a combination is followed whether or not the document satisfies it,
 * so a value counts as a file handle ID if any branch declares it one. Over counting only means that
 * more IDs are access checked, which is the safe direction. Only whole numbers, or strings of
 * digits, are collected: a draft is not yet validated, and no other value can name a file handle.
 */
final class SchemaDataFileHandleIds {

	private static final String DEFINITIONS_PREFIX = "#/definitions/";
	private static final Pattern DIGITS = Pattern.compile("\\d+");

	private SchemaDataFileHandleIds() {
		// utility
	}

	/**
	 * @param root     The schema the document answers, with its references resolved against its own
	 *                 {@code definitions}, as {@code JsonSchemaManager.getValidationSchema} returns it.
	 * @param document The document, as parsed by org.json.
	 * @return The distinct file handle IDs the document references, in the order they are found.
	 */
	static List<String> find(JsonSchema root, Object document) {
		Set<String> ids = new LinkedHashSet<>();
		walk(root, root.getDefinitions(), document, new HashSet<>(), ids);
		return new ArrayList<>(ids);
	}

	/**
	 * @param refsFollowedForValue The references already followed for this same value, which cuts
	 *                             off a cycle of references that never descends into the document.
	 */
	private static void walk(JsonSchema node, Map<String, JsonSchema> definitions, Object value,
			Set<String> refsFollowedForValue, Set<String> ids) {
		if (node == null || value == null || JSONObject.NULL.equals(value)) {
			return;
		}
		if (node.get$ref() != null) {
			if (refsFollowedForValue.add(node.get$ref()) && definitions != null) {
				walk(definitions.get(relativeRef(node.get$ref())), definitions, value, refsFollowedForValue, ids);
			}
			return;
		}
		if (FormTemplateValidator.FILE_HANDLE_FORMAT.equals(node.getFormat())) {
			toFileHandleId(value).ifPresent(ids::add);
		}
		if (value instanceof JSONObject object) {
			for (String key : object.keySet()) {
				JsonSchema propertySchema = node.getProperties() == null ? null : node.getProperties().get(key);
				walk(propertySchema != null ? propertySchema : node.getAdditionalProperties(), definitions,
						object.get(key), new HashSet<>(), ids);
			}
		}
		if (value instanceof JSONArray array) {
			for (int i = 0; i < array.length(); i++) {
				walk(node.getItems(), definitions, array.get(i), new HashSet<>(), ids);
				walk(node.getContains(), definitions, array.get(i), new HashSet<>(), ids);
			}
		}
		walkAll(node.getAllOf(), definitions, value, refsFollowedForValue, ids);
		walkAll(node.getAnyOf(), definitions, value, refsFollowedForValue, ids);
		walkAll(node.getOneOf(), definitions, value, refsFollowedForValue, ids);
		walk(node.get_if(), definitions, value, refsFollowedForValue, ids);
		walk(node.getThen(), definitions, value, refsFollowedForValue, ids);
		walk(node.get_else(), definitions, value, refsFollowedForValue, ids);
	}

	private static void walkAll(List<JsonSchema> nodes, Map<String, JsonSchema> definitions, Object value,
			Set<String> refsFollowedForValue, Set<String> ids) {
		if (nodes == null) {
			return;
		}
		for (JsonSchema node : nodes) {
			walk(node, definitions, value, refsFollowedForValue, ids);
		}
	}

	private static Optional<String> toFileHandleId(Object value) {
		if (value instanceof String string) {
			return DIGITS.matcher(string).matches() ? Optional.of(string) : Optional.empty();
		}
		if (value instanceof Number number) {
			try {
				BigDecimal decimal = new BigDecimal(number.toString());
				return decimal.signum() < 0 ? Optional.empty()
						: Optional.of(decimal.toBigIntegerExact().toString());
			} catch (ArithmeticException | NumberFormatException e) {
				// Not a whole number
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	private static String relativeRef(String ref) {
		return ref.startsWith(DEFINITIONS_PREFIX) ? ref.substring(DEFINITIONS_PREFIX.length()) : ref;
	}
}
