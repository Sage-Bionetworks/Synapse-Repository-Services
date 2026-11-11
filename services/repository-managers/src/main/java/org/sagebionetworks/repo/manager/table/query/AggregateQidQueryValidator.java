package org.sagebionetworks.repo.manager.table.query;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.SQLTranslatorUtils;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.cluster.resolution.ColumnSource;
import org.sagebionetworks.table.cluster.resolution.ColumnSourceResolver;
import org.sagebionetworks.table.cluster.resolution.ResolvedColumn;
import org.sagebionetworks.table.query.model.CaseExpression;
import org.sagebionetworks.table.query.model.CastSpecification;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DefiningClause;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.FactorPrime;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.SortKey;
import org.sagebionetworks.table.query.model.SortSpecification;
import org.sagebionetworks.table.query.model.TermPrime;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Enforces the restrictions that an aggregate-only source places on a query that requests row
 * results:
 * <ul>
 * <li>A quasi-identifier (QID) column may appear only as the argument of a {@code COUNT} (or
 * {@code COUNT(DISTINCT ...)}) or in a {@code WHERE} or {@code DEFINING_WHERE} clause. Any other use
 * exposes the QID as more than a count.</li>
 * <li>Every {@code COUNT} in the select list, whatever it counts, is a protected count subject to
 * cell-level k-anonymity, so it must be the whole value of its column and must not be used to order
 * the rows, whether directly or by its alias. No sort may name a select-list position.</li>
 * <li>No other aggregate whose value depends on the number of rows in a group (e.g. {@code SUM},
 * {@code GROUP_CONCAT}) may be used, since it would reveal a count without suppression. Only
 * {@code MIN}, {@code MAX}, and {@code AVG} of a non-QID column are permitted.</li>
 * <li>Every {@code COUNT} in the select list of a VirtualTable's inlined defining SQL is also a
 * protected count, held to the same rules, and suppressed where it is computed whenever the query
 * reads the column it produces (PLFM-10108).</li>
 * <li>A column whose value is an aggregate of a QID (e.g. a count computed by a MaterializedView or
 * VirtualTable) may not be used to filter the rows.</li>
 * </ul>
 * A violation causes the query's row results to be withheld via a {@link RowSuppressionException}.
 */
public class AggregateQidQueryValidator {

	/**
	 * The aggregates, other than {@code COUNT}, whose value does not depend on the number of rows in
	 * a group, so they cannot be used to reconstruct a suppressed count.
	 */
	static final Set<SetFunctionType> CARDINALITY_INDEPENDENT_AGGREGATES = EnumSet.of(SetFunctionType.MIN,
			SetFunctionType.MAX, SetFunctionType.AVG);

	/**
	 * Validates that every QID column is used only in a count-only manner and that every
	 * {@code COUNT} can be suppressed, and identifies the output columns whose values are counts
	 * protected by cell-level k-anonymity.
	 *
	 * @param model          the query as it executes, including any request-supplied sort,
	 *                       filters, and facet selections.
	 * @param qidColumnIds   the ids of the queried object's own columns that carry a
	 *                       quasi-identifier. May be empty, in which case only the count
	 *                       restrictions apply.
	 * @param schemaProvider resolves the queried object's as-built schema, so each column
	 *                       reference in the query is bound to the same column the query
	 *                       translation will bind it to.
	 * @return the select columns whose value is a {@code COUNT}, in select-list order. These are
	 *         the cells subject to cell-level k-anonymity suppression.
	 * @throws RowSuppressionException if a QID or a {@code COUNT} is used in a way that is not
	 *                                 permitted for a query requesting row results. A QID
	 *                                 violation takes precedence over a {@code COUNT} violation.
	 */
	public static List<DerivedColumn> validate(QuerySpecification model, Set<String> qidColumnIds,
			SchemaProvider schemaProvider) {
		ValidateArgument.required(model, "model");
		ValidateArgument.required(qidColumnIds, "qidColumnIds");
		ValidateArgument.required(schemaProvider, "schemaProvider");
		return validate(model, qidColumnIds, new TableAndColumnMapper(model, schemaProvider));
	}

