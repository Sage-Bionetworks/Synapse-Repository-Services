package org.sagebionetworks.repo.model.dbo.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Codec for opaque-Object JSON columns on the search-feature DTOs:
 * {@code TextAnalyzer.settings}, {@code SynonymSet.definition},
 * {@code SearchConfiguration.defaultAnalyzer}, and each element of
 * {@code SearchConfiguration.columnAnalyzerOverrides}.
 *
 * <p>Two boundaries to cross:</p>
 *
 * <ol>
 *   <li><b>DB &rarr; POJO.</b> The schema-to-pojo wire serializer
 *       ({@code JSONObjectAdapterImpl.putObject}) only accepts {@code String} / scalar /
 *       {@code JSONObjectAdapter} / {@link JSONObject} / {@link JSONArray} when writing
 *       an {@code Object}-typed field. {@link #deserialize} therefore parses the JSON
 *       column via {@link JSONTokener} into one of those shapes &mdash; not a Jackson
 *       tree, which would surface as {@link java.util.LinkedHashMap} and blow up at
 *       the wire boundary.</li>
 *
 *   <li><b>POJO &rarr; DB.</b> {@link #serialize} routes every supported caller-side
 *       shape through a single Jackson pipeline and emits canonical JSON for
 *       persistence.</li>
 * </ol>
 *
 * <h3>Java-API ergonomics: no manual stringification</h3>
 *
 * <p>Callers pass <b>JSON-shaped Java values</b>, never JSON-string literals:</p>
 * <ul>
 *   <li>{@link java.util.Map} or {@link JSONObject} for a JSON object &mdash; e.g. an
 *       inline resource literal, or a {@code {"$ref": "qname"}} reference object.</li>
 *   <li>{@link java.util.List} or {@link JSONArray} for a JSON array.</li>
 *   <li>{@link String} for a JSON scalar &mdash; e.g. a bare qualified-name reference.
 *       The codec encodes it as a JSON scalar; it is <b>not</b> raw-JSON passthrough.</li>
 *   <li>boxed numbers / booleans for matching JSON scalars.</li>
 * </ul>
 *
 * <p>Callers <b>do not</b> pre-stringify, escape, or quote JSON. The codec produces
 * the canonical JSON for storage. To store a JSON object, pass a {@code Map} /
 * {@code JSONObject}; never pass a {@code String} that happens to contain JSON.</p>
 */
final class OpaqueJsonColumnCodecUtil {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private OpaqueJsonColumnCodecUtil() {
		// utility
	}

	/**
	 * Parse a stored JSON column value into the wire-friendly {@code Object} shape that
	 * an opaque-Object POJO field expects:
	 * <ul>
	 *   <li>{@link JSONObject} for a JSON object,</li>
	 *   <li>{@link JSONArray} for a JSON array,</li>
	 *   <li>a boxed scalar ({@code String}, {@code Integer}/{@code Long}/{@code Double},
	 *       {@code Boolean}) for a JSON scalar.</li>
	 * </ul>
	 * Returns {@code null} when {@code json} is {@code null}.
	 */
	static Object deserialize(String json, String fieldDescription) {
		if (json == null) {
			return null;
		}
		try {
			return new JSONTokener(json).nextValue();
		} catch (JSONException e) {
			throw new IllegalStateException("Failed to deserialize " + fieldDescription + " JSON", e);
		}
	}

	/**
	 * Variant of {@link #deserialize} for opaque-Object <i>array</i> POJO fields, which
	 * the schema-to-pojo writer surfaces as {@code List<Object>}. The stored JSON must be
	 * a JSON array; each element retains its wire-friendly shape ({@link JSONObject} for
	 * an object element, scalar otherwise) so the array writer can {@code putObject} it.
	 *
	 * <p>Returns {@code null} when {@code json} is {@code null}.</p>
	 */
	static List<Object> deserializeList(String json, String fieldDescription) {
		Object parsed = deserialize(json, fieldDescription);
		if (parsed == null) {
			return null;
		}
		if (!(parsed instanceof JSONArray)) {
			throw new IllegalStateException("Expected a JSON array for "
					+ fieldDescription + ", got: " + parsed.getClass().getName());
		}
		JSONArray arr = (JSONArray) parsed;
		List<Object> list = new ArrayList<>(arr.length());
		for (int i = 0; i < arr.length(); i++) {
			list.add(arr.get(i));
		}
		return list;
	}

	/**
	 * Variant of {@link #deserializeList(String, String)} that yields a typed list of
	 * Jackson-bound POJOs rather than raw {@link JSONObject} elements. Used when the column
	 * is bound on the schema as a typed array ({@code items: { "$ref": "..." }}) and the
	 * generated POJO declares a {@code List<T>} setter; reading back the column has to
	 * produce {@code T} instances directly because the schema-to-pojo writer's typed-array
	 * setter rejects raw {@code Object} elements.
	 *
	 * <p>Returns {@code null} when {@code json} is {@code null}.</p>
	 *
	 * @param json             the stored JSON column value
	 * @param elementType      the Jackson-bindable POJO class for each element
	 * @param fieldDescription human-readable field name used in error messages
	 */
	static <T> List<T> deserializeList(String json, Class<T> elementType, String fieldDescription) {
		if (json == null) {
			return null;
		}
		try {
			return MAPPER.readerForListOf(elementType).readValue(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to deserialize " + fieldDescription
					+ " JSON as List<" + elementType.getSimpleName() + ">", e);
		}
	}

	/**
	 * Render a caller-supplied opaque-JSON value to its canonical JSON-string form for
	 * persistence. Returns {@code null} when {@code value} is {@code null}.
	 *
	 * <p>Single-pipe-via-Jackson: every input shape is converted to a {@link JsonNode}
	 * once via {@link #toJsonNode(Object)}, then written back as a String. See that
	 * method's javadoc for the per-shape branches, including the smart-{@link String}
	 * handling that distinguishes JSON-literal inputs from bare-scalar inputs.</p>
	 *
	 * @throws IllegalArgumentException when the input cannot be rendered as JSON.
	 */
	static String serialize(Object value, String fieldDescription) {
		if (value == null) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(toJsonNode(value));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(
					"Invalid " + fieldDescription + " JSON: " + e.getOriginalMessage(), e);
		} catch (IllegalArgumentException e) {
			// valueToTree wraps non-serializable inputs as IllegalArgumentException; rethrow
			// with the field name so the caller sees which boundary failed.
			throw new IllegalArgumentException(
					"Invalid " + fieldDescription + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Convert any supported caller-side shape to a {@link JsonNode}. Inputs that already
	 * carry a JSON wire form ({@link JSONObject}, {@link JSONArray},
	 * {@link JSONObjectAdapter}) are parsed via {@link ObjectMapper#readTree}; everything
	 * else (Java {@link String} / number / boolean / {@link java.util.Map} /
	 * {@link java.util.Collection}) is value-converted via
	 * {@link ObjectMapper#valueToTree}.
	 *
	 * <p>{@link String} inputs are encoded as JSON scalars (e.g. {@code "qname"} stores
	 * as {@code "qname"}); they are <b>not</b> treated as raw JSON. Callers who want to
	 * store a JSON object or array must pass a {@code Map} / {@code List} /
	 * {@code JSONObject} / {@code JSONArray}.</p>
	 *
	 * <p>Package-private so each branch is independently testable.</p>
	 */
	static JsonNode toJsonNode(Object value) throws JsonProcessingException {
		if (value instanceof JSONObject || value instanceof JSONArray) {
			return MAPPER.readTree(value.toString());
		}
		if (value instanceof JSONObjectAdapter) {
			return MAPPER.readTree(((JSONObjectAdapter) value).toJSONString());
		}
		if (value instanceof Collection) {
			// Walk per-element so JSONObject / JSONArray / JSONObjectAdapter elements survive
			// — Jackson's valueToTree on a Collection<JSONObject> blows up because it has no
			// bean serializer for org.json.JSONObject.
			ArrayNode array = MAPPER.createArrayNode();
			for (Object element : (Collection<?>) value) {
				array.add(element == null ? MAPPER.nullNode() : toJsonNode(element));
			}
			return array;
		}
		return MAPPER.valueToTree(value);
	}
}
