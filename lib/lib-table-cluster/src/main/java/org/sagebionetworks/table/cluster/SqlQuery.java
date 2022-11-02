package org.sagebionetworks.table.cluster;

import org.apache.commons.lang3.BooleanUtils;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	private QueryExpression model;

	/**
	 * The model transformed to execute against the actual table.
	 */
	private final QuerySpecification transformedModel;
	
	private final SchemaProvider schemaProvider;

	/**
	 * This map will contain all of the bind variable values for the translated query.
	 */
	private final Map<String, Object> parameters;

	/**
	 * The map of column names to column models.
	 */
	private final LinkedHashMap<String, ColumnModel> columnNameToModelMap;

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
	 * Should the query results include the row's etag?
	 * Note: This is true for view queries.
	 */
	private final boolean includeEntityEtag;

	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
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
	
	private final TableAndColumnMapper tableAndColumnMapper;
	private final List<ColumnModel> schemaOfSelect;
	
	private final IndexDescription indexDescription;
	
	private final SqlContext sqlContext;

	/**
	 * The combined sql is, basic input sql combined with requested filters.
	 */
	private final String combinedSQL;

	/**
	 * @param tableId
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	SqlQuery(
			QueryExpression parsedModel,
			SchemaProvider schemaProvider,
			Long overrideOffset,
			Long overrideLimit,
			Long maxBytesPerPage,
			List<SortItem> sortList,
			Boolean includeEntityEtag,
			List<FacetColumnRequest> selectedFacets,
			List<QueryFilter> additionalFilters,
			Long userId,
			IndexDescription indexDescription,
			SqlContext sqlContextIn
			) {
		ValidateArgument.required(schemaProvider, "schemaProvider");
		ValidateArgument.required(indexDescription, "indexDescription");
		this.model = parsedModel;
		this.schemaProvider = schemaProvider;
		if(sqlContextIn == null) {
			this.sqlContext = SqlContext.query;
		}else {
			this.sqlContext = sqlContextIn;
		}
		this.tableAndColumnMapper = new TableAndColumnMapper(model, schemaProvider);
		if(this.tableAndColumnMapper.getTableIds().size() > 1 && !SqlContext.build.equals(this.sqlContext)) {
			throw new IllegalArgumentException(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE);
		}
		this.maxBytesPerPage = maxBytesPerPage;
		this.selectedFacets = selectedFacets;
		this.overrideLimit = overrideLimit;
		this.overrideOffset = overrideOffset;
		this.userId = userId;
		this.indexDescription = indexDescription;
		
		// only a view can include the etag
		if(indexDescription.getTableType().isViewEntityType() && includeEntityEtag != null){
			this.includeEntityEtag = includeEntityEtag;
		}else{
			this.includeEntityEtag = false;
		}

		QuerySpecification lastQuerySpecification = model.getLastElementOfType(QuerySpecification.class);
		lastQuerySpecification.getTableExpression().replaceOrderBy(SqlElementUtils
				.convertToSortedQuery(lastQuerySpecification.getTableExpression().getOrderByClause(), sortList));

		// This map will contain all of the 
		this.parameters = new HashMap<String, Object>();

		List<ColumnModel> unionOfSchemas = tableAndColumnMapper.getUnionOfAllTableSchemas();
		this.columnNameToModelMap = TableModelUtils.createColumnNameToModelMap(unionOfSchemas);
		
		for(SelectList list:  model.createIterable(SelectList.class)) {
			if(BooleanUtils.isTrue(list.getAsterisk())){
				SelectList expandedSelectList = tableAndColumnMapper.buildSelectAllColumns();
				this.model.getSelectList().replaceElement(expandedSelectList);
			}
		}

		// SELECT * is replaced with a select including each column in the schema.
		if (BooleanUtils.isTrue(this.model.getSelectList().getAsterisk())) {

		}

		this.schemaOfSelect = SQLTranslatorUtils.getSchemaOfSelect(
				this.model.getFirstElementOfType(QuerySpecification.class).getSelectList(), tableAndColumnMapper);

		//Append additionalFilters onto the WHERE clause
		if(additionalFilters != null && !additionalFilters.isEmpty()) {
			String additionalFilterSearchCondition = SQLTranslatorUtils.translateQueryFilters(additionalFilters);
			StringBuilder whereClauseBuilder = new StringBuilder();
			SqlElementUtils.appendCombinedWhereClauseToStringBuilder(whereClauseBuilder,additionalFilterSearchCondition, this.model.getTableExpression().getWhereClause());
			try {
				this.model.getTableExpression().replaceWhere(new TableQueryParser(whereClauseBuilder.toString()).whereClause());
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		}

		// Track if this is an aggregate query.
		this.isAggregatedResult = model.hasAnyAggregateElements();
		this.includesRowIdAndVersion = !this.isAggregatedResult;
		// Build headers that describe how the client should read the results of this query.
		this.selectColumns = SQLTranslatorUtils.getSelectColumns(this.model.getSelectList(), tableAndColumnMapper, this.isAggregatedResult);
		// Maximum row size is a function of both the select clause and schema.
		this.maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(selectColumns, columnNameToModelMap);
		if(maxBytesPerPage != null){
			this.maxRowsPerPage =  Math.max(1, maxBytesPerPage / this.maxRowSizeBytes);
		}
		// Does the query contain any text_matches elements?
		this.isIncludeSearch = model.isIncludeSearch();
		// paginated model includes all overrides and max rows per page.
		QuerySpecification paginatedModel = SqlElementUtils.overridePagination(model, overrideOffset, overrideLimit, maxRowsPerPage);

		// Create a copy of the paginated model.
		try {
			transformedModel = new TableQueryParser(paginatedModel.toSql()).querySpecification();
			transformedModel.setSqlContext(this.sqlContext);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		SQLTranslatorUtils.addMetadataColumnsToSelect(this.transformedModel.getSelectList(),
				indexDescription.getColumnNamesToAddToSelect(sqlContext, this.includeEntityEtag, this.isAggregatedResult));

		this.combinedSQL = transformedModel.toSql();

		SQLTranslatorUtils.translateModel(transformedModel, parameters, userId, tableAndColumnMapper);
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
	 * Get the single TableId from the query.
	 * Note: If the SQL includes a JOIN, this will return an Optional.empty();
	 * 
	 * @return
	 */
	public Optional<String> getSingleTableId() {
		List<IdAndVersion> tableIds = tableAndColumnMapper.getTableIds();
		if(tableIds.size() == 1) {
			return Optional.of(tableIds.get(0).toString());
		}else {
			return Optional.empty();
		}
	}
	
	/**
	 * Get the IdAndVersion for each table referenced in the from clause.
	 * @return
	 */
	public List<IdAndVersion> getTableIds(){
		return tableAndColumnMapper.getTableIds();
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
		return tableAndColumnMapper.getUnionOfAllTableSchemas();
	}
	
	/**
	 * Get a ColumnModel representation of each column from the SQL's select statement.
	 * Note: When a result ColumnModel does not match any of the columns from the source
	 * table/view, the {@link ColumnModel#getId()} will be null.
	 * @return
	 */
	public List<ColumnModel> getSchemaOfSelect(){
		return this.schemaOfSelect;
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
	public TableType getTableType(){
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
	 * @return
	 */
	public String getCombinedSQL(){
		return this.combinedSQL;
	}
	
}
