package org.sagebionetworks.repo.manager.table.query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.query.model.CaseExpression;
import org.sagebionetworks.table.query.model.CastSpecification;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.FactorPrime;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.TermPrime;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Enforces the count-only restriction that an aggregate-only source places on its
 * quasi-identifier (QID) columns when a query requests row results. A QID column may
 * appear only as the argument of a {@code COUNT} (or {@code COUNT(DISTINCT ...)}) or in a
 * {@code WHERE} clause. Any other use exposes the QID as more than a count and causes the
 * query's row results to be withheld via a {@link RowSuppressionException}.
 */
public class AggregateQidQueryValidator {

	/**
	 * Validates that every configured QID column is used only in a count-only manner and
	 * identifies the output columns whose values are participant counts protected by
	 * cell-level k-anonymity.
	 *
	 * @param model        the parsed user query.
	 * @param qidColumnNames the configured quasi-identifier column names.
	 * @param sourceSchema the columns of the aggregate-only source.
	 * @return the zero-based indexes into the select list (and therefore into each result
	 *         row's values) of the columns that are a {@code COUNT} of a QID. These are the
	 *         cells subject to cell-level k-anonymity suppression.
	 * @throws IllegalStateException   if a configured QID name does not match any column of the
	 *                                 source, which is an ACT-side configuration fault rather
	 *                                 than a fault in the caller's request.
	 * @throws RowSuppressionException if a QID is used in a way that is not permitted for a
	 *                                 query requesting row results.
	 */
	public static List<Integer> validate(QuerySpecification model, List<String> qidColumnNames,
			List<ColumnModel> sourceSchema) {
		ValidateArgument.required(model, "model");
		ValidateArgument.required(qidColumnNames, "qidColumnNames");
		ValidateArgument.required(sourceSchema, "sourceSchema");

		Set<String> qids = qidColumnNames.stream().map(AggregateQidQueryValidator::normalize)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		// A QID name that does not resolve to a real source column cannot be protected. This is a
		// fault in the ACT-bound configuration, not in the caller's request, so it is signaled as
		// an illegal state (HTTP 500) rather than a bad request (HTTP 400).
		Set<String> sourceColumnNames = sourceSchema.stream().map(ColumnModel::getName)
				.map(AggregateQidQueryValidator::normalize).collect(Collectors.toSet());
		for (String qid : qids) {
			if (!sourceColumnNames.contains(qid)) {
				throw new IllegalStateException(
						"The configured quasi-identifier column does not match any column of the source: " + qid);
			}
		}

		// 'select *' projects every source column, which necessarily exposes each QID.
		if (Boolean.TRUE.equals(model.getSelectList().getAsterisk())) {
			throw new RowSuppressionException(RowSuppressionReasonCode.QID_PROJECTED);
		}

		List<DerivedColumn> selectColumns = model.getSelectList().getColumns();
		Set<Integer> protectedIndexes = new LinkedHashSet<>();

		for (ColumnReference reference : model.createIterable(ColumnReference.class)) {
			if (!isQid(reference, qids)) {
				continue;
			}
			classifyQidReference(reference, model, selectColumns, protectedIndexes);
		}

		return new ArrayList<>(protectedIndexes);
	}

	/**
	 * Classify a single QID reference by its location in the query, throwing when the use is
	 * not permitted and recording the select-list index when the use is a protected count.
	 */
	private static void classifyQidReference(ColumnReference reference, QuerySpecification model,
			List<DerivedColumn> selectColumns, Set<Integer> protectedIndexes) {
		// A QID used only to filter never appears in the results.
		if (reference.isInContext(WhereClause.class)) {
			return;
		}
		// The only aggregate that yields a count (and is therefore suppressible) is COUNT;
		// every other aggregate would expose the QID's values.
		Optional<SetFunctionSpecification> setFunction = reference.getContext(SetFunctionSpecification.class);
		if (setFunction.isPresent()) {
			if (setFunction.get().getSetFunctionType() != SetFunctionType.COUNT) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_NON_COUNT_AGGREGATE);
			}
			// Ordering by a COUNT of a QID sorts the rows by the true below-threshold counts even
			// though each cell is displayed as the suppressed value, leaking the relative
			// magnitudes the suppression exists to hide.
			if (reference.isInContext(OrderByClause.class)) {
				throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_ORDER_BY);
			}
			markProtectedCountIfInSelect(reference, selectColumns, protectedIndexes);
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
	 * When a permitted {@code COUNT(qid)} is an output column, record its index so its cells
	 * can be suppressed. The count must be the whole output value: a count wrapped in an
	 * expression (e.g. {@code count(qid) + 1}) no longer equals the participant count that
	 * the suppression threshold is compared against, so it is rejected.
	 */
	private static void markProtectedCountIfInSelect(ColumnReference reference, List<DerivedColumn> selectColumns,
			Set<Integer> protectedIndexes) {
		DerivedColumn column = reference.getContext(DerivedColumn.class).orElse(null);
		if (column == null) {
			return;
		}
		int index = indexOfIdentity(selectColumns, column);
		if (index < 0) {
			return;
		}
		if (isExpression(column)) {
			throw new RowSuppressionException(RowSuppressionReasonCode.QID_IN_EXPRESSION);
		}
		protectedIndexes.add(index);
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

	private static boolean isQid(ColumnReference reference, Set<String> qids) {
		return qids.contains(normalize(reference.getNameRHS().toSqlWithoutQuotes()));
	}

	private static int indexOfIdentity(List<DerivedColumn> columns, DerivedColumn target) {
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i) == target) {
				return i;
			}
		}
		return -1;
	}

	private static String normalize(String name) {
		return name == null ? null : name.trim().toUpperCase(Locale.ROOT);
	}

}
