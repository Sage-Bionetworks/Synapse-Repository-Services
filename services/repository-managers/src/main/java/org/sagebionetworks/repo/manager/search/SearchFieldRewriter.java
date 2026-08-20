package org.sagebionetworks.repo.manager.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Column-name &harr; column-id rewriter for the opaque OpenSearch DSL JSON envelopes Synapse
 * carries on its search APIs. Operates on a Jackson tree in both directions:
 * <ul>
 *   <li>Request side: {@link #rewriteRequestFields(JsonNode, RoutingContext, Surface)} mutates
 *       an inbound {@code query} / {@code aggregations} tree, rewriting
 *       caller column names to column ids and routing text-typed columns through their
 *       {@code .keyword} sub-field on operations that need it (term-family, range,
 *       aggregations, sort-equivalent clauses).</li>
 *   <li>Response side: {@link #rewriteAggregationResults} walks the AOSS aggregations response
 *       envelope, rewrites embedded column ids back to column names, and strips the
 *       {@code .keyword} suffix so the caller sees their original bare column name even when the
 *       server auto-routed.</li>
 * </ul>
 *
 * <p>Both directions assume {@code "field"} string values and {@code "fields"} string-array
 * values are column references. The OpenSearch DSL areas where {@code "field"} is not a
 * column reference (script bodies, runtime mappings, geo-shape {@code indexed_shape}, ...)
 * are rejected upstream by {@link SearchDslValidator}'s forbidden-key scan, so the rewriter
 * never sees them.</p>
 *
 * <p><b>Auto-routing.</b> Text-typed columns (STRING, STRING_LIST, MEDIUMTEXT, LARGETEXT,
 * LINK) are dual-mapped in the index as a tokenized text field under the bare column name
 * and a raw keyword field under {@code {column}.keyword}. Operations that require doc values
 * (aggregations, sort) or exact match against the original value (term / terms / prefix /
 * wildcard / fuzzy / range) need the {@code .keyword} sub-field; the relevance-scored
 * match-family clauses &mdash; including {@code match_phrase_prefix}, which analyzes its
 * input like {@code match_phrase} &mdash; use the bare tokenized field. The routing is
 * decided per clause kind via the static maps below and applied in
 * {@link #rewriteFieldRef(String, RoutingContext, RoutingMode)}. Callers who supply
 * {@code .keyword} explicitly are unaffected &mdash; the suffix is detected and preserved.</p>
 */
final class SearchFieldRewriter {

	/** Caller's clause kind for routing. {@code KEYWORD_FOR_TEXT} forces {@code .keyword} on
	 * text/link columns; {@code BARE} leaves the reference alone. */
	enum RoutingMode {
		BARE,
		KEYWORD_FOR_TEXT
	}

	/** Which top-level surface the walker is operating on; selects the per-surface kind map. */
	enum Surface {
		QUERY,
		AGGREGATIONS,
		HIGHLIGHT,
		COLLAPSE
	}

	// ---------- sort and _source surfaces (top-level body keys) ----------

	/**
	 * Rewrite a {@code sort} subtree in place: caller column names &rarr; column ids, with
	 * {@code .keyword} auto-routing for text-typed columns (sort needs doc values).
	 *
	 * <p>Accepted OpenSearch sort shapes:</p>
	 * <ul>
	 *   <li>A string &mdash; the column name (or {@code _score}).</li>
	 *   <li>An object whose only key is the column name, value is {@code "asc"} / {@code "desc"}
	 *       or a sort options object.</li>
	 *   <li>An array of any of the above.</li>
	 * </ul>
	 *
	 * <p>The pseudo-column {@code _score} passes through unchanged. Unknown column names pass
	 * through unchanged so AOSS surfaces the typo.</p>
	 */
	static void rewriteSortFields(JsonNode node, RoutingContext ctx) {
		if (node == null) {
			return;
		}
		if (node.isArray()) {
			ArrayNode array = (ArrayNode) node;
			for (int i = 0; i < array.size(); i++) {
				JsonNode element = array.get(i);
				if (element.isTextual()) {
					String original = element.asText();
					if (!"_score".equals(original)) {
						array.set(i, new TextNode(rewriteFieldRef(original, ctx, RoutingMode.KEYWORD_FOR_TEXT)));
					}
				} else if (element.isObject()) {
					rewriteSortObjectKeys((ObjectNode) element, ctx);
				}
			}
		} else if (node.isObject()) {
			rewriteSortObjectKeys((ObjectNode) node, ctx);
		}
	}

	static void rewriteSortObjectKeys(ObjectNode sortObject, RoutingContext ctx) {
		List<String> originalKeys = new ArrayList<>();
		Iterator<String> names = sortObject.fieldNames();
		while (names.hasNext()) {
			originalKeys.add(names.next());
		}
		for (String key : originalKeys) {
			if ("_score".equals(key)) {
				continue;
			}
			String rewritten = rewriteFieldRef(key, ctx, RoutingMode.KEYWORD_FOR_TEXT);
			if (!key.equals(rewritten)) {
				JsonNode value = sortObject.get(key);
				sortObject.remove(key);
				sortObject.set(rewritten, value);
			}
		}
	}

	/**
	 * Rewrite a {@code _source} subtree in place: caller column names &rarr; column ids on
	 * the {@code includes} / {@code excludes} arrays (or the top-level array shorthand).
	 * Boolean-shape source filters and unknown names pass through unchanged.
	 *
	 * <p>Routing is {@link RoutingMode#BARE} &mdash; {@code _source} reads the document body's
	 * stored fields, not the doc-values keyword side.</p>
	 */
	static void rewriteSourceFields(JsonNode node, RoutingContext ctx) {
		if (node == null) {
			return;
		}
		if (node.isArray()) {
			rewriteSourceArrayInPlace((ArrayNode) node, ctx);
		} else if (node.isObject()) {
			ObjectNode obj = (ObjectNode) node;
			JsonNode includes = obj.get("includes");
			if (includes != null && includes.isArray()) {
				rewriteSourceArrayInPlace((ArrayNode) includes, ctx);
			}
			JsonNode excludes = obj.get("excludes");
			if (excludes != null && excludes.isArray()) {
				rewriteSourceArrayInPlace((ArrayNode) excludes, ctx);
			}
		}
	}

	static void rewriteSourceArrayInPlace(ArrayNode array, RoutingContext ctx) {
		for (int i = 0; i < array.size(); i++) {
			JsonNode element = array.get(i);
			if (element.isTextual()) {
				array.set(i, new TextNode(rewriteFieldRef(element.asText(), ctx, RoutingMode.BARE)));
			}
		}
	}

	/**
	 * Provides name &rarr; id mapping and the column-type-derived "is this a text-like column"
	 * predicate used by the auto-router. The id passed to {@link #isTextLike(String)} is the
	 * mapped id, not the caller's name.
	 */
	interface RoutingContext {
		/** Map a caller-supplied column name to its column id. Return the input unchanged when
		 * the name is not in the schema (so AOSS errors surface the typo). */
		String mapName(String name);

		/** True iff the column with this id is text-typed (STRING / STRING_LIST / MEDIUMTEXT /
		 * LARGETEXT / LINK) — the dual-mapped category that has a {@code .keyword} sub-field. */
		boolean isTextLike(String columnId);
	}

	// OpenSearch leaf-query kinds that accept a "shorthand" form where the inner object's
	// single key is the field name itself rather than an explicit "field" property.
	// Example: {"match": {"title": "amyloid"}} ↔ {"match": {"field": "title", "query": "amyloid"}}.
	// The typed deserializer canonicalizes the two forms; on the JsonNode tree we must
	// recognize the shorthand and rewrite the key. multi_match and simple_query_string
	// have no shorthand — they always carry an explicit "fields" array.
	private static final Set<String> SHORTHAND_FIELD_KEYED_KINDS = new HashSet<>(Arrays.asList(
			"match", "match_phrase", "match_phrase_prefix", "match_bool_prefix",
			"term", "terms", "range", "prefix", "wildcard", "fuzzy"));

	// Per-surface routing tables: key is the OpenSearch clause/agg kind name;
	// value is the routing mode that applies to the immediate `field` / `fields` /
	// shorthand-key reference inside that body. Keys not in a table default to BARE.

	private static final Map<String, RoutingMode> QUERY_KIND_MODES = Map.ofEntries(
			// match-family — bare tokenized text field
			Map.entry("match", RoutingMode.BARE),
			Map.entry("match_phrase", RoutingMode.BARE),
			Map.entry("match_phrase_prefix", RoutingMode.BARE),
			Map.entry("match_bool_prefix", RoutingMode.BARE),
			Map.entry("multi_match", RoutingMode.BARE),
			Map.entry("simple_query_string", RoutingMode.BARE),
			Map.entry("exists", RoutingMode.BARE),
			// term-family + range / prefix / wildcard / fuzzy —
			// need the raw keyword sub-field on text columns.
			Map.entry("term", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("terms", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("range", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("prefix", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("wildcard", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("fuzzy", RoutingMode.KEYWORD_FOR_TEXT));

	// Aggregations all need doc values; AOSS rejects bare-text aggregations outright with
	// "Text fields are not optimised for operations that require per-document field data".
	private static final Map<String, RoutingMode> AGG_KIND_MODES = Map.ofEntries(
			Map.entry("terms", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("histogram", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("date_histogram", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("range", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("date_range", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("missing", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("min", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("max", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("avg", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("sum", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("stats", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("extended_stats", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("value_count", RoutingMode.KEYWORD_FOR_TEXT),
			Map.entry("cardinality", RoutingMode.KEYWORD_FOR_TEXT));

	private SearchFieldRewriter() {
	}

	// HIGHLIGHT and COLLAPSE have no nested clause-kind names that drive routing —
	// HIGHLIGHT routes via its `fields` map keys (handled inline in walk()), and COLLAPSE
	// routes its single top-level `field` via the seed mode passed to the walker.
	static Map<String, RoutingMode> kindMapFor(Surface surface) {
		switch (surface) {
		case QUERY: return QUERY_KIND_MODES;
		case AGGREGATIONS: return AGG_KIND_MODES;
		case HIGHLIGHT:
		case COLLAPSE: return Collections.emptyMap();
		default: throw new IllegalStateException("unknown surface: " + surface);
		}
	}

	// ---------- Request-side rewrite (column name → column id, with auto-routing) ----------

	/**
	 * Walks the inbound DSL tree and rewrites every column-name reference to its column id
	 * via {@code ctx}, applying clause-kind-specific {@code .keyword} routing for text-typed
	 * columns. Mutates {@code node} in place.
	 *
	 * <p>Three reference shapes are recognized:</p>
	 * <ul>
	 *   <li>A {@code "field"} string property (long-form leaf queries, aggregations,
	 *       exists).</li>
	 *   <li>A {@code "fields"} string-array property (multi_match, simple_query_string).</li>
	 *   <li>The single key of the inner object of a shorthand leaf query
	 *       (e.g. {@code {"match": {"<column>": "value"}}}).</li>
	 * </ul>
	 *
	 * <p>The routing mode is set when the walker descends into a child whose key matches an
	 * allowlisted clause kind on {@code surface}; otherwise it resets to {@link RoutingMode#BARE}.
	 * The mode applies only to the immediate {@code field} / {@code fields} / shorthand-key
	 * reference inside that clause body.</p>
	 */
	static void rewriteRequestFields(JsonNode node, RoutingContext ctx, Surface surface) {
		// Collapse has no enclosing clause kind to drive routing — the top-level "field" key
		// is the reference. Seed the walker with KEYWORD_FOR_TEXT so the auto-router routes
		// text columns through .keyword (collapse needs doc values, like aggregations).
		RoutingMode initialMode = (surface == Surface.COLLAPSE)
				? RoutingMode.KEYWORD_FOR_TEXT : RoutingMode.BARE;
		walk(node, ctx, surface, initialMode);
	}

	static void walk(JsonNode node, RoutingContext ctx, Surface surface, RoutingMode mode) {
		if (node == null) {
			return;
		}
		if (node.isObject()) {
			ObjectNode obj = (ObjectNode) node;
			Map<String, RoutingMode> kindMap = kindMapFor(surface);
			// Snapshot the entries before mutating, since rewriteShorthandKey replaces an
			// entry in-place and the live iterator can't see the swap.
			List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
			Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
			while (it.hasNext()) {
				entries.add(it.next());
			}
			for (Map.Entry<String, JsonNode> entry : entries) {
				String key = entry.getKey();
				JsonNode value = entry.getValue();
				if ("field".equals(key) && value.isTextual()) {
					obj.set("field", new TextNode(rewriteFieldRef(value.asText(), ctx, mode)));
				} else if ("fields".equals(key) && value.isArray()) {
					ArrayNode array = (ArrayNode) value;
					for (int i = 0; i < array.size(); i++) {
						JsonNode element = array.get(i);
						if (element.isTextual()) {
							array.set(i, new TextNode(rewriteFieldRef(element.asText(), ctx, mode)));
						}
					}
				} else if (surface == Surface.HIGHLIGHT && "fields".equals(key) && value.isObject()) {
					// highlight.fields is { columnName: { ...field options... } } — rewrite each
					// key as a column-name reference, then recurse into the inner option block so
					// any nested highlight_query is reachable.
					rewriteHighlightFieldsMap((ObjectNode) value, ctx);
					walk(value, ctx, surface, RoutingMode.BARE);
				} else if (surface == Surface.HIGHLIGHT && "highlight_query".equals(key) && value.isObject()) {
					// A highlight_query body is a full Query subtree — switch surfaces so the
					// query allowlist + auto-routing applies inside.
					walk(value, ctx, Surface.QUERY, RoutingMode.BARE);
				} else if (surface == Surface.AGGREGATIONS && "filter".equals(key) && value.isObject()) {
					walk(value, ctx, Surface.QUERY, RoutingMode.BARE);
				} else if (surface == Surface.AGGREGATIONS && "filters".equals(key) && value.isObject()) {
					JsonNode inner = value.get("filters");
					if (inner != null) {
						for (JsonNode query : inner) {
							walk(query, ctx, Surface.QUERY, RoutingMode.BARE);
						}
					}
				} else {
					RoutingMode childMode = kindMap.getOrDefault(key, RoutingMode.BARE);
					if (SHORTHAND_FIELD_KEYED_KINDS.contains(key) && value.isObject()) {
						rewriteShorthandKey((ObjectNode) value, ctx, childMode);
					}
					walk(value, ctx, surface, childMode);
				}
			}
		} else if (node.isArray()) {
			for (JsonNode element : node) {
				walk(element, ctx, surface, mode);
			}
		}
	}

	/**
	 * Rewrite each key of a highlight {@code fields} map (a column-name reference) to its
	 * column-id form. Highlighted fields are bound to the analyzer at index time, so the
	 * reference goes to the bare tokenized field — no {@code .keyword} routing.
	 */
	static void rewriteHighlightFieldsMap(ObjectNode highlightFields, RoutingContext ctx) {
		List<String> originalKeys = new ArrayList<>();
		Iterator<String> names = highlightFields.fieldNames();
		while (names.hasNext()) {
			originalKeys.add(names.next());
		}
		for (String key : originalKeys) {
			String rewritten = rewriteFieldRef(key, ctx, RoutingMode.BARE);
			if (!key.equals(rewritten)) {
				JsonNode value = highlightFields.get(key);
				highlightFields.remove(key);
				highlightFields.set(rewritten, value);
			}
		}
	}

	/**
	 * Detect and rewrite a shorthand leaf-query key. If {@code shorthandObject} has exactly
	 * one entry and its key is not the literal {@code "field"} (which would mark long-form),
	 * the key is the field name &mdash; replace it via {@link #rewriteFieldRef}. Long-form
	 * objects (multiple keys, or a single {@code "field"} key) are left alone for the leaf
	 * rule.
	 */
	static void rewriteShorthandKey(ObjectNode shorthandObject, RoutingContext ctx, RoutingMode mode) {
		if (shorthandObject.size() != 1) {
			return;
		}
		String key = shorthandObject.fieldNames().next();
		if ("field".equals(key)) {
			return;
		}
		String rewritten = rewriteFieldRef(key, ctx, mode);
		if (!key.equals(rewritten)) {
			JsonNode value = shorthandObject.get(key);
			shorthandObject.remove(key);
			shorthandObject.set(rewritten, value);
		}
	}

	/**
	 * Rewrite a single field-reference string (column name plus optional {@code .keyword}
	 * sub-field selector, and/or a {@code ^boost} multi_match boost) to the column-id form.
	 * Preserves any explicit {@code .keyword} the caller supplied. When {@code mode} is
	 * {@link RoutingMode#KEYWORD_FOR_TEXT} and the resolved column is text-like, appends
	 * {@code .keyword} on behalf of the caller.
	 *
	 * <p>If the bare-name segment isn't in the schema, the input is returned unchanged
	 * &mdash; unknown references go to AOSS as-is so the error message surfaces the typo.</p>
	 */
	static String rewriteFieldRef(String raw, RoutingContext ctx, RoutingMode mode) {
		if (raw == null) {
			return null;
		}
		// Split off ^boost first so the dot-handling below sees only "namepart[.suffix]".
		int caret = raw.indexOf('^');
		String head = caret >= 0 ? raw.substring(0, caret) : raw;
		String boost = caret >= 0 ? raw.substring(caret) : "";

		// Split off the .keyword sub-field selector (the only one the index emits). A column
		// name itself may legally contain dots, so the split is the last dot rather than the
		// first — and we only recognize the literal "keyword" suffix.
		int dot = head.lastIndexOf('.');
		String namePart = head;
		String subField = "";
		if (dot > 0 && "keyword".equals(head.substring(dot + 1))) {
			namePart = head.substring(0, dot);
			subField = ".keyword";
		}

		String mapped = ctx.mapName(namePart);
		boolean nameResolved = mapped != null && !mapped.equals(namePart);

		// Auto-route: append .keyword on text-like columns when the clause requires it and
		// the caller didn't already supply a sub-field. Only fires when we were able to
		// resolve the name to an id (otherwise we don't know the column type).
		if (subField.isEmpty()
				&& mode == RoutingMode.KEYWORD_FOR_TEXT
				&& nameResolved
				&& ctx.isTextLike(mapped)) {
			subField = ".keyword";
		}

		if (mapped == null || mapped.equals(namePart)) {
			// Unknown name and no auto-routing — preserve the caller's exact spelling.
			return subField.isEmpty() ? raw : namePart + subField + boost;
		}
		return mapped + subField + boost;
	}

	// ---------- Response-side rewrite (column id → column name, strip .keyword) ----------

	/**
	 * Walks the AOSS response's aggregation block and rewrites any embedded column-id
	 * {@code "field"} reference back to its column name, stripping a {@code .keyword}
	 * suffix the auto-router may have appended. The caller's aggregation-name keys are
	 * unchanged (those are caller-chosen labels, not field references).
	 */
	static void rewriteAggregationResults(JsonNode node, Function<String, String> idToName) {
		if (node == null) {
			return;
		}
		if (node.isObject()) {
			ObjectNode obj = (ObjectNode) node;
			Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				if ("field".equals(entry.getKey()) && entry.getValue().isTextual()) {
					String mapped = rewriteIdRefStrippingKeyword(entry.getValue().asText(), idToName);
					if (mapped != null) {
						obj.set("field", new TextNode(mapped));
					}
				} else {
					rewriteAggregationResults(entry.getValue(), idToName);
				}
			}
		} else if (node.isArray()) {
			for (JsonNode element : node) {
				rewriteAggregationResults(element, idToName);
			}
		}
	}

	/**
	 * Inverse of the request-side leaf rewrite: parse {@code {id}[.keyword][^boost]} into its
	 * parts, map id back to the caller's column name, and emit just {@code {name}[^boost]}.
	 * Drops the {@code .keyword} so the caller sees their original bare column name even when
	 * the server auto-routed during the request.
	 *
	 * <p>Returns the input unchanged when the bare id segment is not in {@code idToName}, so
	 * non-column ids (the literal {@code _score}, {@code _id}, etc., or values produced by
	 * AOSS that don't correspond to any column) pass through.</p>
	 */
	static String rewriteIdRefStrippingKeyword(String raw, Function<String, String> idToName) {
		if (raw == null) {
			return null;
		}
		int caret = raw.indexOf('^');
		String head = caret >= 0 ? raw.substring(0, caret) : raw;
		String boost = caret >= 0 ? raw.substring(caret) : "";
		int dot = head.lastIndexOf('.');
		String idPart = head;
		if (dot > 0 && "keyword".equals(head.substring(dot + 1))) {
			idPart = head.substring(0, dot);
		}
		String mapped = idToName.apply(idPart);
		if (mapped == null) {
			return raw;
		}
		return mapped + boost;
	}
}
