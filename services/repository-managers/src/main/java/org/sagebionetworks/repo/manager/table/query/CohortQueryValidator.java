package org.sagebionetworks.repo.manager.table.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.CohortDefinition;
import org.sagebionetworks.repo.model.table.ColumnCohortFilter;
import org.sagebionetworks.repo.model.table.FilterGroup;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.CohortReference;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SQLElement;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.WithListElement;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Validates the request-scoped cohorts of a {@link Query}: that the cohort references and definitions
 * match, and that each cohort query has the shape a cohort requires.
 */
public class CohortQueryValidator {

	/**
	 * Collect the name of every cohort referenced by the given query, either by {@code IN COHORT(<name>)}
	 * in its SQL or by a {@link ColumnCohortFilter} among its additional filters.
	 *
	 * @param model             the parsed SQL.
	 * @param additionalFilters the query's additional filters, may be null.
	 * @return the referenced names in first-seen order.
	 */
	public static Set<String> collectReferences(SQLElement model, List<QueryFilter> additionalFilters) {
		Set<String> names = new LinkedHashSet<>();
		model.stream(CohortReference.class).forEach(reference -> names.add(reference.getName()));
		collectCohortFilters(additionalFilters).forEach(filter -> names.add(filter.getCohortName()));
		return names;
	}

	/**
	 * @param filters the query's additional filters, may be null.
	 * @return every {@link ColumnCohortFilter} among the filters, including those nested in a
	 *         {@link FilterGroup}.
	 */
	public static List<ColumnCohortFilter> collectCohortFilters(List<QueryFilter> filters) {
		List<ColumnCohortFilter> cohortFilters = new ArrayList<>();
		collectCohortFilters(filters, cohortFilters);
		return cohortFilters;
	}

	private static void collectCohortFilters(List<QueryFilter> filters, List<ColumnCohortFilter> accumulator) {
		if (filters == null) {
			return;
		}
		for (QueryFilter filter : filters) {
			if (filter instanceof ColumnCohortFilter cohortFilter) {
				accumulator.add(cohortFilter);
			} else if (filter instanceof FilterGroup group) {
				collectCohortFilters(group.getChildren(), accumulator);
			}
		}
	}

	/**
	 * Validate that the cohorts referenced by the main query match its cohort definitions one-to-one.
	 *
	 * @param model      the parsed main SQL.
	 * @param query      the main query.
	 * @param maxCohorts the maximum number of definitions allowed.
	 * @throws IllegalArgumentException if there are too many definitions, a definition is malformed or
	 *                                  unused, or a reference names no definition.
	 */
	public static void validateDefinitions(QueryExpression model, Query query, int maxCohorts) {
		List<CohortDefinition> cohorts = query.getCohorts() == null ? Collections.emptyList() : query.getCohorts();
		if (cohorts.size() > maxCohorts) {
			throw new IllegalArgumentException("A query may define at most " + maxCohorts + " cohorts");
		}
		Set<String> defined = new HashSet<>();
		for (CohortDefinition cohort : cohorts) {
			ValidateArgument.required(cohort, "cohort");
			ValidateArgument.requirement(CohortReference.isValidName(cohort.getName()),
					"Cohort name must be a simple identifier (letters, digits and underscores, not starting with a digit)");
			ValidateArgument.required(cohort.getQuery(), "Cohort '" + cohort.getName() + "' query");
			if (!defined.add(cohort.getName())) {
				throw new IllegalArgumentException("Cohort '" + cohort.getName() + "' is defined more than once");
			}
		}
		Set<String> referenced = collectReferences(model, query.getAdditionalFilters());
		for (String name : referenced) {
			if (!defined.contains(name)) {
				throw new IllegalArgumentException("Unknown cohort: " + name);
			}
		}
		for (CohortDefinition cohort : cohorts) {
			if (!referenced.contains(cohort.getName())) {
				throw new IllegalArgumentException("Cohort '" + cohort.getName() + "' is defined but never referenced");
			}
		}
	}

	/**
	 * Validate the shape of a cohort's query and build the SQL that captures its distinct values.
	 * <p>
	 * A cohort is the set of distinct values of exactly one plain column of a single table. Truncation
	 * (limit, offset, sort) is rejected so a cohort cannot be narrowed to slip a small set past the
	 * aggregate threshold, and GROUP BY/HAVING are rejected because the source's own definition already
	 * aggregates.
	 *
	 * @param cohort a definition that passed {@link #validateDefinitions(QueryExpression, Query, int)}.
	 * @return the cohort's SQL with its selection made DISTINCT.
	 * @throws IllegalArgumentException if the cohort query violates a cohort rule.
	 */
	public static String createCohortSql(CohortDefinition cohort) {
		String name = cohort.getName();
		Query query = cohort.getQuery();
		ValidateArgument.required(query.getSql(), "Cohort '" + name + "' query.sql");
		rejectIf(query.getCohorts() != null && !query.getCohorts().isEmpty(), name, "may not define nested cohorts");
		rejectIf(query.getLimit() != null || query.getOffset() != null, name, "may not have a limit or offset");
		rejectIf(query.getSort() != null && !query.getSort().isEmpty(), name, "may not be sorted");

		QueryExpression model = parse(query.getSql(), name);
		rejectIf(!collectReferences(model, query.getAdditionalFilters()).isEmpty(), name, "may not reference another cohort");
		rejectIf(model.stream(WithListElement.class).findAny().isPresent() || model.stream(QuerySpecification.class).count() > 1,
				name, "must be a single query without WITH or UNION");

		QuerySpecification specification = model.getFirstElementOfType(QuerySpecification.class);
		rejectIf(specification.getSingleTableName().isEmpty(), name, "must query a single table without joins");
		TableExpression table = specification.getTableExpression();
		rejectIf(table.getGroupByClause() != null || table.getHavingClause() != null, name, "may not use GROUP BY or HAVING");
		rejectIf(table.getOrderByClause() != null, name, "may not be sorted");
		rejectIf(table.getPagination() != null, name, "may not have a limit or offset");
		rejectIf(!isSinglePlainColumn(specification), name, "must select exactly one column, without an alias, expression or aggregate");

		specification.replaceSelectList(specification.getSelectList(), SetQuantifier.DISTINCT);
		return model.toSql();
	}

	private static boolean isSinglePlainColumn(QuerySpecification specification) {
		List<DerivedColumn> columns = specification.getSelectList().getColumns();
		if (columns == null || columns.size() != 1) {
			return false;
		}
		DerivedColumn column = columns.get(0);
		ColumnReference reference = column.getValueExpression().getFirstElementOfType(ColumnReference.class);
		return !column.hasAsClause() && reference != null
				&& reference.toSql().equals(column.getValueExpression().toSql());
	}

	private static QueryExpression parse(String sql, String cohortName) {
		try {
			return new TableQueryParser(sql).queryExpression();
		} catch (ParseException e) {
			throw new IllegalArgumentException("Cohort '" + cohortName + "' query is invalid: " + e.getMessage(), e);
		}
	}

	private static void rejectIf(boolean violated, String cohortName, String rule) {
		if (violated) {
			throw new IllegalArgumentException("Cohort '" + cohortName + "' query " + rule);
		}
	}

}
