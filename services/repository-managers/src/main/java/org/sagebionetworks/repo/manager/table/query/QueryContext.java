package org.sagebionetworks.repo.manager.table.query;

import java.util.List;

import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An immutable wrapper of a query plus all of its optional parts. This also
 * wraps table metadata providers.
 *
 */
public class QueryContext {

	public static final int MAX_SIZE_ADDITIONAL_FILTERS = 50;

	private final String startingSql;
	private final SchemaProvider schemaProvider;
	private final IndexDescription indexDescription;
	private final Long userId;
	private final Long maxBytesPerPage;
	private final Long maxRowsPerCall;
	private final List<QueryFilter> additionalFilters;
	private final List<FacetColumnRequest> selectedFacets;
	private final Long selectFileColumn;
	private final Boolean includeEntityEtag;
	private final Long offset;
	private final Long limit;
	private final List<SortItem> sort;

	public QueryContext(String startingSql, SchemaProvider schemaProvider, IndexDescription indexDescription,
			Long userId, Long maxBytesPerPage, Long maxRowsPerCall, List<QueryFilter> additionalFilters,
			List<FacetColumnRequest> selectedFacets, Long selectFileColumn, Boolean includeEntityEtag, Long offset,
			Long limit, List<SortItem> sort) {

		ValidateArgument.required(startingSql, "startingSql");
		ValidateArgument.required(schemaProvider, "schemaProvider");
		ValidateArgument.required(indexDescription, "indexDescription");
		ValidateArgument.required(userId, "userId");
		if (additionalFilters != null) {
			if (additionalFilters.size() > MAX_SIZE_ADDITIONAL_FILTERS) {
				throw new IllegalArgumentException(String.format(
						"The size of the provided additionalFilters is %d which exceeds the maximum of %d",
						additionalFilters.size(), MAX_SIZE_ADDITIONAL_FILTERS));
			}
		}

		this.startingSql = startingSql;
		this.schemaProvider = new CachingSchemaProvider(schemaProvider);
		this.indexDescription = indexDescription;
		this.userId = userId;
		this.maxBytesPerPage = maxBytesPerPage;
		this.maxRowsPerCall = maxRowsPerCall;
		this.additionalFilters = additionalFilters;
		this.selectedFacets = selectedFacets;
		this.selectFileColumn = selectFileColumn;
		this.includeEntityEtag = includeEntityEtag;
		this.offset = offset;
		this.limit = limit;
		this.sort = sort;
	}

	/**
	 * @return the startingSql
	 */
	public String getStartingSql() {
		return startingSql;
	}

	/**
	 * @return the schemaProvider
	 */
	public SchemaProvider getSchemaProvider() {
		return schemaProvider;
	}

	/**
	 * @return the indexDescription
	 */
	public IndexDescription getIndexDescription() {
		return indexDescription;
	}

	/**
	 * @return the userId
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * @return the maxBytesPerPage
	 */
	public Long getMaxBytesPerPage() {
		return maxBytesPerPage;
	}

	/**
	 * @return the maxRowsPerCall
	 */
	public Long getMaxRowsPerCall() {
		return maxRowsPerCall;
	}

	/**
	 * @return the additionalFilters
	 */
	public List<QueryFilter> getAdditionalFilters() {
		return additionalFilters;
	}

	/**
	 * @return the selectedFacets
	 */
	public List<FacetColumnRequest> getSelectedFacets() {
		return selectedFacets;
	}

	public Long getSelectFileColumn() {
		return selectFileColumn;
	}

	/**
	 * @return the includeEntityEtag
	 */
	public Boolean getIncludeEntityEtag() {
		return includeEntityEtag;
	}

	/**
	 * @return the offset
	 */
	public Long getOffset() {
		return offset;
	}

	/**
	 * @return the limit
	 */
	public Long getLimit() {
		return limit;
	}

	/**
	 * @return the sort
	 */
	public List<SortItem> getSort() {
		return sort;
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
		private Long maxRowsPerCall;
		private List<QueryFilter> additionalFilters;
		private List<FacetColumnRequest> selectedFacets;
		private Long selectFileColumn;
		private Boolean includeEntityEtag;
		private Long offset;
		private Long limit;
		private List<SortItem> sort;

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
		 * @param maxRowsPerCall the maxRowsPerCall to set
		 */
		public Builder setMaxRowsPerCall(Long maxRowsPerCall) {
			this.maxRowsPerCall = maxRowsPerCall;
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
		 * @param selectedFacets the selectedFacets to set
		 */
		public Builder setSelectedFacets(List<FacetColumnRequest> selectedFacets) {
			this.selectedFacets = selectedFacets;
			return this;
		}

		/**
		 * @param selectFileColumn The id of the column to use to select files from the
		 *                         query
		 */
		public Builder setSelectFileColumn(Long selectFileColumn) {
			this.selectFileColumn = selectFileColumn;
			return this;
		}

		/**
		 * @param includeEntityEtag the includeEntityEtag to set
		 */
		public Builder setIncludeEntityEtag(Boolean includeEntityEtag) {
			this.includeEntityEtag = includeEntityEtag;
			return this;
		}

		/**
		 * @param offset the offset to set
		 */
		public Builder setOffset(Long offset) {
			this.offset = offset;
			return this;
		}

		/**
		 * @param limit the limit to set
		 */
		public Builder setLimit(Long limit) {
			this.limit = limit;
			return this;
		}

		/**
		 * @param sort the sort to set
		 */
		public Builder setSort(List<SortItem> sort) {
			this.sort = sort;
			return this;
		}

		public QueryContext build() {
			return new QueryContext(startingSql, schemaProvider, indexDescription, userId, maxBytesPerPage,
					maxRowsPerCall, additionalFilters, selectedFacets, selectFileColumn, includeEntityEtag, offset,
					limit, sort);
		}

	}
}
