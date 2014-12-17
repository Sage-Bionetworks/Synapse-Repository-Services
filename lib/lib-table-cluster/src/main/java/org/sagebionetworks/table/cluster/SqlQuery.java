package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.visitors.GetTableNameVisitor;
import org.sagebionetworks.table.query.model.visitors.IsAggregateVisitor;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 */
	boolean isAggregatedResult;
	
	/**
	 * The list of all columns referenced in the select column.
	 */
	ColumnMapper selectColumnModels;
	
	
	/**
	 * Create a new SQLQuery from an input SQL string and mapping of the column names to column IDs.
	 * 
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public SqlQuery(String sql, List<ColumnModel> tableSchema) throws ParseException {
		if(sql == null) throw new IllegalArgumentException("The input SQL cannot be null");
		QuerySpecification parsedQuery = TableQueryParser.parserQuery(sql);
		init(parsedQuery, tableSchema, parsedQuery.doVisit(new GetTableNameVisitor()).getTableName());
	}
	
	/**
	 * Create a query with a parsed model.
	 * 
	 * @param model
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public SqlQuery(QuerySpecification model, List<ColumnModel> tableSchema, String tableId) {
		if (model == null)
			throw new IllegalArgumentException("The input model cannot be null");
		init(model, tableSchema, tableId);
	}

	/**
	 * @param tableId
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public void init(QuerySpecification parsedModel, List<ColumnModel> tableSchema, String tableId) {
		ValidateArgument.required(tableSchema, "TableSchema");
		ValidateArgument.required(tableId, "tableId");
		this.tableSchema = tableSchema;
		this.model = parsedModel;
		this.tableId = tableId;

		// This map will contain all of the 
		this.parameters = new HashMap<String, Object>();	
		this.columnNameToModelMap = TableModelUtils.createColumnNameToModelMap(tableSchema);
		if (BooleanUtils.isTrue(this.model.getSelectList().getAsterisk())) {
			SelectList expandedSelectList = new SelectList(Lists.newArrayList(Iterables.transform(tableSchema,
					new Function<ColumnModel, DerivedColumn>() {
						@Override
						public DerivedColumn apply(ColumnModel cm) {
							return SQLTranslatorUtils.createDerivedColumn(cm.getName());
						}
					})));
			this.model = new QuerySpecification(this.model.getSqlDirective(), this.model.getSetQuantifier(), expandedSelectList,
					this.model.getTableExpression());
		}

		IsAggregateVisitor visitor = new IsAggregateVisitor();
		model.doVisit(visitor);
		this.isAggregatedResult = visitor.isAggregate();

		QuerySpecification expandedSelectList = this.model;
		if (!this.isAggregatedResult) {
			// we need to add the row count and row version colums
			SelectList selectList = expandedSelectList.getSelectList();
			List<DerivedColumn> selectColumns = Lists.newArrayListWithCapacity(selectList.getColumns().size() + 2);
			selectColumns.addAll(selectList.getColumns());
			selectColumns.add(SQLTranslatorUtils.createDerivedColumn(ROW_ID));
			selectColumns.add(SQLTranslatorUtils.createDerivedColumn(ROW_VERSION));
			selectList = new SelectList(selectColumns);
			expandedSelectList = new QuerySpecification(expandedSelectList.getSetQuantifier(), selectList,
					expandedSelectList.getTableExpression());
		}

		this.outputSQL = SQLTranslatorUtils.translate(expandedSelectList, this.parameters, this.columnNameToModelMap);
		this.selectColumnModels = SQLTranslatorUtils.getSelectColumns(this.model.getSelectList(), columnNameToModelMap, isAggregatedResult);
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
	public ColumnMapper getSelectColumnModels() {
		return selectColumnModels;
	}

	/**
	 * All of the Columns of the table.
	 * @return
	 */
	public List<ColumnModel> getTableSchema() {
		return tableSchema;
	}
}
