package org.sagebionetworks.table.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Provides the translation from the user provided SQL query to the SQL that
 * will be executed against the actual index.
 */
public class QueryTranslator {

	/**
	 * The input SQL is parsed into this object model.
	 *
	 */
	private final String inputSql;

	/**
	 * The model transformed to execute against the actual table.
	 */
	private final QuerySpecification transformedModel;

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

	private final boolean isIncludeSearch;

	private final TableAndColumnMapper tableAndColumnMapper;
	private final List<ColumnModel> schemaOfSelect;

	private final IndexDescription indexDescription;

	private final SqlContext sqlContext;

	/**
	 * @param tableId
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	QueryTranslator(String startingSql, SchemaProvider schemaProvider, Long maxBytesPerPage,
			Boolean includeEntityEtag, Long userId, IndexDescription indexDescription, SqlContext sqlContextIn) {
		ValidateArgument.required(schemaProvider, "schemaProvider");
		ValidateArgument.required(indexDescription, "indexDescription");
		this.inputSql = startingSql;
		try {
			QuerySpecification model = new TableQueryParser(startingSql).querySpecification();
			if (sqlContextIn == null) {
				this.sqlContext = SqlContext.query;
			} else {
				this.sqlContext = sqlContextIn;
			}
			this.tableAndColumnMapper = new TableAndColumnMapper(model, schemaProvider);
			if (this.tableAndColumnMapper.getTableIds().size() > 1 && !SqlContext.build.equals(this.sqlContext)) {
				throw new IllegalArgumentException(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE);
			}
			this.indexDescription = indexDescription;

			// only a view can include the etag
			if (indexDescription.getTableType().isViewEntityType() && includeEntityEtag != null) {
				this.includeEntityEtag = includeEntityEtag;
			} else {
				this.includeEntityEtag = false;
			}
			// This map will contain all of the
			this.parameters = new HashMap<String, Object>();

			List<ColumnModel> unionOfSchemas = tableAndColumnMapper.getUnionOfAllTableSchemas();

			// SELECT * is replaced with a select including each column in the schema.
			if (BooleanUtils.isTrue(model.getSelectList().getAsterisk())) {
				SelectList expandedSelectList = tableAndColumnMapper.buildSelectAllColumns();
				model.getSelectList().replaceElement(expandedSelectList);
			}

			this.schemaOfSelect = SQLTranslatorUtils.getSchemaOfSelect(model.getSelectList(), tableAndColumnMapper);

			// Track if this is an aggregate query.
			this.isAggregatedResult = model.hasAnyAggregateElements();
			this.includesRowIdAndVersion = !this.isAggregatedResult;
			// Build headers that describe how the client should read the results of this
			// query.
			this.selectColumns = SQLTranslatorUtils.getSelectColumns(model.getSelectList(), tableAndColumnMapper,
					this.isAggregatedResult);
			// Maximum row size is a function of both the select clause and schema.
			this.maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(selectColumns, TableModelUtils.createColumnNameToModelMap(unionOfSchemas));

			if (maxBytesPerPage != null) {
				this.maxRowsPerPage = Math.max(1, maxBytesPerPage / this.maxRowSizeBytes);
				model.getTableExpression().replacePagination(
						SqlElementUtils.limitMaxRowsPerPage(model.getTableExpression().getPagination(), maxRowsPerPage));
			}

			// Does the query contain any text_matches elements?
			this.isIncludeSearch = model.isIncludeSearch();

			transformedModel = new TableQueryParser(model.toSql()).querySpecification();
			transformedModel.setSqlContext(this.sqlContext);
			
			SQLTranslatorUtils.addMetadataColumnsToSelect(this.transformedModel.getSelectList(), indexDescription
					.getColumnNamesToAddToSelect(sqlContext, this.includeEntityEtag, this.isAggregatedResult));


			SQLTranslatorUtils.translateModel(transformedModel, parameters, userId, tableAndColumnMapper);
			this.outputSQL = transformedModel.toSql();
		}catch (ParseException e) {
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
	 * Get the original input SQL (prior to translation). 
	 * @return
	 */
	public String getInputSql() {
		return inputSql;
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
	boolean isAggregatedResult() {
		return isAggregatedResult;
	}

	/**
	 * Get the single TableId from the query. Note: If the SQL includes a JOIN, this
	 * will return an Optional.empty();
	 * 
	 * @return
	 */
	public Optional<String> getSingleTableId() {
		List<IdAndVersion> tableIds = tableAndColumnMapper.getTableIds();
		if (tableIds.size() == 1) {
			return Optional.of(tableIds.get(0).toString());
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
		return tableAndColumnMapper.getTableIds();
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
		return tableAndColumnMapper.getUnionOfAllTableSchemas();
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
	int getMaxRowSizeBytes() {
		return maxRowSizeBytes;
	}

	/**
	 * The query model that has been transformed to execute against the actual table
	 * index.
	 * 
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
	 * The type of table.
	 * 
	 * @return
	 */
	TableType getTableType() {
		return this.indexDescription.getTableType();
	}

	public boolean isIncludeSearch() {
		return isIncludeSearch;
	}

	public IndexDescription getIndexDescription() {
		return this.indexDescription;
	}

	SqlContext getSqlContext() {
		return this.sqlContext;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static Builder builder(String sql, Long userId) {
		return new Builder().sql(sql).userId(userId);
	}
	
	public static Builder builder(String sql, SchemaProvider schemaProvider, Long userId) {
		return new Builder().sql(sql).userId(userId).schemaProvider(schemaProvider);
	}
	
	public static Builder builder(String sql, TranslationDependencies dependencies) {
		return new Builder().sql(sql).userId(dependencies.getUserId()).schemaProvider(dependencies.getSchemaProvider())
				.indexDescription(dependencies.getIndexDescription());
	}
	
	public static Builder builder(QuerySpecification model) {
		return new Builder().sql(model.toSql());
	}
	
	public static Builder builder(QuerySpecification model, Long userId) {
		return new Builder().sql(model.toSql()).userId(userId);
	}
	
	public static Builder builder(QuerySpecification model, SchemaProvider schemaProvider, Long maxBytesPerPage, Long userId) {
		return new Builder().sql(model.toSql()).schemaProvider(schemaProvider).maxBytesPerPage(maxBytesPerPage).userId(userId);
	}
	
	public static class Builder {
		
		private String sql;
		private SchemaProvider schemaProvider;
		private Long maxBytesPerPage;
		private Boolean includeEntityEtag;
		private Long userId;
		private IndexDescription indexDescription;
		private SqlContext sqlContext;
		
		
		public Builder sql(String sql) {
			this.sql = sql;
			return this;
		}
		
		public Builder userId(Long userId) {
			this.userId = userId;
			return this;
		}

		public Builder schemaProvider(SchemaProvider schemaProvider) {
			this.schemaProvider = schemaProvider;
			return this;
		}
		
		public Builder maxBytesPerPage(Long maxBytesPerPage) {
			this.maxBytesPerPage = maxBytesPerPage;
			return this;
		}
		
		
		public Builder includeEntityEtag(Boolean includeEntityEtag) {
			this.includeEntityEtag = includeEntityEtag;
			return this;
		}
		
		
		public Builder sqlContext(SqlContext sqlContext) {
			this.sqlContext = sqlContext;
			return this;
		}
		
		
		public Builder indexDescription(IndexDescription indexDescription) {
			this.indexDescription = indexDescription;
			return this;
		}

		public QueryTranslator build() {
			return new QueryTranslator(sql, schemaProvider, maxBytesPerPage, includeEntityEtag, userId, indexDescription,
					sqlContext);
		}

	}

}
