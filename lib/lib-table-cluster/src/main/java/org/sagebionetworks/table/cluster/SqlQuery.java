package org.sagebionetworks.table.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.model.TextMatchesPredicate;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Represents a SQL query for a table.
 * 
 * @author John
 *
 */
public class SqlQuery {

	/**
	 * The input SQL is parsed into this object model.
	 *
	 */
	private final QueryExpression model;

	private final QueryExpression transformedModel;

	private final SchemaProvider schemaProvider;

	/**
	 * This map will contain all of the bind variable values for the translated
	 * query.
	 */
	private final Map<String, Object> parameters;

	/**
	 * The translated SQL.
	 */
	private final String outputSQL;

	/**
	 * The maximum size of each query result row returned by this query.
	 */
	private final int maxRowSizeBytes;

	/**
	 * The maximum number of rows per page for the given query
	 */
	private Long maxRowsPerPage;

	/**
	 * Does this query include ROW_ID and ROW_VERSION?
	 */
	private final boolean includesRowIdAndVersion;

	/**
	 * Should the query results include the row's etag? Note: This is true for view
	 * queries.
	 */
	private final boolean includeEntityEtag;

	/**
	 * Aggregated results are queries that included one or more aggregation
	 * functions in the select clause. These query results will not match columns in
	 * the table. In addition rowIDs and rowVersionNumbers will be null when
	 * isAggregatedResults = true.
	 */
	private final boolean isAggregatedResult;

	/**
	 * The list of all columns referenced in the select column.
	 */
	private final List<SelectColumn> selectColumns;

	private final Long overrideOffset;
	private final Long overrideLimit;
	private final Long maxBytesPerPage;
	private final Long userId;

	private final List<FacetColumnRequest> selectedFacets;

	private final boolean isIncludeSearch;

	private final List<ColumnModel> schemaOfSelect;

	private final IndexDescription indexDescription;

	private final SqlContext sqlContext;
	private final List<IdAndVersion> allTableIds;

	/**
	 * The combined sql is, basic input sql combined with requested filters.
	 */
	private final String combinedSQL;

	/**
	 * The union of all ColumnModel from any part of this query.
	 */
	private final List<ColumnModel> allColumnModels;

	/**
	 * @param tableId
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	SqlQuery(QueryExpression parsedModel, SchemaProvider schemaProvider, Long overrideOffset, Long overrideLimit,
			Long maxBytesPerPage, List<SortItem> sortList, Boolean includeEntityEtag,
			List<FacetColumnRequest> selectedFacets, List<QueryFilter> additionalFilters, Long userId,
			IndexDescription indexDescription, SqlContext sqlContextIn) {
		try {

			ValidateArgument.required(schemaProvider, "schemaProvider");
			ValidateArgument.required(indexDescription, "indexDescription");
			this.model = parsedModel;
			this.schemaProvider = schemaProvider;
			if (sqlContextIn == null) {
				this.sqlContext = SqlContext.query;
			} else {
				this.sqlContext = sqlContextIn;
			}
			this.model.setSqlContext(this.sqlContext);
			this.maxBytesPerPage = maxBytesPerPage;
			this.selectedFacets = selectedFacets;
			this.overrideLimit = overrideLimit;
			this.overrideOffset = overrideOffset;
			this.userId = userId;
			this.indexDescription = indexDescription;
			// A copy of the original model will be transformed.
			this.transformedModel = new TableQueryParser(model.toSql()).queryExpression();

			// only a view can include the etag
			if (indexDescription.getTableType().isViewEntityType() && includeEntityEtag != null) {
				this.includeEntityEtag = includeEntityEtag;
			} else {
				this.includeEntityEtag = false;
			}

			Set<IdAndVersion> setIdsVersion = new LinkedHashSet<>();
			Set<ColumnModel> columModelUnionSet = new LinkedHashSet<ColumnModel>();
			QuerySpecificationDetails firstDetails = null;
			QuerySpecificationDetails lastDetails = null;
			List<QuerySpecificationDetails> allDetails = new ArrayList<>();
			for (QuerySpecification qs : transformedModel.createIterable(QuerySpecification.class)) {
				QuerySpecificationDetails details = new QuerySpecificationDetails(qs, schemaProvider, sqlContextIn);
				allDetails.add(details);
				if (firstDetails == null) {
					firstDetails = details;
				}
				lastDetails = details;
				setIdsVersion.addAll(details.getTableAndColumnMapper().getTableIds());
				columModelUnionSet.addAll(details.getTableAndColumnMapper().getUnionOfAllTableSchemas());
			}

			this.allTableIds = setIdsVersion.stream().collect(Collectors.toList());
			this.allColumnModels = columModelUnionSet.stream().collect(Collectors.toList());
			this.schemaOfSelect = firstDetails.getSchemaOfSelect();

			lastDetails.getQuerySpecification().getTableExpression()
					.replaceOrderBy(SqlElementUtils.convertToSortedQuery(
							lastDetails.getQuerySpecification().getTableExpression().getOrderByClause(), sortList));

			// This map will contain all of the
			this.parameters = new HashMap<String, Object>();

			// Append additionalFilters onto the WHERE clause
			if (additionalFilters != null && !additionalFilters.isEmpty()) {
				String additionalFilterSearchCondition = SQLTranslatorUtils.translateQueryFilters(additionalFilters);
				StringBuilder whereClauseBuilder = new StringBuilder();
				SqlElementUtils.appendCombinedWhereClauseToStringBuilder(whereClauseBuilder,
						additionalFilterSearchCondition,
						lastDetails.getQuerySpecification().getTableExpression().getWhereClause());
				lastDetails.getQuerySpecification().getTableExpression()
						.replaceWhere(new TableQueryParser(whereClauseBuilder.toString()).whereClause());
			}

			// Track if this is an aggregate query.
			this.isAggregatedResult = model.hasAnyAggregateElements();
			this.includesRowIdAndVersion = !this.isAggregatedResult;
			// Build headers that describe how the client should read the results of this
			// query.
			this.selectColumns = firstDetails.getSelectList();
			// Maximum row size is a function of both the select clause and schema.
			this.maxRowSizeBytes = firstDetails.getMaxRowSizeBytes();
			if (maxBytesPerPage != null) {
				this.maxRowsPerPage = Math.max(1, maxBytesPerPage / this.maxRowSizeBytes);
			}
			// Does the query contain any text_matches elements?
			this.isIncludeSearch = model.getFirstElementOfType(TextMatchesPredicate.class) != null;

			// paginated model includes all overrides and max rows per page.
			lastDetails.getQuerySpecification().getTableExpression()
					.replacePagination(SqlElementUtils.overridePagination(
							lastDetails.getQuerySpecification().getTableExpression().getPagination(), overrideOffset,
							overrideLimit, maxRowsPerPage));

			for (QuerySpecificationDetails details : allDetails) {
				SQLTranslatorUtils.addMetadataColumnsToSelect(details.getQuerySpecification().getSelectList(),
						indexDescription.getColumnNamesToAddToSelect(sqlContext, this.includeEntityEtag,
								details.getQuerySpecification().hasAnyAggregateElements()));
			}

			this.combinedSQL = transformedModel.toSql();

			for (QuerySpecificationDetails details : allDetails) {
				SQLTranslatorUtils.translateModel(details.getQuerySpecification(), parameters, userId,
						details.getTableAndColumnMapper());
			}

			this.outputSQL = transformedModel.toSql();

		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Does this query include ROW_ID and ROW_VERSION
	 * 
	 * @return
	 */
	public boolean includesRowIdAndVersion() {
		return this.includesRowIdAndVersion;
	}

