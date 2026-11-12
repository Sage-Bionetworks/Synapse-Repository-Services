package org.sagebionetworks.repo.manager.table.query;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.manager.table.query.AggregateQidQueryValidator.ProtectedCounts;
import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.CountSuppressionSpec;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.util.ValidateArgument;

public class MainQuery {

	private static final ProtectedCounts NO_PROTECTED_COUNTS = new ProtectedCounts(List.of(), List.of());

	private final QueryTranslator sqlQuery;
	private final ProtectedCounts protectedCounts;

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
		protectedCounts = expansion.getQidColumnIds()
				.map(qidColumnIds -> AggregateQidQueryValidator.validate(parse(combined), qidColumnIds,
						expansion.getAggregateQidColumnIds(), expansion.getSchemaProvider()))
				.orElse(NO_PROTECTED_COUNTS);

		sqlQuery = QueryTranslator.builder(combined.getCombinedSql(), expansion.getUserId())
				.schemaProvider(expansion.getSchemaProvider()).indexDescription(expansion.getIndexDescription())
				.maxBytesPerPage(expansion.getMaxBytesPerPage()).includeEntityEtag(expansion.getIncludeEntityEtag())
				.countSuppressionSpec(createCountSuppressionSpec(expansion.getAggregateDataConfiguration(),
						protectedCounts))
				.build();
	}

	/**
	 * The query as it executes. A VirtualTable's defining SQL is inlined as a CTE.
	 */
	static QueryExpression parse(CombinedQuery combined) {
		try {
			return new TableQueryParser(combined.getCombinedSql()).queryExpression();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Build the cell-level k-anonymity spec for the main (row-returning) query, or null when there is
	 * nothing to suppress. Suppression applies only when the source is aggregate-bound and the query
	 * reads at least one protected count.
	 */
	static CountSuppressionSpec createCountSuppressionSpec(Optional<AggregateDataConfiguration> config,
			ProtectedCounts protectedCounts) {
		if (config.isEmpty()
				|| (protectedCounts.selectIndexes().isEmpty() && protectedCounts.definingSelectIndexes().isEmpty())) {
			return null;
		}
		AggregateDataConfiguration configuration = config.get();
		// When the ACT has not chosen a strategy, below-threshold rows are excluded.
		AggregateCountSuppressionStrategy strategy = configuration.getCountSuppressionStrategy() == null
				? AggregateCountSuppressionStrategy.EXCLUDE_ROW
				: configuration.getCountSuppressionStrategy();
		return new CountSuppressionSpec(strategy, configuration.getSuppressionThreshold(),
				protectedCounts.selectIndexes(), protectedCounts.definingSelectIndexes());
	}

	public QueryTranslator getTranslator() {
		return sqlQuery;
	}

	/**
	 * @return The zero-based indexes into each result row's values of the columns that are a
	 *         protected {@code COUNT} and have cell-level k-anonymity applied. Empty unless
	 *         this is an aggregate read that returns rows and projects such a column.
	 */
	public List<Integer> getProtectedCountColumnIndexes() {
		return protectedCounts.selectIndexes();
	}

	/**
	 * @return The zero-based indexes into the select list of an inlined VirtualTable's defining SQL of
	 *         the counts that have cell-level k-anonymity applied where they are computed. Empty
	 *         unless this is an aggregate read that returns rows and reads such a column.
	 */
	public List<Integer> getDefiningCountColumnIndexes() {
		return protectedCounts.definingSelectIndexes();
	}

}
