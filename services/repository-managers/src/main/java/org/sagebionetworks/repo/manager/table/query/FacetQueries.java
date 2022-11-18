package org.sagebionetworks.repo.manager.table.query;

import java.util.List;

import org.sagebionetworks.repo.manager.table.FacetModel;
import org.sagebionetworks.repo.manager.table.FacetTransformer;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.TableExpression;

public class FacetQueries {

	private final List<FacetTransformer> transformers;

	private FacetQueries(List<FacetColumnRequest> selectedFacets, List<QueryFilter> additionalFilters , String originalSql,
			TranslationDependencies dependencies, Boolean returnFacets) {
		try {
			
			CombinedQuery combined = CombinedQuery.builder()
					.setQuery(originalSql)
					.setSchemaProvider(dependencies.getSchemaProvider())
					// Note: The selected facets must be included in the combined SQL.
					.setSelectedFacets(null)
					.setAdditionalFilters(additionalFilters).build();
			
			TableExpression originalQuery = new TableQueryParser(combined.getCombinedSql()).querySpecification().getTableExpression();
			transformers = new FacetModel(selectedFacets, originalQuery, dependencies, returnFacets)
					.getFacetInformationQueries();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public List<FacetTransformer> getFacetInformationQueries() {
		return transformers;
	}

	public static class Builder {
		private List<FacetColumnRequest> selectedFacets;
		private List<QueryFilter> additionalFilters;
		private String originalSql;
		private TranslationDependencies dependencies;
		private Boolean returnFacets = Boolean.FALSE;

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
		 * @param originalSql the originalSql to set
		 */
		public Builder setOriginalSql(String originalSql) {
			this.originalSql = originalSql;
			return this;
		}

		/**
		 * @param dependencies the dependencies to set
		 */
		public Builder setDependencies(TranslationDependencies dependencies) {
			this.dependencies = dependencies;
			return this;
		}

		/**
		 * @param returnFacets the returnFacets to set
		 */
		public Builder setReturnFacets(Boolean returnFacets) {
			this.returnFacets = returnFacets;
			return this;
		}

		public FacetQueries build() {
			return new FacetQueries(selectedFacets, additionalFilters, originalSql, dependencies, returnFacets);
		}

	}

}