	private static List<DerivedColumn> validate(QuerySpecification model, Set<String> qidColumnIds,
			TableAndColumnMapper mapper) {
		if (!qidColumnIds.isEmpty()) {
			validateQidUses(model, qidColumnIds, mapper);
		}
		// Every row an aggregate-only source contributes carries its quasi-identifiers, so a count over
		// those rows is a protected count whatever column it counts. This holds even when the queried
		// object projects no QID-derived column at all.
		return collectProtectedCounts(model, true);
	}

	/**
	 * Validates the query as {@link #validate(QuerySpecification, Set, SchemaProvider)} does, and also
	 * protects every {@code COUNT} computed by a VirtualTable's defining SQL that the query inlines. Such
	 * a count is suppressed where it is computed, so the column it produces is no longer treated as a
	 * quasi-identifier by the outer query: it may be projected, filtered, and sorted, because each of
	 * those sees only the suppressed value.
	 *
	 * @param model          the query as it executes, including any inlined defining SQL and any
	 *                       request-supplied sort, filters, and facet selections.
	 * @param qidColumns     the queried object's own columns that carry a quasi-identifier. The query
	 *                       may not filter on those whose value is an aggregate of a quasi-identifier.
	 * @param schemaProvider resolves the as-built schema of each object the query reads.
	 * @return the ids of the inlined object's columns whose value is a count computed by its defining SQL
	 *         and read by the outer query. These are suppressed where they are computed. Empty when no
	 *         defining SQL is inlined.
	 * @throws RowSuppressionException if a QID or a {@code COUNT} is used in a way that is not
	 *                                 permitted for a query requesting row results.
	 */
	public static Set<String> validate(QueryExpression model, QidColumns qidColumns, SchemaProvider schemaProvider) {
		ValidateArgument.required(model, "model");
		ValidateArgument.required(qidColumns, "qidColumns");
		ValidateArgument.required(schemaProvider, "schemaProvider");

		ColumnSourceResolver resolver = new ColumnSourceResolver(model, schemaProvider);
		QuerySpecification outer = resolver.getOuterQuery();
		TableAndColumnMapper outerMapper = resolver.getMapper(outer);
		rejectFilterOnAggregateQid(outer, qidColumns.aggregates(), outerMapper);
		if (resolver.getInlinedQuery().isEmpty()) {
			validate(outer, qidColumns.all(), outerMapper);
			return Set.of();
		}

		// The defining SQL was written by the data manager who created the VirtualTable rather than by the
		// caller, so only its counts are held to the count rules; its other aggregates are not rejected.
		collectProtectedCounts(resolver.getInlinedQuery().get(), false);
		Set<String> unsuppressedQidColumnIds = new HashSet<>(qidColumns.all());
		unsuppressedQidColumnIds.removeAll(getDefiningCountColumnIds(resolver, schemaProvider));
		validate(outer, unsuppressedQidColumnIds, outerMapper);
		return getReadDefiningCountColumnIds(resolver, schemaProvider);
	}

	/**
	 * Identifies the counts computed by an inlined VirtualTable's defining SQL that the query reads,
	 * without applying any of the QID or count restrictions to the query itself.
	 *
	 * @param model          the query as it executes, including any inlined defining SQL.
	 * @param schemaProvider resolves the as-built schema of each object the query reads.
	 * @return the ids of the inlined object's columns whose value is a count computed by its defining SQL
	 *         and read anywhere by the outer query. Empty when no defining SQL is inlined.
	 */
	public static Set<String> getReadDefiningCountColumnIds(QueryExpression model, SchemaProvider schemaProvider) {
		ValidateArgument.required(model, "model");
		ValidateArgument.required(schemaProvider, "schemaProvider");
		return getReadDefiningCountColumnIds(new ColumnSourceResolver(model, schemaProvider), schemaProvider);
	}

