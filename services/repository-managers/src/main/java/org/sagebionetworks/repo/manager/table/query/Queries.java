package org.sagebionetworks.repo.manager.table.query;

import java.util.Optional;

import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.cluster.description.IndexDescription;

public class Queries {

	private final MainQuery mainQuery;
	private final FacetQueries facetQueries;
	private final CountQuery countQuery;
	private final SumFileSizesQuery sumFileSizesQuery;

	private Queries(String startingSql, SchemaProvider schemaProvider, IndexDescription indexDescription, Long userId,
			Long maxBytesPerPage, Query query, QueryOptions options, Long maxRowsPerCall) {
		CachingSchemaProvider providerCache = new CachingSchemaProvider(schemaProvider);
		TranslationDependencies dependencies = TranslationDependencies.builder().setSchemaProvider(providerCache)
				.setIndexDescription(indexDescription).setUserId(userId).build();

		mainQuery = MainQuery.builder().setStartingSql(startingSql).setDependencies(dependencies)
				.setMaxBytesPerPage(maxBytesPerPage).setQuery(query).build();
		if (options.returnFacets()) {
			facetQueries = FacetQueries.builder().setOriginalSql(startingSql).setReturnFacets(true)
					.setSelectedFacets(query.getSelectedFacets()).setAdditionalFilters(query.getAdditionalFilters()).setDependencies(dependencies).build();
		} else {
			facetQueries = null;
		}

		if (options.runCount()) {
			countQuery = CountQuery.builder().setStartingSql(startingSql)
					.setAdditionalFilters(query.getAdditionalFilters()).setSelectedFacets(query.getSelectedFacets())
					.setDependencies(dependencies).build();
		} else {
			countQuery = null;
		}
		if (options.runSumFileSizes()) {
			sumFileSizesQuery = SumFileSizesQuery.builder().setStartingSql(startingSql)
					.setAdditionalFilters(query.getAdditionalFilters()).setSelectedFacets(query.getSelectedFacets())
					.setMaxRowsPerCall(maxRowsPerCall).setDependencies(dependencies).build();
		} else {
			sumFileSizesQuery = null;
		}
	}

	public MainQuery getMainQuery() {
		return mainQuery;
	}

	public Optional<CountQuery> getCountQuery() {
		return Optional.ofNullable(countQuery);
	}

	public Optional<FacetQueries> getFacetQueries() {
		return Optional.ofNullable(facetQueries);
	}

	public Optional<SumFileSizesQuery> getSumFileSizesQuery() {
		return Optional.ofNullable(sumFileSizesQuery);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String startingSql;
		private SchemaProvider schemaProvider;
		private IndexDescription indexDescription;
		private Long userId;
		private Long maxBytesPerPage;
		private Query query;
		private QueryOptions options;
		private Long maxRowsPerCall;

		/**
		 * @param startingSql the startingSql to set
		 */
		public Builder setStartingSql(String startingSql) {
			this.startingSql = startingSql;
			return this;
		}

		/**
		 * @param schemaProvider the schemaProvider to set
		 */
		public Builder setSchemaProvider(SchemaProvider schemaProvider) {
			this.schemaProvider = schemaProvider;
			return this;
		}

		/**
		 * @param indexDescription the indexDescription to set
		 */
		public Builder setIndexDescription(IndexDescription indexDescription) {
			this.indexDescription = indexDescription;
			return this;
		}

		/**
		 * @param userId the userId to set
		 */
		public Builder setUserId(Long userId) {
			this.userId = userId;
			return this;
		}

		/**
		 * @param maxBytesPerPage the maxBytesPerPage to set
		 */
		public Builder setMaxBytesPerPage(Long maxBytesPerPage) {
			this.maxBytesPerPage = maxBytesPerPage;
			return this;
		}

		/**
		 * @param query the query to set
		 */
		public Builder setQuery(Query query) {
			this.query = query;
			return this;
		}

		/**
		 * @param options the options to set
		 */
		public Builder setOptions(QueryOptions options) {
			this.options = options;
			return this;
		}

		/**
		 * @param maxRowsPerCall the maxRowsPerCall to set
		 */
		public Builder setMaxRowsPerCall(Long maxRowsPerCall) {
			this.maxRowsPerCall = maxRowsPerCall;
			return this;
		}

		public Queries build() {
			return new Queries(startingSql, schemaProvider, indexDescription, userId, maxBytesPerPage, query, options,
					maxRowsPerCall);
		}

	}

}
