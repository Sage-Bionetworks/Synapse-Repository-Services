package org.sagebionetworks.repo.manager.table.query;

import java.util.Optional;

import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An immutable count query to be run against a table/view. The count query will
 * include the original where clause plus any additional filters and/or facet
 * selections.
 *
 */
public class CountQuery {

	private final BasicQuery countQuery;
	private final Pagination originalPagination;
	private final String tableHash;
	private final String singleTableId;

	public CountQuery(QueryContext expansion) {
		this(expansion, false);
	}

	/**
	 * @param expansion    The expanded query context.
	 * @param forceRowCount When true, a plain {@code COUNT(*)} of the rows matched
	 *                      by the where clause (plus additional filters/facets) is
	 *                      always produced, ignoring the caller's select list,
	 *                      grouping, ordering and pagination. This yields the size
	 *                      of the underlying row set (cohort) rather than the number
	 *                      of result rows, which is what the aggregate-only
	 *                      suppression gate must measure. When false, the count
	 *                      mirrors the result set of the caller's query (and may be
	 *                      absent for aggregate queries that cannot be counted).
	 */
	public CountQuery(QueryContext expansion, boolean forceRowCount) {
		ValidateArgument.required(expansion, "expansion");

		try {
			CombinedQuery combined = CombinedQuery.builder().setQuery(expansion.getStartingSql())
					.setAdditionalFilters(expansion.getAdditionalFilters())
					.setSchemaProvider(expansion.getSchemaProvider()).setSelectedFacets(expansion.getSelectedFacets())
					.build();

			QueryExpression queryExpression = new TableQueryParser(combined.getCombinedSql()).queryExpression();
			QuerySpecification model = queryExpression.getFirstElementOfType(QuerySpecification.class);

			boolean hasCount;
			if (forceRowCount) {
				// Force a plain COUNT(*) of the matched rows: strip the select list,
				// grouping, ordering and pagination so the result is the cohort size.
				model.replaceSelectList(new TableQueryParser("COUNT(*)").selectList(), null);
				model.getTableExpression().replaceGroupBy(null);
				model.getTableExpression().replaceOrderBy(null);
				model.getTableExpression().replacePagination(null);
				originalPagination = null;
				hasCount = true;
			} else {
				originalPagination = model.getFirstElementOfType(Pagination.class);
				hasCount = SqlElementUtils.createCountSql(model);
			}

			if (hasCount) {
				QueryTranslator sqlQuery = QueryTranslator.builder(queryExpression.toSql(), expansion.getUserId())
					.schemaProvider(expansion.getSchemaProvider())
					.indexDescription(expansion.getIndexDescription())
					.build();

				countQuery = new BasicQuery(
					sqlQuery.getTranslatedModel().toSql(),
					sqlQuery.getParameters()
				);

				tableHash = sqlQuery.getIndexDescription().getTableHash();
				singleTableId = sqlQuery.getSingleTableId();

			} else {
				countQuery = null;
				tableHash = null;
				singleTableId = null;
			}
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 *
	 * @return {@link Optional#empty()} if a count cannot be run against the
	 *         provided input.
	 */
	public Optional<BasicQuery> getCountQuery() {
		return Optional.ofNullable(countQuery);
	}

	public Pagination getOriginalPagination() {
		return originalPagination;
	}

	public String getTableHash() {
		return tableHash;
	}

	public String getSingleTableId() {
		return singleTableId;
	}



}
