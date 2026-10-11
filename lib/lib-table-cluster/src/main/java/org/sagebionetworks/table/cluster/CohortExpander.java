package org.sagebionetworks.table.cluster;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.CohortReference;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.InValueList;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.util.SqlElementUtils;

/**
 * Replaces each {@code <column> IN COHORT(<name>)} of a query with the resolved cohort's values.
 * <p>
 * Error messages name the cohort and column but never the cohort's values or size, since the values
 * must not be disclosed to the caller.
 */
public class CohortExpander {

	/**
	 * Expand every cohort reference in the given query in place.
	 *
	 * @param queryExpression the query to expand.
	 * @param mapper          resolves the left-hand column of each cohort predicate.
	 * @param cohorts         the resolved cohorts keyed by name.
	 * @throws IllegalArgumentException if a reference names an unknown cohort, its left-hand side is not a
	 *                                  column of the query, or the column's type differs from the cohort's.
	 */
	public static void expandCohorts(QueryExpression queryExpression, TableAndColumnMapper mapper,
			Map<String, ResolvedCohort> cohorts) throws ParseException {
		// Collect first, since expanding replaces elements of the tree being walked.
		List<InPredicate> cohortPredicates = queryExpression.stream(InPredicate.class)
				.filter(predicate -> predicate.getInPredicateValue().getCohortReference().isPresent())
				.collect(Collectors.toList());
		for (InPredicate predicate : cohortPredicates) {
			CohortReference reference = predicate.getInPredicateValue().getCohortReference().get();
			ResolvedCohort cohort = cohorts.get(reference.getName());
			if (cohort == null) {
				throw new IllegalArgumentException("Unknown cohort: " + reference.getName());
			}
			validateColumnType(predicate, mapper, cohort);
			replaceWithValues(predicate.getInPredicateValue(), cohort.values());
		}
	}

	static void validateColumnType(InPredicate predicate, TableAndColumnMapper mapper, ResolvedCohort cohort) {
		Element leftHandSide = predicate.getLeftHandSide().getChild();
		if (!(leftHandSide instanceof ColumnReference columnReference)) {
			throw new IllegalArgumentException(
					"The left-hand side of IN COHORT(" + cohort.name() + ") must be a column reference");
		}
		ColumnTranslationReference column = mapper.lookupColumnReference(columnReference)
				.orElseThrow(() -> new IllegalArgumentException(
						"Column does not exist: " + columnReference.toSqlWithoutQuotes()));
		ColumnType columnType = column.getColumnType();
		if (!cohort.columnType().equals(columnType)) {
			throw new IllegalArgumentException(String.format(
					"Cohort '%s' selects a column of type %s, which is not compatible with column '%s' of type %s",
					cohort.name(), cohort.columnType(), column.getUserQueryColumnName(), columnType));
		}
	}

	static void replaceWithValues(InPredicateValue inPredicateValue, List<String> values) throws ParseException {
		// An empty cohort must match nothing. Comparing against NULL is never true, for IN and NOT IN alike,
		// and stays not-true under any enclosing NOT. Otherwise, parse the whole list at once; the downstream
		// translation binds each literal to a variable.
		String valueList = values.isEmpty() ? "(NULL)"
				: values.stream().map(CohortExpander::toSingleQuotedLiteral).collect(Collectors.joining(",", "(", ")"));
		InValueList inValueList = SqlElementUtils.createInPredicateValue(valueList).getFirstElementOfType(InValueList.class);
		inPredicateValue.replaceChildren(inValueList);
		inPredicateValue.recursiveSetParent();
	}

	private static String toSingleQuotedLiteral(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

}
