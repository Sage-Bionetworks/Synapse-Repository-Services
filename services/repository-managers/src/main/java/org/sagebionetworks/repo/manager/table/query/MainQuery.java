package org.sagebionetworks.repo.manager.table.query;

import java.util.Set;

import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.CountPostProcessing;
import org.sagebionetworks.table.cluster.CountSuppressionSpec;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.util.ValidateArgument;

public class MainQuery {

	private final QueryTranslator sqlQuery;
	private final boolean countsPostProcessed;

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
		CountSuppressionSpec countSuppressionSpec = expansion.getQidColumns()
				.map(qidColumns -> AggregateQidQueryValidator.validate(parse(combined), qidColumns,
						expansion.getInlinedSourceQidColumns(), expansion.getSchemaProvider()))
				.map(definingCountColumnIds -> {
					AggregateDataConfiguration configuration = expansion.getAggregateDataConfiguration()
							.orElseThrow(() -> new IllegalStateException(
									"Quasi-identifier columns require an aggregate data configuration"));
					CountPostProcessing postProcessing = expansion.getCountPostProcessing().orElseThrow(
							() -> new IllegalStateException("Quasi-identifier columns require a count post-processing"));
					// The rows release every count, of the query and of its defining SQL, obscured alike.
					return createCountSuppressionSpec(configuration, definingCountColumnIds, postProcessing,
							postProcessing);
				}).orElse(null);
		countsPostProcessed = countSuppressionSpec != null;

		sqlQuery = QueryTranslator.builder(combined.getCombinedSql(), expansion.getUserId())
				.schemaProvider(expansion.getSchemaProvider()).indexDescription(expansion.getIndexDescription())
				.maxBytesPerPage(expansion.getMaxBytesPerPage()).includeEntityEtag(expansion.getIncludeEntityEtag())
				.countSuppressionSpec(countSuppressionSpec)
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
	 * Build the cell-level k-anonymity spec for an aggregate query. Every count of the query is protected,
	 * along with each named count computed by an inlined VirtualTable's defining SQL.
	 *
	 * @param configuration          the aggregate data configuration of the read.
	 * @param definingCountColumnIds the ids of the inlined object's columns whose value is a count computed
	 *                               by its defining SQL and read by the query.
	 * @param outerCountPostProcessing    how a count of the query that is not suppressed is obscured before the
	 *                                    SQL releases it.
	 * @param definingCountPostProcessing how a count of the inlined defining SQL that is not suppressed is
	 *                                    obscured where it is computed.
	 */
	public static CountSuppressionSpec createCountSuppressionSpec(AggregateDataConfiguration configuration,
			Set<String> definingCountColumnIds, CountPostProcessing outerCountPostProcessing,
			CountPostProcessing definingCountPostProcessing) {
		ValidateArgument.required(configuration, "configuration");
		ValidateArgument.required(configuration.getSuppressionThreshold(), "configuration.suppressionThreshold");
		ValidateArgument.required(definingCountColumnIds, "definingCountColumnIds");
		// When the ACT has not chosen a strategy, below-threshold rows are excluded.
		AggregateCountSuppressionStrategy strategy = configuration.getCountSuppressionStrategy() == null
				? AggregateCountSuppressionStrategy.EXCLUDE_ROW
				: configuration.getCountSuppressionStrategy();
		return new CountSuppressionSpec(strategy, configuration.getSuppressionThreshold(), definingCountColumnIds,
				outerCountPostProcessing, definingCountPostProcessing);
	}

	public QueryTranslator getTranslator() {
		return sqlQuery;
	}

	/**
	 * @return true when every count the query returns in its rows, of the query and of an inlined defining
	 *         SQL, is suppressed below the threshold and otherwise post-processed, so the counts are approximate.
	 */
	public boolean isCountPostProcessingApplied() {
		return countsPostProcessed;
	}

}
