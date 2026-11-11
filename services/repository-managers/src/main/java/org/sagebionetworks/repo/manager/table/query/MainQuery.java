package org.sagebionetworks.repo.manager.table.query;

import java.util.Set;

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

	private final QueryTranslator sqlQuery;

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
						expansion.getSchemaProvider()))
				.map(definingCountColumnIds -> createCountSuppressionSpec(
						expansion.getAggregateDataConfiguration().orElseThrow(() -> new IllegalStateException(
								"Quasi-identifier columns require an aggregate data configuration")),
						definingCountColumnIds))
				.orElse(null);

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
	 */
	public static CountSuppressionSpec createCountSuppressionSpec(AggregateDataConfiguration configuration,
			Set<String> definingCountColumnIds) {
		ValidateArgument.required(configuration, "configuration");
		ValidateArgument.required(configuration.getSuppressionThreshold(), "configuration.suppressionThreshold");
		ValidateArgument.required(definingCountColumnIds, "definingCountColumnIds");
		// When the ACT has not chosen a strategy, below-threshold rows are excluded.
		AggregateCountSuppressionStrategy strategy = configuration.getCountSuppressionStrategy() == null
				? AggregateCountSuppressionStrategy.EXCLUDE_ROW
				: configuration.getCountSuppressionStrategy();
		return new CountSuppressionSpec(strategy, configuration.getSuppressionThreshold(), definingCountColumnIds);
	}

	public QueryTranslator getTranslator() {
		return sqlQuery;
	}

}