	/**
	 * Only a count the outer query reads is suppressed, because excluding the rows of a count it never reads
	 * would drop rows the caller is entitled to see. A count is read when the outer query refers to its
	 * column anywhere: in its select list (including through {@code *}), filters, grouping, or sort.
	 */
	private static Set<String> getReadDefiningCountColumnIds(ColumnSourceResolver resolver,
			SchemaProvider schemaProvider) {
		if (resolver.getInlinedQuery().isEmpty()) {
			return Set.of();
		}
		QuerySpecification outer = resolver.getOuterQuery();
		if (Boolean.TRUE.equals(outer.getSelectList().getAsterisk())) {
			return getDefiningCountColumnIds(resolver, schemaProvider);
		}
		Set<String> read = new LinkedHashSet<>();
		for (ColumnReference reference : outer.createIterable(ColumnReference.class)) {
			resolver.resolve(reference)
					.filter(source -> source.getDefinition().map(ResolvedColumn::isWholeCount).orElse(false))
					.flatMap(ColumnSource::getColumnId).ifPresent(read::add);
		}
		return read;
	}

	/**
	 * @return the ids of the inlined object's columns whose value is a count computed by its defining SQL.
	 *         Each select column of the defining SQL is paired with the bound column of the same name.
	 */
	private static Set<String> getDefiningCountColumnIds(ColumnSourceResolver resolver,
			SchemaProvider schemaProvider) {
		IdAndVersion inlinedObjectId = resolver.getInlinedObjectId().orElseThrow();
		Map<String, String> boundIdByName = schemaProvider.getTableSchema(inlinedObjectId).stream()
				.collect(Collectors.toMap(ColumnModel::getName, ColumnModel::getId));
		Set<String> ids = new LinkedHashSet<>();
		for (ResolvedColumn column : resolver.getInlinedColumns()) {
			if (!column.isWholeCount()) {
				continue;
			}
			String id = boundIdByName.get(column.getOutputName());
			if (id == null) {
				throw new IllegalStateException("The schema of " + inlinedObjectId + " has no column named '"
						+ column.getOutputName() + "' produced by its defining SQL");
			}
			ids.add(id);
		}
		return ids;
	}

