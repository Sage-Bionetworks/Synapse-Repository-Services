package org.sagebionetworks.table.cluster;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.FacetRequestColumnModel;
import org.sagebionetworks.table.query.util.FacetUtils;
import org.sagebionetworks.table.query.util.SqlElementUtils;

/**
 * An immutable class to create the combined SQL using each type of optional
 * query override including: Pagination, Sorting, Selected Facets, and any
 * Additional filters. Note: This class does not translate the resulting SQL.
 *
 */
public class CombinedQuery {

	private final TableAndColumnMapper tableAndColumnMapper;
	private final String combinedSql;
	private final Pagination originalPagination;

	private CombinedQuery(String query, SchemaProvider schemaProvider, Long overrideOffset, Long overrideLimit,
			List<SortItem> sortList, List<FacetColumnRequest> selectedFacets, List<QueryFilter> additionalFilters) {
		super();
		try {
			QuerySpecification querySpecification = new TableQueryParser(query).querySpecification();
			
			originalPagination = querySpecification.getFirstElementOfType(Pagination.class);
			
			this.tableAndColumnMapper = new TableAndColumnMapper(querySpecification, schemaProvider);

			// Append additionalFilters onto the WHERE clause
			if (additionalFilters != null && !additionalFilters.isEmpty()) {
				String additionalFilterSearchCondition = SQLTranslatorUtils.translateQueryFilters(additionalFilters);
				StringBuilder whereClauseBuilder = new StringBuilder();
				SqlElementUtils.appendCombinedWhereClauseToStringBuilder(whereClauseBuilder,
						additionalFilterSearchCondition, querySpecification.getTableExpression().getWhereClause());
				querySpecification.getTableExpression()
						.replaceWhere(new TableQueryParser(whereClauseBuilder.toString()).whereClause());
			}

			// facet selection
			if (selectedFacets != null) {
				List<FacetRequestColumnModel> facetRequestModels = new ArrayList<>();
				for (FacetColumnRequest facetRequest : selectedFacets) {
					ColumnTranslationReference ref = tableAndColumnMapper
							.lookupColumnReference(facetRequest.getColumnName())
							.orElseThrow(() -> new IllegalArgumentException(
									String.format("Facet selection: '%s' does not match any column name of the schema",
											facetRequest.getColumnName())));
					facetRequestModels
							.add(new FacetRequestColumnModel(
									new ColumnModel().setColumnType(ref.getColumnType())
											.setFacetType(ref.getFacetType()).setName(facetRequest.getColumnName()),
									facetRequest));
				}

				querySpecification.getTableExpression()
						.replaceWhere(FacetUtils.appendFacetSearchConditionToQuerySpecification(
								querySpecification.getTableExpression().getWhereClause(), facetRequestModels));
			}

			// order by
			querySpecification.getTableExpression().replaceOrderBy(SqlElementUtils
					.convertToSortedQuery(querySpecification.getTableExpression().getOrderByClause(), sortList));
			
			// pagination
			querySpecification.getTableExpression().replacePagination(
					SqlElementUtils.overridePagination(querySpecification.getTableExpression().getPagination(),
							overrideOffset, overrideLimit));

			this.combinedSql = querySpecification.toSql();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * @return the tableAndColumnMapper
	 */
	public TableAndColumnMapper getTableAndColumnMapper() {
		return tableAndColumnMapper;
	}

	/**
	 * @return the combinedSql
	 */
	public String getCombinedSql() {
		return combinedSql;
	}
	
	/**
	 * The original Pagination (before overrides were applied).
	 * @return the originalPagination
	 */
	public Pagination getOriginalPagination() {
		return originalPagination;
	}


	public static class Builder {
		private String query;
		private SchemaProvider schemaProvider;
		private Long overrideOffset;
		private Long overrideLimit;
		private List<SortItem> sortList;
		private List<FacetColumnRequest> selectedFacets;
		private List<QueryFilter> additionalFilters;
		/**
		 * @param query the query to set
		 */
		public Builder setQuery(String query) {
			this.query = query;
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
		 * @param overrideOffset the overrideOffset to set
		 */
		public Builder setOverrideOffset(Long overrideOffset) {
			this.overrideOffset = overrideOffset;
			return this;
		}
		/**
		 * @param overrideLimit the overrideLimit to set
		 */
		public Builder setOverrideLimit(Long overrideLimit) {
			this.overrideLimit = overrideLimit;
			return this;
		}
		/**
		 * @param sortList the sortList to set
		 */
		public Builder setSortList(List<SortItem> sortList) {
			this.sortList = sortList;
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
		
		public CombinedQuery build() {
			return new CombinedQuery(query, schemaProvider, overrideOffset, overrideLimit, sortList, selectedFacets, additionalFilters);
		}
	}

}
