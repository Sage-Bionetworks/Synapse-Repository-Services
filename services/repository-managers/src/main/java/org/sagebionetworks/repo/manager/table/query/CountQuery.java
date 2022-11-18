package org.sagebionetworks.repo.manager.table.query;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUtils;

/**
 * An immutable count query to be run against a table/view. The count query will
 * include the original where clause plus any additional filters and/or facet
 * selections.
 *
 */
public class CountQuery {

	private final BasicQuery countQuery;
	private final Pagination originalPagination;

	private CountQuery(String startingSql, TranslationDependencies dependencies,
			List<FacetColumnRequest> selectedFacets, List<QueryFilter> additionalFilters) {

		try {
			CombinedQuery combined = CombinedQuery.builder().setQuery(startingSql)
					.setAdditionalFilters(additionalFilters).setSchemaProvider(dependencies.getSchemaProvider())
					.setSelectedFacets(selectedFacets).build();

			QuerySpecification model = new TableQueryParser(combined.getCombinedSql()).querySpecification();
			originalPagination = model.getFirstElementOfType(Pagination.class);
			SqlQuery sqlQuery = new SqlQueryBuilder(combined.getCombinedSql(), dependencies).build();

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

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String startingSql;
		private TranslationDependencies dependencies;
		private List<FacetColumnRequest> selectedFacets;
		private List<QueryFilter> additionalFilters;

		/**
		 * @param startingSql the startingSql to set
		 */
		public Builder setStartingSql(String startingSql) {
			this.startingSql = startingSql;
			return this;
		}

		/**
		 * @param selectedFacets the selectedFacets to set
		 */
		public Builder setSelectedFacets(List<FacetColumnRequest> selectedFacets) {
			this.selectedFacets = selectedFacets;
			return this;
		}

		/**
		 * @param additionalFilters the additionalFilters to set
		 */
		public Builder setAdditionalFilters(List<QueryFilter> additionalFilters) {
			this.additionalFilters = additionalFilters;
			return this;
		}

		/**
		 * @param dependencies the dependencies to set
		 */
		public Builder setDependencies(TranslationDependencies dependencies) {
			this.dependencies = dependencies;
			return this;
		}

		public CountQuery build() {
			return new CountQuery(startingSql, dependencies, selectedFacets, additionalFilters);
		}
	}
}
