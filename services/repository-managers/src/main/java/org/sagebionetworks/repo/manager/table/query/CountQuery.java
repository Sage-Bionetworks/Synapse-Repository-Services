package org.sagebionetworks.repo.manager.table.query;

import java.util.Optional;

import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
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
	
	public CountQuery(QueryContext expansion) {
		ValidateArgument.required(expansion, "expansion");

		try {
			CombinedQuery combined = CombinedQuery.builder().setQuery(expansion.getStartingSql())
					.setAdditionalFilters(expansion.getAdditionalFilters())
					.setSchemaProvider(expansion.getSchemaProvider()).setSelectedFacets(expansion.getSelectedFacets())
					.build();

			QuerySpecification model = new TableQueryParser(combined.getCombinedSql()).querySpecification();
			originalPagination = model.getFirstElementOfType(Pagination.class);
			QueryTranslator sqlQuery = QueryTranslator.builder(combined.getCombinedSql(), expansion.getUserId())
					.schemaProvider(expansion.getSchemaProvider()).indexDescription(expansion.getIndexDescription())
					.build();

			// if a count cannot be run then this will be null.
			countQuery = SqlElementUtils.createCountSql(sqlQuery.getTransformedModel()).map(counSql -> {
				return new BasicQuery(counSql, sqlQuery.getParameters());
			}).orElse(null);

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

	public Pagination getOrignialPagination() {
		return originalPagination;
	}

}
