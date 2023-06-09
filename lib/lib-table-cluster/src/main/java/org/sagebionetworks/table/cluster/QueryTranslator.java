package org.sagebionetworks.table.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.description.ColumnToAdd;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.model.TextMatchesPredicate;
import org.sagebionetworks.table.query.model.WithListElement;
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
	
	private final QueryExpression translated;

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

	private final List<ColumnModel> schemaOfSelect;

	private final IndexDescription indexDescription;

	private final SqlContext sqlContext;
	
	private final LinkedHashSet<IdAndVersion> distinctTableIds;
	
	private final List<ColumnModel> tableSchema;
	
	private boolean isCommonTableExpression;

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

			if (sqlContextIn == null) {
				this.sqlContext = SqlContext.query;
			} else {
				this.sqlContext = sqlContextIn;
			}
			String preprocessedSql = indexDescription.preprocessQuery(startingSql);
			QueryExpression transformedModel = new TableQueryParser(preprocessedSql).queryExpression();
			transformedModel.setSqlContext(this.sqlContext);
			
			List<QueryPart> parts = transformedModel.stream(QuerySpecification.class).map((q)-> new QueryPart(q, schemaProvider)).collect(Collectors.toList());
			
			distinctTableIds = parts.stream().flatMap(p->p.getMapper().getTableIds().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
			
			QueryPart firstPart = parts.get(0);
			
			tableSchema = firstPart.getMapper().getUnionOfAllTableSchemas();
			
			isCommonTableExpression = transformedModel.getWithListElements().isPresent();
			
			if (!SqlContext.build.equals(this.sqlContext)) {
				int maxParts = isCommonTableExpression ? 2 : 1;
				if (parts.size() > maxParts) {
					throw new IllegalArgumentException(TableConstants.UNION_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE);
				}
				parts.stream().filter(p -> p.getMapper().getTableIds().size() > 1).findFirst().ifPresent((p) -> {
					throw new IllegalArgumentException(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE);
				});
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

			List<ColumnModel> unionOfSchemas = firstPart.getMapper().getUnionOfAllTableSchemas();


			List<List<ColumnModel>> partsSelectSchemas = new ArrayList<>(parts.size());
			for(QueryPart part: parts) {
				List<ColumnModel> schema = SQLTranslatorUtils.getSchemaOfSelect(part.getQuerySpecification().getSelectList(), part.getMapper());
				partsSelectSchemas.add(schema);
			}
			this.schemaOfSelect = SQLTranslatorUtils.createSchemaOfSelect(partsSelectSchemas);
			
			transformedModel.getWithListElements().ifPresent((withListElements)->{
				if(withListElements.size() != 1) {
					throw new IllegalArgumentException("A CTE must have one and only one inner query.");
				}
				WithListElement wle = withListElements.get(0);
				SQLTranslatorUtils.translateWithListElement(wle, schemaProvider);
			});
			
			// Track if this is an aggregate query.
			this.isAggregatedResult = transformedModel.hasAnyAggregateElements();
			this.includesRowIdAndVersion = !this.isAggregatedResult;
			// Build headers that describe how the client should read the results of this
			// query.
			this.selectColumns = SQLTranslatorUtils.getSelectColumns(firstPart.getQuerySpecification().getSelectList(), firstPart.getMapper(),
					this.isAggregatedResult);
			// Maximum row size is a function of both the select clause and schema.
			this.maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(selectColumns, TableModelUtils.createColumnNameToModelMap(unionOfSchemas));

			if (maxBytesPerPage != null) {
				this.maxRowsPerPage = Math.max(1, maxBytesPerPage / this.maxRowSizeBytes);
				firstPart.getQuerySpecification().getTableExpression().replacePagination(
						SqlElementUtils.limitMaxRowsPerPage(firstPart.getQuerySpecification().getTableExpression().getPagination(), maxRowsPerPage));
			}

			// Does the query contain any text_matches elements?
			this.isIncludeSearch = transformedModel.getFirstElementOfType(TextMatchesPredicate.class) != null;

			List<ColumnToAdd> columnsToAddToSelect = indexDescription
					.getColumnNamesToAddToSelect(sqlContext, this.includeEntityEtag, this.isAggregatedResult);
			
			
			parts.forEach(p -> {
				SQLTranslatorUtils.addMetadataColumnsToSelect(p.getQuerySpecification().getSelectList(),
						new HashSet<>(p.getMapper().getTableIds()), columnsToAddToSelect);

				// translate each part
				SQLTranslatorUtils.translateModel(p.getQuerySpecification(), parameters, userId, p.getMapper());
			});
			this.translated = transformedModel;
			this.outputSQL = transformedModel.toSql();
		}catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private static class QueryPart {

		private final QuerySpecification querySpecification;
		private final TableAndColumnMapper mapper;
		
		QueryPart(QuerySpecification querySpecification, SchemaProvider provider) {
			super();
			this.querySpecification = querySpecification;
			this.mapper = new TableAndColumnMapper(querySpecification, provider);
			
			// SELECT * is replaced with a select including each column in the schema.
			if (BooleanUtils.isTrue(querySpecification.getSelectList().getAsterisk())) {
				SelectList expandedSelectList = mapper.buildSelectAllColumns();
				querySpecification.getSelectList().replaceElement(expandedSelectList);
			}
		}
		/**
		 * @return the querySpecification
		 */
		public QuerySpecification getQuerySpecification() {
			return querySpecification;
		}
		/**
		 * @return the mapper
		 */
		public TableAndColumnMapper getMapper() {
			return mapper;
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
	 * Get the single TableId from the query. Note: If the SQL includes a JOIN or UNION, this
	 * will return an Optional.empty();
	 * 
	 * @return
	 */
	public Optional<String> getSingleTableId() {
		int maxParts = isCommonTableExpression ? 2 : 1;
		if (distinctTableIds.size() <= maxParts) {
			return Optional.of(distinctTableIds.iterator().next().toString());
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
		return distinctTableIds.stream().collect(Collectors.toList());
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
		return tableSchema;
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
	 * 
	 * @return
	 */
	public Long getMaxRowsPerPage() {
		return maxRowsPerPage;
	}
	
	public QueryExpression getTranslatedModel() {
		return this.translated;
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
