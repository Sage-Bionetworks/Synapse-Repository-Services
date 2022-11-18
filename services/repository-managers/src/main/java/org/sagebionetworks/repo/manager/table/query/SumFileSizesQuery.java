package org.sagebionetworks.repo.manager.table.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class SumFileSizesQuery {


	private final BasicQuery rowIdAndVersionQuery;

	private SumFileSizesQuery(String startingSql, List<FacetColumnRequest> selectedFacets,
			List<QueryFilter> additionalFilters, Long maxRowsPerCall, TranslationDependencies dependencies) {

		if (TableType.entityview.equals(dependencies.getIndexDescription().getTableType())
				|| TableType.dataset.equals(dependencies.getIndexDescription().getTableType())) {
			try {
				CombinedQuery combined = CombinedQuery.builder().setQuery(startingSql)
						.setAdditionalFilters(additionalFilters).setSelectedFacets(selectedFacets)
						.setSchemaProvider(dependencies.getSchemaProvider()).build();
				QuerySpecification model = new TableQueryParser(combined.getCombinedSql()).querySpecification();
				SqlQuery sqlQuery = new SqlQueryBuilder(model.toSql(), dependencies).build();
				// first get the rowId and rowVersions for the given query up to the limit + 1.
				rowIdAndVersionQuery = SqlElementUtils.buildSqlSelectRowIdAndVersions(sqlQuery.getTransformedModel(), maxRowsPerCall + 1L)
						.map((sql)->{
							return new BasicQuery(sql, sqlQuery.getParameters());
						}).orElse(null);
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		} else {
			rowIdAndVersionQuery = null;
		}

	}

	public Optional<BasicQuery> getRowIdAndVersionQuery() {
		return Optional.ofNullable(rowIdAndVersionQuery);
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private String startingSql;
		private List<FacetColumnRequest> selectedFacets;
		private List<QueryFilter> additionalFilters;
		private Long maxRowsPerCall;
		private TranslationDependencies dependencies;
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
		 * @param maxRowsPerCall the maxRowsPerCall to set
		 */
		public Builder setMaxRowsPerCall(Long maxRowsPerCall) {
			this.maxRowsPerCall = maxRowsPerCall;
			return this;
		}
		/**
		 * @param dependencies the dependencies to set
		 */
		public Builder setDependencies(TranslationDependencies dependencies) {
			this.dependencies = dependencies;
			return this;
		}
		
		public SumFileSizesQuery build() {
			return new SumFileSizesQuery(startingSql, selectedFacets, additionalFilters, maxRowsPerCall, dependencies);
		}
		
	}

}