	/**
	 * Does this query include ROW_ETAG
	 * 
	 * @return
	 */
	public boolean includeEntityEtag() {
		return this.includeEntityEtag;
	}

	/**
	 * The input SQL was parsed into this model object.
	 * 
	 * @return
	 */
	public QueryExpression getModel() {
		return model;
	}

	/**
	 * This map contains the values of all bind variables referenced in the
	 * translated output SQL.
	 * 
	 * @return
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}

	/**
	 * The translated output SQL.
	 * 
	 * @return
	 */
	public String getOutputSQL() {
		return outputSQL;
	}

	/**
	 * Aggregated results are queries that included one or more aggregation
	 * functions in the select clause. These query results will not match columns in
	 * the table. In addition rowIDs and rowVersionNumbers will be null when
	 * isAggregatedResults = true.
	 * 
	 * @return
	 */
	public boolean isAggregatedResult() {
		return isAggregatedResult;
	}

	/**
	 * Get the single TableId from the query. Note: If the SQL includes a JOIN, this
	 * will return an Optional.empty();
	 * 
	 * @return
	 */
	public Optional<String> getSingleTableId() {
		if (allTableIds.size() == 1) {
			return Optional.of(allTableIds.get(0).toString());
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Get the IdAndVersion for each table referenced in the from clause.
	 * 
	 * @return
	 */
	public List<IdAndVersion> getTableIds() {
		return allTableIds;
	}

	/**
	 * The list of column models from the select clause.
	 * 
	 * @return
	 */
	public List<SelectColumn> getSelectColumns() {
		return selectColumns;
	}

	/**
	 * All of the Columns of the table.
	 * 
	 * @return
	 */
	public List<ColumnModel> getTableSchema() {
		return allColumnModels;
	}

	/**
	 * Get a ColumnModel representation of each column from the SQL's select
	 * statement. Note: When a result ColumnModel does not match any of the columns
	 * from the source table/view, the {@link ColumnModel#getId()} will be null.
	 * 
	 * @return
	 */
	public List<ColumnModel> getSchemaOfSelect() {
		return this.schemaOfSelect;
	}

	/**
	 * The maximum size of each query result row returned by this query.
	 * 
	 * @return
	 */
	public int getMaxRowSizeBytes() {
		return maxRowSizeBytes;
	}

	/**
	 * The query model that has been transformed to execute against the actual table
	 * index.
	 * 
	 * @return
	 */
	public QueryExpression getTransformedModel() {
		return transformedModel;
	}

	/**
	 * 
	 * @return
	 */
	public Long getMaxRowsPerPage() {
		return maxRowsPerPage;
	}

	/**
	 * Get the selected facets
	 * 
	 * @return
	 */
	public List<FacetColumnRequest> getSelectedFacets() {
		return this.selectedFacets;
	}

	/**
	 * The type of table.
	 * 
	 * @return
	 */
	public TableType getTableType() {
		return this.indexDescription.getTableType();
	}

	public boolean isIncludesRowIdAndVersion() {
		return includesRowIdAndVersion;
	}

	public Long getOverrideOffset() {
		return overrideOffset;
	}

	public Long getOverrideLimit() {
		return overrideLimit;
	}

	public Long getMaxBytesPerPage() {
		return maxBytesPerPage;
	}

	public Long getUserId() {
		return userId;
	}

	public boolean isIncludeSearch() {
		return isIncludeSearch;
	}

	public SchemaProvider getSchemaProvider() {
		return schemaProvider;
	}

	public IndexDescription getIndexDescription() {
		return this.indexDescription;
	}

	public SqlContext getSqlContext() {
		return this.sqlContext;
	}

	/**
	 * Get the combined sql
	 * 
	 * @return
	 */
	public String getCombinedSQL() {
		return this.combinedSQL;
	}

}
