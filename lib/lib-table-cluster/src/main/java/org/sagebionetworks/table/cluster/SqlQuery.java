package org.sagebionetworks.table.cluster;

import org.apache.commons.lang3.BooleanUtils;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReferenceLookup;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.ValidateArgument;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	QuerySpecification model;

	/**
	 * The model transformed to execute against the actual table.
	 */
	QuerySpecification transformedModel;

	/**
	 * The full list of all of the columns of this table
	 */
	List<ColumnModel> tableSchema;

	/**
	 * This map will contain all of the bind variable values for the translated query.
	 */
	Map<String, Object> parameters;

	/**
	 * The map of column names to column models.
	 */
	LinkedHashMap<String, ColumnModel> columnNameToModelMap;

	/**
	 * The translated SQL.
	 */
	String outputSQL;

	/**
	 * The Id of the table.
	 */
	String tableId;

	/**
	 * The maximum size of each query result row returned by this query.
	 */
	int maxRowSizeBytes;

	/**
	 * The maximum number of rows per page for the given query
	 */
	Long maxRowsPerPage;

	/**
	 * Does this query include ROW_ID and ROW_VERSION?
	 */
	boolean includesRowIdAndVersion;

	/**
	 * Should the query results include the row's etag?
	 * Note: This is true for view queries.
	 */
	boolean includeEntityEtag;

	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 */
	boolean isAggregatedResult;

	/**
	 * The list of all columns referenced in the select column.
	 */
	List<SelectColumn> selectColumns;

	Long overrideOffset;
	Long overrideLimit;
	Long maxBytesPerPage;
	Long userId;

	List<FacetColumnRequest> selectedFacets;

	EntityType tableType;

	/**
	 * @param tableId
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	SqlQuery(
			QuerySpecification parsedModel,
			List<ColumnModel> tableSchema,
			Long overrideOffset,
			Long overrideLimit,
			Long maxBytesPerPage,
			List<SortItem> sortList,
			Boolean includeEntityEtag,
			EntityType tableType,
			List<FacetColumnRequest> selectedFacets,
			List<QueryFilter> additionalFilters,
			Long userId
			) {
		ValidateArgument.required(tableSchema, "TableSchema");
		if(tableSchema.isEmpty()){
			throw new IllegalArgumentException("Table schema cannot be empty");
		}
		this.tableSchema = tableSchema;
		this.model = parsedModel;
		this.tableId = parsedModel.getTableName();
		this.maxBytesPerPage = maxBytesPerPage;
		this.selectedFacets = selectedFacets;
		this.overrideLimit = overrideLimit;
		this.overrideOffset = overrideOffset;
		this.userId = userId;

		if(tableType == null){
			// default to table
			this.tableType = EntityType.table;
		}else{
			this.tableType = tableType;
		}

		// only a view can include the etag
		if(EntityTypeUtils.isViewType(this.tableType) && includeEntityEtag != null){
			this.includeEntityEtag = includeEntityEtag;
		}else{
			this.includeEntityEtag = false;
		}

		if (sortList != null && !sortList.isEmpty()) {
			// change the query to use the sort list
			try {
				model = SqlElementUntils.convertToSortedQuery(model, sortList);
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		}

		// This map will contain all of the 
		this.parameters = new HashMap<String, Object>();


		this.columnNameToModelMap = TableModelUtils.createColumnNameToModelMap(tableSchema);
		ColumnTranslationReferenceLookup columnTranslationReferenceLookup = new ColumnTranslationReferenceLookup(tableSchema);

		// SELECT * is replaced with a select including each column in the schema.
		if (BooleanUtils.isTrue(this.model.getSelectList().getAsterisk())) {
			SelectList expandedSelectList = SQLTranslatorUtils.createSelectListFromSchema(tableSchema);
			this.model.replaceSelectList(expandedSelectList);
		}

		//Append additionalFilters onto the WHERE clause
		if(additionalFilters != null && !additionalFilters.isEmpty()) {
			String additionalFilterSearchCondition = SQLTranslatorUtils.translateQueryFilters(additionalFilters);
			StringBuilder whereClauseBuilder = new StringBuilder();
			SqlElementUntils.appendCombinedWhereClauseToStringBuilder(whereClauseBuilder,additionalFilterSearchCondition, this.model.getTableExpression().getWhereClause());
			try {
				this.model.getTableExpression().replaceWhere(new TableQueryParser(whereClauseBuilder.toString()).whereClause());
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		}

		// Track if this is an aggregate query.
		this.isAggregatedResult = model.hasAnyAggregateElements();
		// Build headers that describe how the client should read the results of this query.
		this.selectColumns = SQLTranslatorUtils.getSelectColumns(this.model.getSelectList(), columnTranslationReferenceLookup, this.isAggregatedResult);
		// Maximum row size is a function of both the select clause and schema.
		this.maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(selectColumns, columnNameToModelMap);
		if(maxBytesPerPage != null){
			this.maxRowsPerPage =  Math.max(1, maxBytesPerPage / this.maxRowSizeBytes);
		}
		// paginated model includes all overrides and max rows per page.
		QuerySpecification paginatedModel = SqlElementUntils.overridePagination(model, overrideOffset, overrideLimit, maxRowsPerPage);

		// Create a copy of the paginated model.
		try {
			transformedModel = new TableQueryParser(paginatedModel.toSql()).querySpecification();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		if (!this.isAggregatedResult) {
			// we need to add the row count and row version columns
			SelectList expandedSelectList = SQLTranslatorUtils.addMetadataColumnsToSelect(this.transformedModel.getSelectList(), this.includeEntityEtag);
			transformedModel.replaceSelectList(expandedSelectList);
			this.includesRowIdAndVersion = true;
		}else{
			this.includesRowIdAndVersion = false;
		}

		SQLTranslatorUtils.translateModel(transformedModel, parameters, columnTranslationReferenceLookup, userId);
		this.outputSQL = transformedModel.toSql();
	}
	
	/**
	 * Does this query include ROW_ID and ROW_VERSION
	 * 
	 * @return
	 */
	public boolean includesRowIdAndVersion(){
		return this.includesRowIdAndVersion;
	}
	
	/**
	 * Does this query include ROW_ETAG
	 * @return
	 */
	public boolean includeEntityEtag(){
		return this.includeEntityEtag;
	}

	/**
	 * The input SQL was parsed into this model object.
	 * 
	 * @return
	 */
	public QuerySpecification getModel() {
		return model;
	}


	/**
	 * This map contains the values of all bind variables referenced in the translated output SQL.
	 * @return
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}


	/**
	 * The column name to column ID mapping.
	 * @return
	 */
	public Map<String, ColumnModel> getColumnNameToModelMap() {
		return columnNameToModelMap;
	}


	/**
	 * The translated output SQL.
	 * @return
	 */
	public String getOutputSQL() {
		return outputSQL;
	}

	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 * @return
	 */
	public boolean isAggregatedResult() {
		return isAggregatedResult;
	}

	/**
	 * The ID of the table.
	 * @return
	 */
	public String getTableId() {
		return tableId;
	}

	/**
	 * The list of column models from the select clause.
	 * @return
	 */
	public List<SelectColumn> getSelectColumns() {
		return selectColumns;
	}

	/**
	 * All of the Columns of the table.
	 * @return
	 */
	public List<ColumnModel> getTableSchema() {
		return tableSchema;
	}

	/**
	 * The maximum size of each query result row returned by this query.
	 * @return
	 */
	public int getMaxRowSizeBytes() {
		return maxRowSizeBytes;
	}

	/**
	 * The query model that has been transformed to execute against the actual table index.
	 * @return
	 */
	public QuerySpecification getTransformedModel() {
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
	 * @return
	 */
	public List<FacetColumnRequest> getSelectedFacets(){
		return this.selectedFacets;
	}
	
	/**
	 * The type of table.
	 * @return
	 */
	public EntityType getTableType(){
		return this.tableType;
	}

	public boolean isIncludesRowIdAndVersion() {
		return includesRowIdAndVersion;
	}

	public boolean isIncludeEntityEtag() {
		return includeEntityEtag;
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
}