	/**
	 * Filtering on an aggregate of a quasi-identifier (e.g. {@code where individualCount = 1}) selects
	 * exactly the rows whose count the suppression would hide. Additional filters, selected facets, and
	 * cohort filters are all part of the WHERE clause of the combined query, so this covers each of them.
	 */
	private static void rejectFilterOnAggregateQid(QuerySpecification outer, Set<String> aggregateQidColumnIds,
			TableAndColumnMapper mapper) {
		if (aggregateQidColumnIds.isEmpty()) {
			return;
		}
		for (ColumnReference reference : outer.createIterable(ColumnReference.class)) {
			if (reference.isInContext(WhereClause.class) && isQid(reference, mapper, aggregateQidColumnIds)) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER);
			}
		}
	}

	private static void validateQidUses(QuerySpecification model, Set<String> qidColumnIds,
			TableAndColumnMapper mapper) {
		// 'select *' projects every column, which necessarily exposes each QID.
		if (Boolean.TRUE.equals(model.getSelectList().getAsterisk())) {
			throw new RowSuppressionException(RowSuppressionReasonCode.QID_PROJECTED);
		}

		// A reference is resolved to the column it actually reads, so the restriction holds however
		// the caller spelled it: an alias or a correlation prefix changes the text, not the column.
		for (ColumnReference reference : model.createIterable(ColumnReference.class)) {
			if (isQid(reference, mapper, qidColumnIds)) {
				classifyQidReference(reference, model);
			}
		}
	}

	/**
	 * Classify a single QID reference by its location in the query, throwing when the use is
	 * not permitted.
	 */
	private static void classifyQidReference(ColumnReference reference, QuerySpecification model) {
		// A QID used only to filter never appears in the results.
		if (reference.isInContext(WhereClause.class) || reference.isInContext(DefiningClause.class)) {
			return;
		}
		// The only aggregate that yields a count (and is therefore suppressible) is COUNT;
		// every other aggregate would expose the QID's values. How a COUNT itself may be used is
		// enforced for every COUNT by collectProtectedCounts.
		Optional<SetFunctionSpecification> setFunction = reference.getContext(SetFunctionSpecification.class);
		if (setFunction.isPresent()) {
			if (setFunction.get().getSetFunctionType() != SetFunctionType.COUNT) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_NON_COUNT_AGGREGATE);
			}
			return;
		}
		if (reference.isInContext(GroupByClause.class)) {
			throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_GROUP_BY);
		}
		if (reference.isInContext(OrderByClause.class)) {
			throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_ORDER_BY);
		}
		// Remaining case: the QID is projected in the select list outside of a COUNT.
		if (SetQuantifier.DISTINCT.equals(model.getSetQuantifier())) {
			throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_SELECT_DISTINCT);
		}
		DerivedColumn column = reference.getContext(DerivedColumn.class).orElse(null);
		if (column != null && isExpression(column)) {
			throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_EXPRESSION);
		}
		throw new RowSuppressionException(RowSuppressionReasonCode.QID_PROJECTED);
	}

	/**
	 * @param rejectCardinalityDependentAggregates true to reject every aggregate other than a
	 *                                             {@code COUNT} or an
	 *                                             {@link #CARDINALITY_INDEPENDENT_AGGREGATES}.
	 * @return the select columns whose value is a {@code COUNT}, in select-list order.
	 * @throws RowSuppressionException if a rejected aggregate is used, if a {@code COUNT} orders
	 *                                 the rows or is wrapped in an expression, or if the rows are
	 *                                 sorted by a select-list position.
	 */
	private static List<DerivedColumn> collectProtectedCounts(QuerySpecification model,
			boolean rejectCardinalityDependentAggregates) {
		rejectPositionalSort(model);
		// A 'select *' has no select columns of its own, so none of them can be a count.
		List<DerivedColumn> selectColumns = Boolean.TRUE.equals(model.getSelectList().getAsterisk()) ? List.of()
				: model.getSelectList().getColumns();
		Set<DerivedColumn> protectedColumns = Collections.newSetFromMap(new IdentityHashMap<>());
		for (SetFunctionSpecification aggregate : model.createIterable(SetFunctionSpecification.class)) {
			if (aggregate.getSetFunctionType() != SetFunctionType.COUNT) {
				if (!rejectCardinalityDependentAggregates) {
					continue;
				}
				// Any other aggregate whose value grows with the number of rows in a group is an
				// unsuppressed count in disguise: sum(1), sum(case ... then 1 else 0 end), or
				// group_concat(id), whose elements can be counted.
				if (!CARDINALITY_INDEPENDENT_AGGREGATES.contains(aggregate.getSetFunctionType())) {
					throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_NON_COUNT_AGGREGATE);
				}
				continue;
			}
			// Ordering by a COUNT sorts the rows by the true below-threshold counts even though each
			// cell is displayed as the suppressed value, leaking the relative magnitudes the
			// suppression exists to hide.
			if (aggregate.isInContext(OrderByClause.class)) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_ORDER_BY);
			}
			// A count outside the outer select list (e.g. in a subquery) is never displayed. The grammar
			// has no HAVING, so a count can never filter the groups on its true value.
			DerivedColumn column = aggregate.getContext(DerivedColumn.class).orElse(null);
			if (column == null || selectColumns.stream().noneMatch(selectColumn -> selectColumn == column)) {
				continue;
			}
			// A count wrapped in an expression (e.g. count(*) + 1) no longer equals the count that the
			// suppression threshold is compared against.
			if (!SQLTranslatorUtils.isWholeCount(column)) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_EXPRESSION);
			}
			protectedColumns.add(column);
		}
		List<DerivedColumn> inSelectOrder = selectColumns.stream().filter(protectedColumns::contains)
				.collect(Collectors.toList());
		rejectSortByProtectedCount(model, inSelectOrder);
		return inSelectOrder;
	}

	/**
	 * MySQL reads an unquoted integer sort key as a one-based position in the select list it executes,
	 * which translation can widen (a DOUBLE column expands to two), so the position cannot be tied to the
	 * column it sorts by. Any positional sort is therefore rejected rather than risk sorting by a count.
	 */
	private static void rejectPositionalSort(QuerySpecification model) {
		OrderByClause orderBy = model.getTableExpression().getOrderByClause();
		if (orderBy == null) {
			return;
		}
		for (SortSpecification sort : orderBy.getSortSpecificationList().getSortSpecifications()) {
			if (sort.getSortKey().toSql().matches("\\d+")) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_ORDER_BY);
			}
		}
	}

	/**
	 * Reject a sort key that refers to a protected count column by its name, since MySQL resolves an ORDER
	 * BY name to a select-list alias before a table column. A sort key that contains the {@code COUNT}
	 * itself is rejected by {@link #collectProtectedCounts(QuerySpecification, boolean)}.
	 */
	private static void rejectSortByProtectedCount(QuerySpecification model, List<DerivedColumn> protectedColumns) {
		OrderByClause orderBy = model.getTableExpression().getOrderByClause();
		if (orderBy == null || protectedColumns.isEmpty()) {
			return;
		}
		Set<String> protectedNames = new HashSet<>();
		for (DerivedColumn column : protectedColumns) {
			protectedNames.add(column.getDisplayName().toLowerCase());
		}
		for (SortSpecification sort : orderBy.getSortSpecificationList().getSortSpecifications()) {
			if (protectedNames.contains(getSortKeyName(sort.getSortKey()).toLowerCase())) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_ORDER_BY);
			}
		}
	}

	/**
	 * @return the unquoted name of a sort key that is a bare, unqualified column reference, or an
	 *         empty string for any other sort key. A qualified reference (e.g. {@code t.n}) always
	 *         names a table column, never an alias.
	 */
	private static String getSortKeyName(SortKey key) {
		ColumnReference reference = key.getFirstElementOfType(ColumnReference.class);
		if (reference == null || reference.getNameLHS().isPresent() || !reference.toSql().equals(key.toSql())) {
			return "";
		}
		return reference.toSqlWithoutQuotes();
	}

	/**
	 * @return true when the derived column applies an arithmetic, string, case, or cast
	 *         expression rather than being a plain column reference or aggregate.
	 */
	private static boolean isExpression(DerivedColumn column) {
		// TermPrime and FactorPrime are the AST nodes that carry an arithmetic operator applied to a
		// following operand, so their presence means the value is an arithmetic expression.
		return column.stream(TermPrime.class).findAny().isPresent()
				|| column.stream(FactorPrime.class).findAny().isPresent()
				|| column.stream(MySqlFunction.class).findAny().isPresent()
				|| column.stream(CaseExpression.class).findAny().isPresent()
				|| column.stream(CastSpecification.class).findAny().isPresent();
	}

	/**
	 * A reference that resolves to no column of the schema, or to a row metadata column, can never
	 * be a QID; the query translation rejects an unresolvable reference on its own.
	 */
	private static boolean isQid(ColumnReference reference, TableAndColumnMapper mapper, Set<String> qidColumnIds) {
		return mapper.lookupColumnReference(reference).flatMap(ColumnTranslationReference::getColumnId)
				.filter(qidColumnIds::contains).isPresent();
	}

}
