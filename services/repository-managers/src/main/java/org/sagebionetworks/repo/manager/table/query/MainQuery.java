package org.sagebionetworks.repo.manager.table.query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.CountSuppressionSpec;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.util.ValidateArgument;

public class MainQuery {

	private final QueryTranslator sqlQuery;
	private final List<Integer> protectedCountColumnIndexes;

	/**
	 * @throws RowSuppressionException if this is an aggregate read that returns rows and the query,
	 *                                 including the parts the request supplies on its own (sort,
	 *                                 selected facets, additional filters), uses a quasi-identifier or
	 *                                 a protected count in a way that is not permitted.
	 */
	public MainQuery(QueryContext expansion) {

		ValidateArgument.required(expansion, "expansion");

		CombinedQuery combined = CombinedQuery.builder().setQuery(expansion.getStartingSql())
				.setSchemaProvider(expansion.getSchemaProvider()).setOverrideOffset(expansion.getOffset())
				.setOverrideLimit(expansion.getLimit()).setSelectedFacets(expansion.getSelectedFacets())
				.setSortList(expansion.getSort()).setAdditionalFilters(expansion.getAdditionalFilters()).setCohorts(expansion.getCohorts()).build();

		// The restriction is enforced on the query as it executes, not on the SQL the user wrote, so a
		// request-supplied sort cannot order the rows by a quasi-identifier or by a protected count.
		protectedCountColumnIndexes = expansion.getQidColumnIds()
				.map(qidColumnIds -> AggregateQidQueryValidator.validate(getOuterQuerySpecification(combined),
						qidColumnIds, expansion.getSchemaProvider()))
				.orElse(Collections.emptyList());

		sqlQuery = QueryTranslator.builder(combined.getCombinedSql(), expansion.getUserId())
				.schemaProvider(expansion.getSchemaProvider()).indexDescription(expansion.getIndexDescription())
				.maxBytesPerPage(expansion.getMaxBytesPerPage()).includeEntityEtag(expansion.getIncludeEntityEtag())
				.countSuppressionSpec(createCountSuppressionSpec(expansion.getAggregateDataConfiguration(),
						protectedCountColumnIndexes))
				.build();
	}

	/**
	 * The query specification whose select list defines the results. A VirtualTable's defining SQL is
	 * inlined as a CTE, which this skips.
	 */
	static QuerySpecification getOuterQuerySpecification(CombinedQuery combined) {
		try {
			return new TableQueryParser(combined.getCombinedSql()).queryExpression()
					.getNonJoinQueryExpression().getFirstElementOfType(QuerySpecification.class);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Build the cell-level k-anonymity spec for the main (row-returning) query, or null when there is
	 * nothing to suppress. Suppression applies only when the source is aggregate-bound and the query
	 * projects at least one protected count.
	 */
	static CountSuppressionSpec createCountSuppressionSpec(Optional<AggregateDataConfiguration> config,
			List<Integer> protectedIndexes) {
		if (config.isEmpty() || protectedIndexes.isEmpty()) {
			return null;
		}
		AggregateDataConfiguration configuration = config.get();
		// When the ACT has not chosen a strategy, below-threshold rows are excluded.
		AggregateCountSuppressionStrategy strategy = configuration.getCountSuppressionStrategy() == null
				? AggregateCountSuppressionStrategy.EXCLUDE_ROW
				: configuration.getCountSuppressionStrategy();
		return new CountSuppressionSpec(strategy, configuration.getSuppressionThreshold(), protectedIndexes);
	}

	public QueryTranslator getTranslator() {
		return sqlQuery;
	}

	/**
	 * @return The zero-based indexes into each result row's values of the columns that are a
	 *         protected {@code COUNT} and have cell-level k-anonymity applied. Empty unless this is
	 *         an aggregate read that returns rows and projects such a column.
	 */
	public List<Integer> getProtectedCountColumnIndexes() {
		return protectedCountColumnIndexes;
	}

}
