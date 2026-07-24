package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilterDefinition;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Rescore;
import org.opensearch.client.opensearch.core.search.TrackHits;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Unit tests for {@link SearchOpaqueJsonUtil}, organized by the concerns the util
 * exposes: shape conversion ({@link SearchOpaqueJsonUtil#parse},
 * package-private {@link SearchOpaqueJsonUtil#asJsonString}); reference detection
 * ({@link SearchOpaqueJsonUtil#readRef(Object)},
 * {@link SearchOpaqueJsonUtil#readRef(JsonNode)},
 * {@link SearchOpaqueJsonUtil#collectRefs}); inline materialization
 * ({@link SearchOpaqueJsonUtil#toInline}); and the analyzer-typed
 * splice + deserialize surface
 * ({@link SearchOpaqueJsonUtil#resolveAnalyzerSettings}).
 */
public class SearchOpaqueJsonUtilTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** Test-only RoutingContext that maps via {@code nameToId} and reports every column as
	 *  non-text — produces a name-only walker, behaviorally equivalent on a numeric schema. */
	private static SearchFieldRewriter.RoutingContext nameOnly(Function<String, String> nameToId) {
		return new SearchFieldRewriter.RoutingContext() {
			@Override public String mapName(String name) {
				String mapped = nameToId.apply(name);
				return mapped == null ? name : mapped;
			}
			@Override public boolean isTextLike(String columnId) { return false; }
		};
	}

	// ===================== shape conversion =====================

	@Test
	public void testParseWithStringJson() {
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}");

		assertNotNull(root);
		assertEquals("standard", root.at("/analyzer/default/tokenizer").asText());
	}

	@Test
	public void testParseWithMapInput() {
		// settings is schema-typed as opaque object; the POJO surfaces it as a Map for
		// programmatic callers. parse(Object) handles that shape exactly like a String.
		Map<String, Object> settings = Map.of(
				"analyzer", Map.of("default", Map.of("type", "custom", "tokenizer", "standard")));

		JsonNode node = SearchOpaqueJsonUtil.parse(settings);

		assertEquals("standard", node.at("/analyzer/default/tokenizer").asText());
	}

	@Test
	public void testParseWithJsonObjectInput() {
		// Post-DAO shape: an org.json.JSONObject surfaces from the opaque-Object column codec.
		JSONObject settings = new JSONObject().put("k", "v");

		JsonNode node = SearchOpaqueJsonUtil.parse(settings);

		assertEquals("v", node.at("/k").asText());
	}

	@Test
	public void testParseWithMalformedJsonThrows() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.parse("{not valid"));

		assertTrue(e.getMessage().startsWith("Invalid JSON"),
				"error must surface a user-facing message: " + e.getMessage());
	}

	@Test
	public void testParseWithNullThrows() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.parse(null));

		assertTrue(e.getMessage().contains("required"));
	}

	// --- asJsonString (package-private dispatch) ---

	@Test
	public void testAsJsonStringWithStringPassesThrough() {
		// String inputs are JSON literals; the helper must not double-encode.
		String s = "{\"a\":1}";

		assertSame(s, SearchOpaqueJsonUtil.asJsonString(s));
	}

	@Test
	public void testAsJsonStringWithJsonObject() {
		String out = SearchOpaqueJsonUtil.asJsonString(new JSONObject().put("a", 1));

		assertEquals(1, new JSONObject(out).getInt("a"));
	}

	@Test
	public void testAsJsonStringWithJsonArray() {
		String out = SearchOpaqueJsonUtil.asJsonString(new JSONArray().put(1).put(2));

		assertEquals(2, new JSONArray(out).length());
	}

	@Test
	public void testAsJsonStringWithJsonObjectAdapter() throws Exception {
		// Post-controller shape: schema-to-pojo wraps inbound objects in JSONObjectAdapter.
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl("{\"a\":1}");

		String out = SearchOpaqueJsonUtil.asJsonString(adapter);

		assertEquals(1, new JSONObject(out).getInt("a"));
	}

	@Test
	public void testAsJsonStringWithJSONEntityHoldingAdapterField() throws Exception {
		// Post-round-trip production shape: the async framework deserializes the request
		// body via EntityFactory, so SearchQuery's opaque "sort" elements surface as
		// JSONObjectAdapter values. asJsonString must render the whole entity through the schema
		// adapter — Jackson cannot serialize the nested JSONObjectAdapterImpl.
		SearchQuery body = new SearchQuery()
				.setSort(java.util.Arrays.asList(
						new JSONObjectAdapterImpl("{\"year\":{\"order\":\"desc\"}}")));

		String out = SearchOpaqueJsonUtil.asJsonString(body);

		assertEquals("desc",
				new JSONObject(out).getJSONArray("sort").getJSONObject(0)
						.getJSONObject("year").getString("order"));
	}

	@Test
	public void testAsJsonStringWithJSONEntityThatFailsToSerializeThrows() {
		// A JSONEntity whose schema serialization fails surfaces the JSONObjectAdapterException
		// as an IllegalArgumentException carrying the "Invalid JSON" marker.
		JSONEntity failing = new JSONEntity() {
			@Override
			public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter)
					throws JSONObjectAdapterException {
				throw new JSONObjectAdapterException("boom");
			}
			@Override
			public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter)
					throws JSONObjectAdapterException {
				throw new JSONObjectAdapterException("boom");
			}
		};

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.asJsonString(failing));

		assertTrue(e.getMessage().contains("Invalid JSON"),
				"surfaced error must carry the Invalid JSON marker: " + e.getMessage());
	}

	// --- response-path opaque outputs must survive async-response serialization ---
	// The async framework serializes SearchQueryResults via EntityFactory (the schema
	// adapter). Its putObject rejects java.util.Map, so any opaque response slot the
	// manager populates must be a JSONObject (object slot) or scalars (list slot).

	@Test
	public void testSerializeAggregationsResultSurvivesAdapterSerialization() throws Exception {
		Map<String, org.opensearch.client.opensearch._types.aggregations.Aggregate> aggs = new LinkedHashMap<>();
		aggs.put("by_status", org.opensearch.client.opensearch._types.aggregations.Aggregate.of(
				a -> a.sterms(st -> st.buckets(b -> b.array(Collections.emptyList())))));

		Object aggResults = SearchOpaqueJsonUtil.serializeAggregations(aggs, Function.identity());
		SearchQueryResults results = new SearchQueryResults().setAggregationResults(aggResults);

		// call under test — must not throw JSONObjectAdapterException (the IT-path 500)
		String json = org.sagebionetworks.schema.adapter.org.json.EntityFactory
				.createJSONStringForEntity(results);

		assertTrue(new JSONObject(json).getJSONObject("aggregationResults").has("by_status"));
	}

	@Test
	public void testToSearchAfterCursorResultSurvivesAdapterSerialization() throws Exception {
		// A sort over a scalar field yields scalar cursor elements — the schema adapter's
		// putObjectArray accepts those, so the cursor must round-trip cleanly.
		List<org.opensearch.client.opensearch._types.FieldValue> sortValues = Arrays.asList(
				org.opensearch.client.opensearch._types.FieldValue.of(42L),
				org.opensearch.client.opensearch._types.FieldValue.of("syn123"));

		List<Object> cursor = SearchOpaqueJsonUtil.toSearchAfterCursor(sortValues);
		SearchQueryResults results = new SearchQueryResults().setNextSearchAfter(cursor);

		// call under test — must not throw JSONObjectAdapterException
		String json = org.sagebionetworks.schema.adapter.org.json.EntityFactory
				.createJSONStringForEntity(results);

		assertEquals(2, new JSONObject(json).getJSONArray("nextSearchAfter").length());
	}

	@Test
	public void testAsJsonStringWithMap() {
		Map<String, Object> in = new LinkedHashMap<>();
		in.put("a", 1);

		String out = SearchOpaqueJsonUtil.asJsonString(in);

		assertEquals(1, new JSONObject(out).getInt("a"));
	}

	@Test
	public void testAsJsonStringWithCollection() {
		String out = SearchOpaqueJsonUtil.asJsonString(Arrays.asList(1, 2, 3));

		assertEquals(3, new JSONArray(out).length());
	}

	@Test
	public void testAsJsonStringWithScalar() {
		// Scalars route through Jackson's writeValueAsString and emit a JSON scalar literal.
		assertEquals("42", SearchOpaqueJsonUtil.asJsonString(42));
		assertEquals("true", SearchOpaqueJsonUtil.asJsonString(Boolean.TRUE));
	}

	@Test
	public void testAsJsonStringWithNonSerializableThrows() {
		// Self-referential structure isn't serializable as JSON; surfaced as
		// IllegalArgumentException with a "Invalid JSON" prefix so the boundary
		// failure is identifiable.
		Object[] selfRef = new Object[1];
		selfRef[0] = selfRef;

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.asJsonString(selfRef));

		assertTrue(e.getMessage().contains("Invalid JSON"),
				"surfaced error must carry the Invalid JSON marker: " + e.getMessage());
	}

	// ===================== reference detection =====================

	// --- readRef(Object) ---

	@Test
	public void testReadRefObjectWithMapRef() {
		assertEquals("biomed-FOO",
				SearchOpaqueJsonUtil.readRef(Map.of("$ref", "biomed-FOO")));
	}

	@Test
	public void testReadRefObjectWithJsonObjectRef() {
		// JSONObject is the post-DAO shape of an opaque-Object {"$ref": "..."} value.
		assertEquals("biomed-FOO",
				SearchOpaqueJsonUtil.readRef(new JSONObject().put("$ref", "biomed-FOO")));
	}

	@Test
	public void testReadRefObjectWithJsonObjectAdapterRef() throws Exception {
		// Controllers receive opaque-Object fields as a JSONObjectAdapter after JSON binding;
		// readRef must parse its serialized form and extract the $ref qname.
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl("{\"$ref\":\"biomed-FOO\"}");
		assertEquals("biomed-FOO", SearchOpaqueJsonUtil.readRef(adapter));
	}

	@Test
	public void testReadRefObjectRejectsMultiKeyMap() {
		// Multi-key map — $ref alongside other fields — is inline, not a ref.
		assertNull(SearchOpaqueJsonUtil.readRef(
				Map.of("$ref", "biomed-FOO", "extra", "stuff")));
	}

	@Test
	public void testReadRefObjectRejectsMultiKeyJsonObject() {
		JSONObject inline = new JSONObject().put("$ref", "biomed-FOO").put("extra", "stuff");
		assertNull(SearchOpaqueJsonUtil.readRef(inline));
	}

	@Test
	public void testReadRefObjectRejectsEmptyMap() {
		assertNull(SearchOpaqueJsonUtil.readRef(Collections.emptyMap()));
	}

	@Test
	public void testReadRefObjectRejectsEmptyJsonObject() {
		assertNull(SearchOpaqueJsonUtil.readRef(new JSONObject()));
	}

	@Test
	public void testReadRefObjectRejectsRefWithNonStringValueInMap() {
		// $ref values must be strings; a numeric value isn't a qname.
		assertNull(SearchOpaqueJsonUtil.readRef(Map.of("$ref", 7)));
	}

	@Test
	public void testReadRefObjectRejectsRefWithNonStringValueInJsonObject() {
		assertNull(SearchOpaqueJsonUtil.readRef(new JSONObject().put("$ref", 7)));
	}

	@Test
	public void testReadRefObjectRejectsString() {
		// A bare String scalar (e.g. a plain qname) is not a ref object — it's a scalar.
		assertNull(SearchOpaqueJsonUtil.readRef("biomed-FOO"));
	}

	@Test
	public void testReadRefObjectRejectsScalarTypes() {
		assertNull(SearchOpaqueJsonUtil.readRef(42));
		assertNull(SearchOpaqueJsonUtil.readRef(Boolean.TRUE));
	}

	@Test
	public void testReadRefObjectRejectsNull() {
		assertNull(SearchOpaqueJsonUtil.readRef((Object) null));
	}

	@Test
	public void testReadRefObjectRejectsMapWithSizeOneButWrongKey() {
		// One-key Map whose key isn't $ref is not a ref.
		assertNull(SearchOpaqueJsonUtil.readRef(Map.of("type", "stop")));
	}

	// --- readRef(JsonNode) ---

	@Test
	public void testReadRefJsonNodeWithRef() throws Exception {
		JsonNode node = MAPPER.readTree("{\"$ref\":\"biomed-FOO\"}");

		assertEquals("biomed-FOO", SearchOpaqueJsonUtil.readRef(node));
	}

	@Test
	public void testReadRefJsonNodeWithNullReturnsNull() {
		assertNull(SearchOpaqueJsonUtil.readRef((JsonNode) null));
	}

	@Test
	public void testReadRefJsonNodeWithNonObjectReturnsNull() {
		// A non-object JsonNode (a string, a number, an array) is not a ref.
		assertNull(SearchOpaqueJsonUtil.readRef(JsonNodeFactory.instance.textNode("biomed-FOO")));
		assertNull(SearchOpaqueJsonUtil.readRef(JsonNodeFactory.instance.numberNode(42)));
		assertNull(SearchOpaqueJsonUtil.readRef(JsonNodeFactory.instance.arrayNode()));
	}

	@Test
	public void testReadRefJsonNodeWithMultiKeyObjectReturnsNull() throws Exception {
		JsonNode node = MAPPER.readTree("{\"$ref\":\"x\",\"type\":\"stop\"}");

		assertNull(SearchOpaqueJsonUtil.readRef(node));
	}

	@Test
	public void testReadRefJsonNodeWithEmptyObjectReturnsNull() throws Exception {
		assertNull(SearchOpaqueJsonUtil.readRef(MAPPER.readTree("{}")));
	}

	@Test
	public void testReadRefJsonNodeWithoutRefKeyReturnsNull() throws Exception {
		// One-key object whose key isn't $ref is not a ref.
		JsonNode node = MAPPER.readTree("{\"type\":\"stop\"}");

		assertNull(SearchOpaqueJsonUtil.readRef(node));
	}

	@Test
	public void testReadRefJsonNodeWithNonTextualRefValueReturnsNull() throws Exception {
		JsonNode node = MAPPER.readTree("{\"$ref\":42}");

		assertNull(SearchOpaqueJsonUtil.readRef(node));
	}

	// --- collectRefs ---

	@Test
	public void testCollectRefsReturnsRefsInWalkOrderDeduplicated() {
		String json = "{"
				+ "\"filter\":{"
				+ "\"a_syn\":{\"$ref\":\"org-A\"},"
				+ "\"b_syn\":{\"$ref\":\"org-B\"},"
				+ "\"c_syn\":{\"$ref\":\"org-A\"}"
				+ "}}";

		Set<String> refs = SearchOpaqueJsonUtil.collectRefs(SearchOpaqueJsonUtil.parse(json));

		assertEquals(new LinkedHashSet<>(Arrays.asList("org-A", "org-B")), refs);
	}

	@Test
	public void testCollectRefsReturnsEmptyWhenNoRefs() {
		Set<String> refs = SearchOpaqueJsonUtil.collectRefs(SearchOpaqueJsonUtil.parse(
				"{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}"));

		assertTrue(refs.isEmpty());
	}

	@Test
	public void testCollectRefsIgnoresStringValuesInsideArrays() {
		// Strings inside chain arrays look textually similar to qnames but must NOT be
		// treated as refs — only object nodes with a "$ref" field are refs.
		String json = "{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\","
				+ "\"filter\":[\"lowercase\",\"org-pretend\"]}}}";

		Set<String> refs = SearchOpaqueJsonUtil.collectRefs(SearchOpaqueJsonUtil.parse(json));

		assertTrue(refs.isEmpty());
	}

	@Test
	public void testCollectRefsWithNullRootReturnsEmpty() {
		// Defensive null guard so callers that already failed to parse don't NPE.
		assertTrue(SearchOpaqueJsonUtil.collectRefs(null).isEmpty());
	}

	@Test
	public void testCollectRefsIgnoresNonTextualRefValue() throws Exception {
		// A {"$ref": <number>} entry is malformed; collectRefs must not coerce a number
		// to a bogus qname.
		JsonNode root = MAPPER.readTree("{\"filter\":{\"bogus\":{\"$ref\":42}}}");

		assertTrue(SearchOpaqueJsonUtil.collectRefs(root).isEmpty());
	}

	// ===================== inline materialization =====================

	@Test
	public void testToInlineWithNullReturnsNull() {
		assertNull(SearchOpaqueJsonUtil.toInline(null, TextAnalyzer.class));
	}

	@Test
	public void testToInlineConvertsMapToTypedPojo() {
		TextAnalyzer ta = SearchOpaqueJsonUtil.toInline(
				Map.of("organizationName", "biomed", "name", "publications",
						"settings", Map.of("k", "v")),
				TextAnalyzer.class);

		assertEquals("biomed", ta.getOrganizationName());
		assertEquals("publications", ta.getName());
		assertTrue(ta.getSettings() instanceof Map);
	}

	@Test
	public void testToInlineConvertsJsonObjectToTypedPojo() {
		TextAnalyzer ta = SearchOpaqueJsonUtil.toInline(
				new JSONObject().put("organizationName", "biomed").put("name", "publications"),
				TextAnalyzer.class);

		assertEquals("biomed", ta.getOrganizationName());
		assertEquals("publications", ta.getName());
	}

	@Test
	public void testToInlineConvertsJsonArrayToTypedList() {
		// JSONArray inputs route through readValue(toString) for the same canonical form.
		List<?> list = SearchOpaqueJsonUtil.toInline(
				new JSONArray().put("a").put("b"), List.class);

		assertEquals(Arrays.asList("a", "b"), list);
	}

	@Test
	public void testToInlineConvertsListToTypedList() {
		List<?> list = SearchOpaqueJsonUtil.toInline(Arrays.asList("a", "b"), List.class);

		assertEquals(Arrays.asList("a", "b"), list);
	}

	@Test
	public void testToInlineRejectsIncompatibleMapShape() {
		// A map whose fields cannot deserialize as the target POJO surfaces a user-facing
		// IllegalArgumentException at create / update time rather than later corruption.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.toInline(Map.of("createdBy", Map.of("not", "a-string")),
						TextAnalyzer.class));

		assertTrue(ex.getMessage().contains("Invalid inline TextAnalyzer"), ex.getMessage());
	}

	@Test
	public void testToInlineRejectsMalformedJsonObject() throws Exception {
		// JSONObject input that doesn't deserialize as the target POJO must surface as
		// IllegalArgumentException — same contract as Map inputs.
		JSONObject incompatible = new JSONObject().put("createdBy",
				new JSONObject().put("not", "a-string"));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.toInline(incompatible, TextAnalyzer.class));

		assertTrue(ex.getMessage().contains("Invalid inline TextAnalyzer"), ex.getMessage());
	}

	@Test
	public void testToInlineConvertsJsonObjectAdapterToTypedPojo() throws Exception {
		// JSONObjectAdapter is the wire-deserialized shape controllers receive for opaque-Object
		// fields after JSON binding. Without the JSONObjectAdapter branch in toInline, this falls
		// through to MAPPER.convertValue, which tries to serialize JSONObjectAdapterImpl via
		// Jackson (no BeanSerializer) and throws "No serializer found for class JSONObjectAdapterImpl".
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(
				"{\"organizationName\":\"biomed\",\"name\":\"publications\"}");

		// call under test
		TextAnalyzer ta = SearchOpaqueJsonUtil.toInline(adapter, TextAnalyzer.class);

		assertEquals("biomed", ta.getOrganizationName());
		assertEquals("publications", ta.getName());
	}

	@Test
	public void testToInlineRejectsMalformedJsonObjectAdapter() throws Exception {
		// A JSONObjectAdapter whose payload doesn't deserialize as the target POJO must surface
		// as IllegalArgumentException — same contract as Map / JSONObject inputs.
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(
				"{\"createdBy\":{\"not\":\"a-string\"}}");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.toInline(adapter, TextAnalyzer.class));

		assertTrue(ex.getMessage().contains("Invalid inline TextAnalyzer"), ex.getMessage());
	}

	// ===================== analyzer-typed splice + deserialize =====================

	@Test
	public void testResolveAnalyzerSettingsWithNullRootReturnsNull() {
		// Null in, null out — paired with spliceRefsInFilterMap's null-root short-circuit
		// so callers that already failed to parse don't NPE here.
		assertNull(SearchOpaqueJsonUtil.resolveAnalyzerSettings(null, q -> null));
	}

	@Test
	public void testResolveAnalyzerSettingsWithNoFilterMapDeserializes() {
		// A settings tree with no `filter` map at all must still deserialize — the splice
		// phase short-circuits and the typed deserializer handles the rest.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}");

		IndexSettingsAnalysis resolved = SearchOpaqueJsonUtil.resolveAnalyzerSettings(
				root, q -> { throw new AssertionError("resolver must not be called"); });

		assertNotNull(resolved.analyzer().get("default"));
		assertTrue(resolved.analyzer().get("default").isCustom());
	}

	@Test
	public void testResolveAnalyzerSettingsWithEmptyFilterMapDeserializes() {
		// Empty filter map: splice phase iterates zero entries and the deserializer
		// produces an empty typed filter map.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{},\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}");

		IndexSettingsAnalysis resolved = SearchOpaqueJsonUtil.resolveAnalyzerSettings(
				root, q -> { throw new AssertionError("resolver must not be called"); });

		assertTrue(resolved.filter().isEmpty());
	}

	@Test
	public void testResolveAnalyzerSettingsSplicesSingleRef() throws Exception {
		// Splice phase resolves the ref, replacing the {"$ref": ...} object with the
		// resolved synonym definition before the typed deserializer runs.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{\"med_syn\":{\"$ref\":\"biomed-medical_terms\"}}}");
		JsonNode synonymDef = MAPPER.readTree(
				"{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");

		Function<String, JsonNode> resolver = qname ->
				"biomed-medical_terms".equals(qname) ? synonymDef : null;

		IndexSettingsAnalysis resolved = SearchOpaqueJsonUtil.resolveAnalyzerSettings(root, resolver);

		TokenFilter med = resolved.filter().get("med_syn");
		assertNotNull(med);
		TokenFilterDefinition def = med.definition();
		assertNotNull(def);
		assertTrue(def.isSynonymGraph());
	}

	@Test
	public void testResolveAnalyzerSettingsSplicesMultipleRefsInSamePass() throws Exception {
		// Multiple refs in the same filter map all resolve in one pass; the resolver is
		// called once per ref and the final tree carries each substituted definition.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{"
				+ "\"med\":{\"$ref\":\"biomed-medical\"},"
				+ "\"stop\":{\"$ref\":\"biomed-stop\"}"
				+ "}}");
		JsonNode medDef = MAPPER.readTree("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");
		JsonNode stopDef = MAPPER.readTree("{\"type\":\"stop\",\"stopwords\":\"_english_\"}");

		IndexSettingsAnalysis resolved = SearchOpaqueJsonUtil.resolveAnalyzerSettings(root,
				qname -> {
					switch (qname) {
						case "biomed-medical": return medDef;
						case "biomed-stop": return stopDef;
						default: return null;
					}
				});

		assertTrue(resolved.filter().get("med").definition().isSynonymGraph());
		assertTrue(resolved.filter().get("stop").definition().isStop());
	}

	@Test
	public void testResolveAnalyzerSettingsLeavesInlineEntriesUntouched() throws Exception {
		// An inline filter definition (no $ref) sitting alongside a ref must not be
		// touched. The resolver must be called exactly once — for the ref entry — and the
		// inline entry must round-trip verbatim.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{"
				+ "\"english_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"},"
				+ "\"med_syn\":{\"$ref\":\"biomed-medical\"}"
				+ "}}");
		JsonNode synonymDef = MAPPER.readTree(
				"{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");

		int[] resolverCallCount = { 0 };
		IndexSettingsAnalysis resolved = SearchOpaqueJsonUtil.resolveAnalyzerSettings(root,
				qname -> {
					resolverCallCount[0]++;
					return "biomed-medical".equals(qname) ? synonymDef : null;
				});

		assertEquals(1, resolverCallCount[0],
				"resolver must be called exactly once — for the ref entry only");
		assertTrue(resolved.filter().get("english_stop").definition().isStop());
		assertTrue(resolved.filter().get("med_syn").definition().isSynonymGraph());
	}

	@Test
	public void testResolveAnalyzerSettingsMissingTargetThrowsWithPointer() {
		// A null resolver result for a present $ref must surface an IllegalArgumentException
		// that names both the qname and the JSON pointer to the offending entry — this is
		// what shows up at the user/API boundary on a missing SynonymSet.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{\"ghost\":{\"$ref\":\"org-Ghost\"}}}");

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.resolveAnalyzerSettings(root, qname -> null));

		assertTrue(e.getMessage().contains("Unresolved $ref"));
		assertTrue(e.getMessage().contains("org-Ghost"));
		assertTrue(e.getMessage().contains("/filter/ghost"),
				"message must include the JSON pointer to the offending entry: " + e.getMessage());
	}

	@Test
	public void testResolveAnalyzerSettingsWithNonObjectFilterValueIgnoresEntry() {
		// A filter entry whose value is not an object (e.g. an inline `stop` definition
		// shape with a top-level type) is left alone by the splice. The resolver must not
		// be called.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{\"my_filter\":{\"type\":\"stop\",\"stopwords\":\"_english_\"}}}");

		IndexSettingsAnalysis resolved = SearchOpaqueJsonUtil.resolveAnalyzerSettings(root,
				q -> { throw new AssertionError("resolver must not be called for non-ref entries"); });

		assertNotNull(resolved.filter().get("my_filter"));
	}

	@Test
	public void testResolveAnalyzerSettingsWithMultiKeyObjectIsNotARef() {
		// readRef rejects objects whose size != 1, so a filter entry that has $ref alongside
		// other keys is treated as inline — the resolver must not be invoked. The typed
		// deserializer ignores the extra $ref key and parses the entry as a regular filter.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{\"my_filter\":{\"$ref\":\"org-X\",\"type\":\"stop\","
				+ "\"stopwords\":\"_english_\"}}}");

		IndexSettingsAnalysis resolved = SearchOpaqueJsonUtil.resolveAnalyzerSettings(root,
				q -> { throw new AssertionError("resolver must not be called for non-ref shape"); });

		assertNotNull(resolved.filter().get("my_filter"));
	}

	@Test
	public void testResolveAnalyzerSettingsWithNonTextualRefValueIsNotARef() {
		// A {"$ref": <number>} entry doesn't match the textual-$ref shape. readRef returns
		// null, the splice skips it, and the typed deserializer surfaces the shape error.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{\"bogus\":{\"$ref\":42}}}");

		assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.resolveAnalyzerSettings(root,
						q -> { throw new AssertionError("resolver must not be called"); }));
	}

	@Test
	public void testResolveAnalyzerSettingsWithNonObjectFilterMapDeserializerThrows() {
		// A non-object filter value (string, number) at the top-level FILTER_KEY position
		// fails the splice's isObject() guard, so resolveAnalyzerSettings skips substitution
		// and lets the typed deserializer surface the shape error — wrapped as an
		// IllegalArgumentException by the analyzer-typed deserialize step.
		JsonNode root = SearchOpaqueJsonUtil.parse("{\"filter\":\"not_an_object\"}");

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.resolveAnalyzerSettings(root, q -> null));

		assertTrue(e.getMessage().contains("Invalid analyzer settings"),
				"deserialize errors must be rewrapped as Invalid analyzer settings: " + e.getMessage());
	}

	@Test
	public void testResolveAnalyzerSettingsWithMalformedFilterDefinitionThrows() throws Exception {
		// The splice itself succeeds (the spliced node is valid JSON), but the OpenSearch
		// typed deserializer rejects the resulting filter shape ("type": "not_a_real_filter")
		// — that error must be rewrapped as IllegalArgumentException so the manager-side
		// validation surfaces it at create / update time.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"filter\":{\"bad\":{\"$ref\":\"org-bad\"}}}");
		JsonNode badDef = MAPPER.readTree("{\"type\":\"not_a_real_filter_kind\"}");

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchOpaqueJsonUtil.resolveAnalyzerSettings(root,
						qname -> badDef));

		assertTrue(e.getMessage().contains("Invalid analyzer settings"),
				"typed-deserializer errors must surface as Invalid analyzer settings: " + e.getMessage());
	}

	@Test
	public void testResolveAnalyzerSettingsAnalyzerChainArrayPreservedVerbatim() {
		// The splice only touches the filter map. Analyzer-chain arrays (which can contain
		// strings that look qname-shaped) must not be rewritten.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\","
				+ "\"filter\":[\"lowercase\",\"my_filter\"]}},"
				+ "\"filter\":{\"my_filter\":{\"$ref\":\"org-X\"}}}");
		JsonNode resolved = SearchOpaqueJsonUtil.parse(
				"{\"type\":\"stop\",\"stopwords\":\"_english_\"}");

		IndexSettingsAnalysis result = SearchOpaqueJsonUtil.resolveAnalyzerSettings(
				root, qname -> "org-X".equals(qname) ? resolved : null);

		assertEquals(Arrays.asList("lowercase", "my_filter"),
				result.analyzer().get("default").custom().filter());
	}

	// ===================== toInlineAnalyzerSettings =====================

	@Test
	public void testToInlineAnalyzerSettingsWithBareBlockMap() {
		// Curator-supplied bare OpenSearch settings.analysis block (no envelope).
		// Single round-trip exercising every allowed root key (char_filter / tokenizer /
		// filter / analyzer) so a regression in any one branch shows up here.
		Map<String, Object> bareBlock = Map.of(
				"char_filter", Map.of("strip", Map.of("type", "html_strip")),
				"tokenizer",   Map.of("std",   Map.of("type", "standard")),
				"filter",      Map.of("english_stop", Map.of("type", "stop", "stopwords", "_english_")),
				"analyzer",    Map.of("default", Map.of(
						"type", "custom",
						"tokenizer", "std",
						"char_filter", List.of("strip"),
						"filter", List.of("lowercase", "english_stop"))));

		// call under test
		IndexSettingsAnalysis result = SearchOpaqueJsonUtil.toInlineAnalyzerSettings(bareBlock,
				qname -> null);

		assertNotNull(result);
		assertTrue(result.charFilter().containsKey("strip"));
		assertTrue(result.tokenizer().containsKey("std"));
		assertTrue(result.filter().containsKey("english_stop"));
		assertEquals("std", result.analyzer().get("default").custom().tokenizer());
	}

	@Test
	public void testToInlineAnalyzerSettingsWithJSONObject() {
		JSONObject bareBlock = new JSONObject().put("analyzer", new JSONObject()
				.put("default", new JSONObject()
						.put("type", "custom")
						.put("tokenizer", "standard")));

		// call under test
		IndexSettingsAnalysis result = SearchOpaqueJsonUtil.toInlineAnalyzerSettings(bareBlock,
				qname -> null);

		assertNotNull(result);
		assertEquals("standard", result.analyzer().get("default").custom().tokenizer());
	}

	@Test
	public void testToInlineAnalyzerSettingsWithNullReturnsNull() {
		// call under test
		assertNull(SearchOpaqueJsonUtil.toInlineAnalyzerSettings(null, qname -> null));
	}

	@Test
	public void testToInlineAnalyzerSettingsWithUnresolvedRefThrows() {
		// At create-time the resolver always returns null, so any $ref inside the inline
		// literal surfaces as IllegalArgumentException — matching the schema contract that
		// SynonymSet refs are not permitted inside an inline analyzer slot.
		Map<String, Object> withRef = Map.of(
				"filter",   Map.of("syn", Map.of("$ref", "org-NOT_RESOLVABLE")),
				"analyzer", Map.of("default", Map.of(
						"type", "custom",
						"tokenizer", "standard",
						"filter", List.of("lowercase", "syn"))));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchOpaqueJsonUtil.toInlineAnalyzerSettings(withRef, qname -> null));
		assertTrue(ex.getMessage().contains("org-NOT_RESOLVABLE"),
				"expected message to identify the unresolved ref, got: " + ex.getMessage());
	}

	@Test
	public void testToInlineAnalyzerSettingsWithResolverSplicesRef() {
		// At build-time the resolver returns the SynonymSet definition; the spliced filter
		// map must reflect the resolved type, proving the resolver is honored.
		Map<String, Object> withRef = Map.of(
				"filter",   Map.of("syn", Map.of("$ref", "biomed-medical_terms")),
				"analyzer", Map.of("default", Map.of(
						"type", "custom",
						"tokenizer", "standard",
						"filter", List.of("lowercase", "syn"))));
		JsonNode synonymSet = SearchOpaqueJsonUtil.parse(
				"{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");

		// call under test
		IndexSettingsAnalysis result = SearchOpaqueJsonUtil.toInlineAnalyzerSettings(withRef,
				qname -> "biomed-medical_terms".equals(qname) ? synonymSet : null);

		TokenFilter spliced = result.filter().get("syn");
		assertNotNull(spliced);
		TokenFilterDefinition def = spliced.definition();
		assertNotNull(def, "spliced filter should be a typed token-filter definition");
	}

	// ===================== constants =====================

	@Test
	public void testRefKeyConstant() {
		// REF_KEY must stay $ref to remain compatible with the OpenSearch / curator-facing
		// convention. If this constant ever drifts, every $ref recognition path breaks.
		assertEquals("$ref", SearchOpaqueJsonUtil.REF_KEY);
	}

	@Test
	public void testFromJsonpTreeDeserializesTypedQuery() throws Exception {
		JsonNode node = new ObjectMapper().readTree("{\"match\":{\"abstract\":\"amyloid\"}}");
		// call under test
		org.opensearch.client.opensearch._types.query_dsl.Query query = SearchOpaqueJsonUtil.fromJsonpTree(
				node, org.opensearch.client.opensearch._types.query_dsl.Query._DESERIALIZER);
		assertTrue(query.isMatch());
		assertEquals("abstract", query.match().field());
	}

	@Test
	public void testToJsonpTreeSerializesTypedValue() {
		org.opensearch.client.opensearch._types.FieldValue value =
				org.opensearch.client.opensearch._types.FieldValue.of(42L);
		// call under test
		JsonNode node = SearchOpaqueJsonUtil.toJsonpTree(value);
		assertEquals(42L, node.asLong());
	}

	@Test
	public void testJsonpTreeRoundTrips() {
		org.opensearch.client.opensearch._types.aggregations.Aggregation agg =
				org.opensearch.client.opensearch._types.aggregations.Aggregation.of(a -> a.avg(av -> av.field("score")));
		// call under test — serialize then deserialize back through the typed bridge
		JsonNode node = SearchOpaqueJsonUtil.toJsonpTree(agg);
		org.opensearch.client.opensearch._types.aggregations.Aggregation back = SearchOpaqueJsonUtil.fromJsonpTree(
				node, org.opensearch.client.opensearch._types.aggregations.Aggregation._DESERIALIZER);
		assertTrue(back.isAvg());
		assertEquals("score", back.avg().field());
	}

	// ===================== parseRequiredQuery =====================

	@Test
	public void testParseRequiredQueryWithMissingThrows() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"size\":10}");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchOpaqueJsonUtil.parseRequiredQuery(body, nameOnly(Function.identity()), false));
		assertTrue(ex.getMessage().contains("body.query"));
	}

	@Test
	public void testParseRequiredQueryWithNullNodeThrows() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"query\":null}");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchOpaqueJsonUtil.parseRequiredQuery(body, nameOnly(Function.identity()), false));
		assertTrue(ex.getMessage().contains("body.query"));
	}

	@Test
	public void testParseRequiredQueryWithPresentReturnsQuery() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"query\":{\"match_all\":{}}}");
		// call under test
		Query query = SearchOpaqueJsonUtil.parseRequiredQuery(body, nameOnly(Function.identity()), false);
		assertNotNull(query);
		assertTrue(query.isMatchAll());
	}

	// ===================== parseAggregations =====================

	@Test
	public void testParseAggregationsWithAbsentReturnsEmpty() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"query\":{\"match_all\":{}}}");
		// call under test
		Map<String, Aggregation> aggs =
				SearchOpaqueJsonUtil.parseAggregations(body, nameOnly(Function.identity()));
		assertTrue(aggs.isEmpty());
	}

	@Test
	public void testParseAggregationsWithNullNodeReturnsEmpty() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"aggregations\":null}");
		// call under test
		Map<String, Aggregation> aggs =
				SearchOpaqueJsonUtil.parseAggregations(body, nameOnly(Function.identity()));
		assertTrue(aggs.isEmpty());
	}

	@Test
	public void testParseAggregationsWithPresentBuildsMap() {
		JsonNode body = SearchOpaqueJsonUtil.parse(
				"{\"aggregations\":{\"by_year\":{\"terms\":{\"field\":\"year\"}}}}");
		// call under test
		Map<String, Aggregation> aggs =
				SearchOpaqueJsonUtil.parseAggregations(body, nameOnly(Function.identity()));
		assertEquals(1, aggs.size());
		assertTrue(aggs.get("by_year").isTerms());
	}

	// ===================== parseSearchAfter =====================

	@Test
	public void testParseSearchAfterWithAbsentReturnsEmpty() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"query\":{\"match_all\":{}}}");
		// call under test
		assertTrue(SearchOpaqueJsonUtil.parseSearchAfter(body).isEmpty());
	}

	@Test
	public void testParseSearchAfterWithNullNodeReturnsEmpty() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"search_after\":null}");
		// call under test
		assertTrue(SearchOpaqueJsonUtil.parseSearchAfter(body).isEmpty());
	}

	@Test
	public void testParseSearchAfterWithArrayCollects() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"search_after\":[\"abc\",100]}");
		// call under test
		List<FieldValue> values = SearchOpaqueJsonUtil.parseSearchAfter(body);
		assertEquals(2, values.size());
	}

	// ===================== parseSort =====================

	@Test
	public void testParseSortWithAbsentInjectsScoreDescending() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"query\":{\"match_all\":{}}}");
		// call under test
		List<SortOptions> sort = SearchOpaqueJsonUtil.parseSort(body, nameOnly(Function.identity()));
		assertEquals(1, sort.size());
		assertEquals("_score", sort.get(0).field().field());
		assertEquals(SortOrder.Desc, sort.get(0).field().order());
	}

	@Test
	public void testParseSortWithBareScoreStringNotWrapped() {
		// A bare "_score" is left as a scalar (not wrapped + walked) and deserializes to the
		// Score variant of SortOptions, not a field sort.
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"sort\":\"_score\"}");
		// call under test
		List<SortOptions> sort = SearchOpaqueJsonUtil.parseSort(body, nameOnly(Function.identity()));
		assertEquals(1, sort.size());
		assertTrue(sort.get(0).isScore());
	}

	@Test
	public void testParseSortWithBareColumnStringWrapped() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"sort\":\"title\"}");
		// call under test
		List<SortOptions> sort = SearchOpaqueJsonUtil.parseSort(body, nameOnly(Function.identity()));
		assertEquals(1, sort.size());
		assertEquals("title", sort.get(0).field().field());
	}

	@Test
	public void testParseSortWithArrayOfElements() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"sort\":[\"title\",{\"year\":\"desc\"}]}");
		// call under test
		List<SortOptions> sort = SearchOpaqueJsonUtil.parseSort(body, nameOnly(Function.identity()));
		assertEquals(2, sort.size());
	}

	@Test
	public void testParseSortWithSingleObject() {
		JsonNode body = SearchOpaqueJsonUtil.parse("{\"sort\":{\"year\":{\"order\":\"asc\"}}}");
		// call under test
		List<SortOptions> sort = SearchOpaqueJsonUtil.parseSort(body, nameOnly(Function.identity()));
		assertEquals(1, sort.size());
		assertEquals("year", sort.get(0).field().field());
		assertEquals(SortOrder.Asc, sort.get(0).field().order());
	}

	@Test
	public void testParseSortWithFieldAndModeAndMissing() {
		// Native field sort carrying the full option set (mode + missing) round-trips.
		JsonNode body = SearchOpaqueJsonUtil.parse(
				"{\"sort\":[{\"year\":{\"order\":\"asc\",\"mode\":\"min\",\"missing\":\"_last\"}}]}");
		// call under test
		List<SortOptions> sort = SearchOpaqueJsonUtil.parseSort(body, nameOnly(Function.identity()));
		assertEquals(1, sort.size());
		assertEquals("year", sort.get(0).field().field());
		assertEquals(SortOrder.Asc, sort.get(0).field().order());
	}

	@Test
	public void testParseSortWithScriptSortRejected() {
		// A native _script sort deserializes to the Script kind, which is not on
		// ALLOWED_SORT_KINDS — validateSort rejects it (replacing the old sanitizer blocklist).
		JsonNode body = SearchOpaqueJsonUtil.parse(
				"{\"sort\":[{\"_script\":{\"type\":\"number\","
						+ "\"script\":{\"source\":\"doc['x'].value\"},\"order\":\"asc\"}}]}");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchOpaqueJsonUtil.parseSort(body, nameOnly(Function.identity())));
		assertTrue(ex.getMessage().contains("sort kind is not allowed"));
	}

	@Test
	public void testParseSortWithGeoDistanceSortRejected() {
		// A _geo_distance sort never reaches OpenSearch: its geo-location body fails the typed
		// SortOptions deserializer outright (Synapse indexes have no geo fields), so it is
		// rejected with a RuntimeException before the validateSort kind allowlist would also
		// reject the GeoDistance kind. Either way it cannot get through.
		JsonNode body = SearchOpaqueJsonUtil.parse(
				"{\"sort\":[{\"_geo_distance\":{\"loc\":[0,0],\"order\":\"asc\"}}]}");
		assertThrows(RuntimeException.class,
				// call under test
				() -> SearchOpaqueJsonUtil.parseSort(body, nameOnly(Function.identity())));
	}

	// ===================== parseRescore =====================

	@Test
	public void testParseRescoreWithRescoreQueryRewritesInner() {
		JsonNode node = SearchOpaqueJsonUtil.parse(
				"{\"window_size\":50,\"query\":{\"rescore_query\":{\"match_all\":{}}}}");
		// call under test
		Rescore rescore = SearchOpaqueJsonUtil.parseRescore(node, nameOnly(Function.identity()));
		assertNotNull(rescore);
		assertNotNull(rescore.query().rescoreQuery());
	}

	@Test
	public void testParseRescoreWithoutRescoreQueryRejected() {
		// The missing-node guard skips the inner rewrite; the typed Rescore deserializer then
		// rejects the absent rescore_query, raising MissingRequiredPropertyException (a
		// RuntimeException) that parseRescore propagates unwrapped.
		JsonNode node = SearchOpaqueJsonUtil.parse("{\"window_size\":50,\"query\":{}}");
		assertThrows(RuntimeException.class,
				// call under test
				() -> SearchOpaqueJsonUtil.parseRescore(node, nameOnly(Function.identity())));
	}

	// ===================== applyBodyToRequest =====================

	private static final int APPLY_DEFAULT_SIZE = 10;
	private static final int APPLY_MAX_SIZE = 1000;

	private static SearchRequest applyBody(String json,
			Set<SearchQueryPart> options) {
		SearchRequest.Builder req =
				new SearchRequest.Builder().index("test-index");
		SearchOpaqueJsonUtil.applyBodyToRequest(json, nameOnly(Function.identity()), req, options,
				APPLY_DEFAULT_SIZE, APPLY_MAX_SIZE, Collections.emptyList());
		return req.build();
	}

	private static SearchRequest applyAutocompleteBody(String json,
			Set<SearchQueryPart> options) {
		SearchRequest.Builder req =
				new SearchRequest.Builder().index("test-index");
		SearchOpaqueJsonUtil.applyAutocompleteBodyToRequest(json, nameOnly(Function.identity()), req,
				options, APPLY_DEFAULT_SIZE, Collections.emptyList());
		return req.build();
	}

	/** The complete set of top-level keys the {@code SearchQuery} schema accepts on a search body. */
	private static final Set<String> SEARCH_QUERY_TOP_LEVEL_KEYS = Set.of(
			"query", "post_filter", "aggregations", "highlight", "collapse", "rescore",
			"sort", "_source", "from", "size", "search_after");

	/**
	 * Single round trip exercising every top-level key the {@code SearchQuery} schema accepts
	 * (except {@code search_after}, which forces {@code from} to 0 and is covered separately) in
	 * one body.
	 *
	 * <p>Coverage guard: any key the fixture forgets to populate causes the assertion to fail, so a
	 * future schema change that adds a new top-level key surfaces here until the fixture is
	 * updated.</p>
	 */
	@Test
	public void testApplyBodyToRequestWithEveryTopLevelKeyRoundTrips() {
		String json = "{"
				+ "\"query\":{\"match_all\":{}},"
				+ "\"post_filter\":{\"match_all\":{}},"
				+ "\"aggregations\":{\"by_year\":{\"terms\":{\"field\":\"year\"}}},"
				+ "\"highlight\":{\"fields\":{\"title\":{}}},"
				+ "\"collapse\":{\"field\":\"projectId\"},"
				+ "\"rescore\":{\"window_size\":50,\"query\":{\"rescore_query\":{\"match_all\":{}}}},"
				+ "\"sort\":[\"title\"],"
				+ "\"_source\":[\"title\"],"
				+ "\"from\":5,"
				+ "\"size\":50"
				+ "}";

		// call under test
		SearchRequest req = applyBody(json,
				EnumSet.allOf(SearchQueryPart.class));

		assertNotNull(req.query());
		assertNotNull(req.postFilter());
		assertEquals(1, req.aggregations().size());
		assertNotNull(req.highlight());
		assertNotNull(req.collapse());
		assertEquals(1, req.rescore().size());
		assertEquals(1, req.sort().size());
		assertNotNull(req.source());
		assertEquals(5, req.from().intValue());
		assertEquals(50, req.size().intValue());

		// Coverage guard: every top-level key the SearchQuery schema accepts must be reachable across
		// this suite. This body covers everything except search_after, which forces from to 0
		// (covered in testApplyBodyToRequestWithSearchAfterCursor). Adding a new top-level key fails
		// this assertion until a test for it is added.
		Set<String> exercised = new LinkedHashSet<>();
		JsonNode parsed = SearchOpaqueJsonUtil.parse(json);
		Iterator<String> names = parsed.fieldNames();
		while (names.hasNext()) {
			exercised.add(names.next());
		}
		exercised.add("search_after");
		assertEquals(SEARCH_QUERY_TOP_LEVEL_KEYS, exercised,
				"every SearchQuery top-level key must be exercised across the round-trip suite");
	}

	@Test
	public void testApplyBodyToRequestWithSearchAfterCursor() {
		// search_after replaces from — verify the cursor is propagated and from is forced to 0.
		String json = "{\"query\":{\"match_all\":{}},\"search_after\":[\"abc\",100]}";

		SearchRequest req = applyBody(json,
				EnumSet.of(SearchQueryPart.HITS));

		assertEquals(2, req.searchAfter().size());
		assertEquals(0, req.from().intValue());
	}

	@Test
	public void testApplyBodyToRequestWithSearchAfterAndPositiveFrom() {
		// search_after wins over from: the caller's from > 0 is ignored and from is forced to 0.
		String json = "{\"query\":{\"match_all\":{}},\"search_after\":[\"abc\"],\"from\":5}";

		// call under test
		SearchRequest req = applyBody(json,
				EnumSet.of(SearchQueryPart.HITS));

		assertEquals(1, req.searchAfter().size());
		assertEquals(0, req.from().intValue());
	}

	@Test
	public void testApplyBodyToRequestWithoutHitsForcesSizeZeroAndSkipsHitsOnlySlots() {
		// HITS absent: applyBodyToRequest must force size=0 and skip _source / sort /
		// search_after / highlight / collapse / rescore (every "meaningless without hits"
		// slot). post_filter / aggregations still run.
		String json = "{"
				+ "\"query\":{\"match_all\":{}},"
				+ "\"post_filter\":{\"match_all\":{}},"
				+ "\"aggregations\":{\"a\":{\"terms\":{\"field\":\"year\"}}},"
				+ "\"highlight\":{\"fields\":{\"title\":{}}},"
				+ "\"collapse\":{\"field\":\"projectId\"},"
				+ "\"rescore\":{\"window_size\":50,\"query\":{\"rescore_query\":{\"match_all\":{}}}},"
				+ "\"sort\":[\"title\"],"
				+ "\"_source\":[\"title\"],"
				+ "\"size\":50,"
				+ "\"search_after\":[\"abc\"]"
				+ "}";

		// call under test — TOTAL_HITS only, no HITS
		SearchRequest req = applyBody(json,
				EnumSet.of(SearchQueryPart.TOTAL_HITS));

		assertEquals(0, req.size().intValue(), "size forced to 0 when HITS is absent");
		assertNull(req.highlight(), "highlight skipped without HITS");
		assertNull(req.collapse(), "collapse skipped without HITS");
		assertTrue(req.rescore() == null || req.rescore().isEmpty(), "rescore skipped without HITS");
		assertNull(req.source(), "_source skipped without HITS");
		assertTrue(req.sort() == null || req.sort().isEmpty(), "sort skipped without HITS");
		assertTrue(req.searchAfter() == null || req.searchAfter().isEmpty(),
				"search_after skipped without HITS");
		// post_filter / aggregations still run because they are not gated by HITS.
		assertNotNull(req.postFilter());
		assertEquals(1, req.aggregations().size());
	}

	@Test
	public void testApplyBodyToRequestTrackTotalHitsCountWhenTotalHitsRequested() {
		String json = "{\"query\":{\"match_all\":{}}}";

		SearchRequest req = applyBody(json,
				EnumSet.of(SearchQueryPart.HITS,
						SearchQueryPart.TOTAL_HITS));

		TrackHits track = req.trackTotalHits();
		assertNotNull(track);
		assertTrue(track.isCount(), "TOTAL_HITS requested → track_total_hits.count=Integer.MAX_VALUE");
		assertEquals(Integer.MAX_VALUE, track.count().intValue());
	}

	@Test
	public void testApplyBodyToRequestTrackTotalHitsDisabledWhenTotalHitsAbsent() {
		String json = "{\"query\":{\"match_all\":{}}}";

		SearchRequest req = applyBody(json,
				EnumSet.of(SearchQueryPart.HITS));

		TrackHits track = req.trackTotalHits();
		assertNotNull(track);
		assertTrue(track.isEnabled(), "TOTAL_HITS absent → track_total_hits.enabled=false");
		assertEquals(Boolean.FALSE, track.enabled());
	}


	@Test
	public void testApplyBodyToRequestDefaultsAndClampsSize() {
		// Body omits from — defaults to 0; size above maxSize must clamp.
		String json = "{\"query\":{\"match_all\":{}},\"size\":10000}";

		SearchRequest req = applyBody(json,
				EnumSet.of(SearchQueryPart.HITS));

		assertEquals(0, req.from().intValue(), "from defaults to 0");
		assertEquals(APPLY_MAX_SIZE, req.size().intValue(), "size clamps at maxSize");
	}

	@Test
	public void testApplyBodyToRequestDefaultsSizeWhenOmitted() {
		String json = "{\"query\":{\"match_all\":{}}}";

		SearchRequest req = applyBody(json,
				EnumSet.of(SearchQueryPart.HITS));

		assertEquals(APPLY_DEFAULT_SIZE, req.size().intValue(),
				"size omitted → defaultSize");
	}

	@Test
	public void testApplyAutocompleteBodyToRequestWithQueryAndSourceOnlyAccepted() {
		String json = "{\"query\":{\"prefix\":{\"title\":\"alz\"}},\"_source\":[\"title\"]}";

		// call under test
		SearchRequest req = applyAutocompleteBody(json,
				EnumSet.of(SearchQueryPart.HITS));

		assertNotNull(req.query());
		assertNotNull(req.source());
		// Autocomplete narrows the surface — these slots must NOT be populated.
		assertNull(req.postFilter(), "autocomplete must not surface post_filter");
		assertTrue(req.aggregations() == null || req.aggregations().isEmpty(),
				"autocomplete must not surface aggregations");
		assertNull(req.highlight(), "autocomplete must not surface highlight");
		assertNull(req.collapse(), "autocomplete must not surface collapse");
		assertTrue(req.rescore() == null || req.rescore().isEmpty(),
				"autocomplete must not surface rescore");
	}

	@Test
	public void testApplyAutocompleteBodyToRequestRejectsDisallowedTopLevelQueryKind() {
		// match_all is in the general allowlist but not in ALLOWED_AUTOCOMPLETE_TOP_LEVEL.
		String json = "{\"query\":{\"match_all\":{}}}";

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> applyAutocompleteBody(json,
						EnumSet.of(SearchQueryPart.HITS)));
		assertTrue(ex.getMessage().contains("autocomplete"));
	}

	@Test
	public void testApplyAutocompleteBodyToRequestForcesSizeZeroWithoutHits() {
		String json = "{\"query\":{\"prefix\":{\"title\":\"alz\"}}}";

		SearchRequest req = applyAutocompleteBody(json,
				EnumSet.noneOf(SearchQueryPart.class));

		assertEquals(0, req.size().intValue(), "size forced to 0 without HITS");
	}

	@Test
	public void testApplyBodyToRequestWithNegativeFromRejected() {
		// Smoke test that applyBodyToRequest wires resolveFrom in; the per-branch boundary
		// cases are covered directly in SearchDslValidatorTest's resolveFrom / resolveSize tests.
		String json = "{\"query\":{\"match_all\":{}},\"from\":-1}";

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> applyBody(json,
						EnumSet.of(SearchQueryPart.HITS)));
		assertTrue(ex.getMessage().contains("from"));
	}

	@Test
	public void testApplyBodyToRequestWithNullFromAndSizeUsesDefaults() {
		// Explicit null on from/size goes through the !node.isNull() branch as a no-op.
		String json = "{\"query\":{\"match_all\":{}},\"from\":null,\"size\":null}";

		SearchRequest req = applyBody(json,
				EnumSet.of(SearchQueryPart.HITS));

		assertEquals(0, req.from().intValue(), "null from → 0");
		assertEquals(APPLY_DEFAULT_SIZE, req.size().intValue(), "null size → defaultSize");
	}

	// ===================== JSONObjectAdapter inline analyzer =====================

	@Test
	public void testToInlineAnalyzerSettingsWithJSONObjectAdapter() throws Exception {
		// JSONObjectAdapter is the wire-deserialized shape that controllers receive after JSON
		// binding for opaque-Object slots; verify the analyzer-settings path accepts it the
		// same way it accepts a Map / JSONObject.
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(
				"{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}");

		// call under test
		IndexSettingsAnalysis settings = SearchOpaqueJsonUtil.toInlineAnalyzerSettings(adapter, qname -> null);

		assertNotNull(settings);
		assertNotNull(settings.analyzer().get("default"));
	}

	@Test
	public void testCollectRefsDeduplicatesAcrossNestedObjects() {
		// Same qname appearing under multiple branches must be returned once, in walk order.
		JsonNode root = SearchOpaqueJsonUtil.parse(
				"{\"a\":{\"$ref\":\"org.sagebionetworks-X\"},"
						+ "\"b\":{\"c\":{\"$ref\":\"org.sagebionetworks-X\"}},"
						+ "\"d\":{\"$ref\":\"org.sagebionetworks-Y\"}}");

		// call under test
		Set<String> refs = SearchOpaqueJsonUtil.collectRefs(root);

		assertEquals(2, refs.size(), "duplicates across branches collapse");
		assertTrue(refs.contains("org.sagebionetworks-X"));
		assertTrue(refs.contains("org.sagebionetworks-Y"));
	}
}
