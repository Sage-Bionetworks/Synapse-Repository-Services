package org.sagebionetworks.repo.manager.table.query;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.query.model.CaseExpression;
import org.sagebionetworks.table.query.model.CastSpecification;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DefiningClause;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.FactorPrime;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.OrderByClause;
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
 * the rows, whether directly, by its alias, or by its position.</li>
 * <li>No other aggregate whose value depends on the number of rows in a group (e.g. {@code SUM},
 * {@code GROUP_CONCAT}) may be used, since it would reveal a count without suppression. Only
 * {@code MIN}, {@code MAX}, and {@code AVG} of a non-QID column are permitted.</li>
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
	 * @return the zero-based indexes into the select list (and therefore into each result
	 *         row's values) of every column that is a {@code COUNT}. These are the cells
	 *         subject to cell-level k-anonymity suppression.
	 * @throws RowSuppressionException if a QID or a {@code COUNT} is used in a way that is not
	 *                                 permitted for a query requesting row results. A QID
	 *                                 violation takes precedence over a {@code COUNT} violation.
	 */
	public static List<Integer> validate(QuerySpecification model, Set<String> qidColumnIds,
			SchemaProvider schemaProvider) {
		ValidateArgument.required(model, "model");
		ValidateArgument.required(qidColumnIds, "qidColumnIds");
		ValidateArgument.required(schemaProvider, "schemaProvider");

		if (!qidColumnIds.isEmpty()) {
			validateQidUses(model, qidColumnIds, schemaProvider);
		}
		// Every row an aggregate-only source contributes carries its quasi-identifiers, so a count over
		// those rows is a protected count whatever column it counts. This holds even when the queried
		// object projects no QID-derived column at all.
		return collectProtectedCounts(model);
	}

	private static void validateQidUses(QuerySpecification model, Set<String> qidColumnIds,
			SchemaProvider schemaProvider) {
		// 'select *' projects every column, which necessarily exposes each QID.
		if (Boolean.TRUE.equals(model.getSelectList().getAsterisk())) {
			throw new RowSuppressionException(RowSuppressionReasonCode.QID_PROJECTED);
		}

		// A reference is resolved to the column it actually reads, so the restriction holds however
		// the caller spelled it: an alias or a correlation prefix changes the text, not the column.
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, schemaProvider);

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
	 * @return the select-list indexes of every {@code COUNT}, in select-list order.
	 * @throws RowSuppressionException if an aggregate other than a {@code COUNT} or an
	 *                                 {@link #CARDINALITY_INDEPENDENT_AGGREGATES} is used, or if a
	 *                                 {@code COUNT} orders the rows or is wrapped in an expression.
	 */
	private static List<Integer> collectProtectedCounts(QuerySpecification model) {
		List<DerivedColumn> selectColumns = model.getSelectList().getColumns();
		Set<Integer> protectedIndexes = new LinkedHashSet<>();
		for (SetFunctionSpecification aggregate : model.createIterable(SetFunctionSpecification.class)) {
			if (aggregate.getSetFunctionType() != SetFunctionType.COUNT) {
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
			int index = column == null ? -1 : indexOfIdentity(selectColumns, column);
			if (index < 0) {
				continue;
			}
			// A count wrapped in an expression (e.g. count(*) + 1) no longer equals the count that the
			// suppression threshold is compared against.
			if (!isWholeValue(column, aggregate)) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_EXPRESSION);
			}
			protectedIndexes.add(index);
		}
		rejectSortByProtectedCount(model, selectColumns, protectedIndexes);
		return new ArrayList<>(protectedIndexes);
	}

	/**
	 * Reject a sort key that refers to a protected count column by its name or by its position,
	 * since MySQL resolves an ORDER BY name to a select-list alias before a table column, and reads
	 * an unquoted integer as a one-based select-list position. A sort key that contains the
	 * {@code COUNT} itself is rejected by {@link #collectProtectedCounts(QuerySpecification)}.
	 */
	private static void rejectSortByProtectedCount(QuerySpecification model, List<DerivedColumn> selectColumns,
			Set<Integer> protectedIndexes) {
		OrderByClause orderBy = model.getTableExpression().getOrderByClause();
		if (orderBy == null || protectedIndexes.isEmpty()) {
			return;
		}
		Set<String> protectedNames = new HashSet<>();
		for (int index : protectedIndexes) {
			protectedNames.add(selectColumns.get(index).getDisplayName().toLowerCase());
		}
		for (SortSpecification sort : orderBy.getSortSpecificationList().getSortSpecifications()) {
			SortKey key = sort.getSortKey();
			if (protectedNames.contains(getSortKeyName(key).toLowerCase())
					|| getSortKeyPosition(key).filter(position -> protectedIndexes.contains(position - 1)).isPresent()) {
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
	 * @return the one-based select-list position of a sort key that is an unquoted integer.
	 */
	private static Optional<Integer> getSortKeyPosition(SortKey key) {
		String sql = key.toSql();
		if (!sql.matches("\\d{1,9}")) {
			return Optional.empty();
		}
		return Optional.of(Integer.parseInt(sql));
	}

	/**
	 * @return true when the count is the entire value of the column. An expression inside the
	 *         count's own argument (e.g. {@code count(distinct concat(a, b))}) still leaves a
	 *         plain count.
	 */
	private static boolean isWholeValue(DerivedColumn column, SetFunctionSpecification count) {
		return column.getValueExpression().toSql().equals(count.toSql());
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

	private static int indexOfIdentity(List<DerivedColumn> columns, DerivedColumn target) {
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i) == target) {
				return i;
			}
		}
		return -1;
	}

}
