package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ActualIdentifier;
import org.sagebionetworks.table.query.model.BooleanFunctionPredicate;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.FunctionType;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.HasPredicate;
import org.sagebionetworks.table.query.model.HasQuoteValue;
import org.sagebionetworks.table.query.model.HasReferencedColumn;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SignedLiteral;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.TableReference;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Helper methods to translate user generated queries
 * to queries that run against the actual database.
 *
 */
public class SQLTranslatorUtils {

	private static final String COLON = ":";
	public static final String BIND_PREFIX = "b";
	public static final Set<String> rowMetadataColumnNames = Sets.newHashSet(ROW_ID, ROW_VERSION);

	/**
	 * Get the list of column IDs that are referenced in the select clause.
	 * 
	 * @param allColumns
	 * @param selectList
	 * @param isAggregatedResult
	 * @return
	 */
	public static List<SelectColumn> getSelectColumns(SelectList selectList, Map<String, ColumnModel> columnNameToModelMap, boolean isAggregate) {
		ValidateArgument.required(columnNameToModelMap, "all columns");
		ValidateArgument.required(selectList, "selectList");
		if (selectList.getAsterisk() != null) {
			throw new IllegalStateException("The columns should have been expanded before getting here");
		} 
		List<SelectColumn> selects = Lists.newArrayListWithCapacity(selectList.getColumns().size());
		boolean isAtLeastOneColumnIdNull = false;
		for (DerivedColumn dc : selectList.getColumns()) {
			SelectColumn model = getSelectColumns(dc, columnNameToModelMap);
			selects.add(model);
			if(model.getId() == null){
				isAtLeastOneColumnIdNull = true;
			}
		}
		
		// All columnIds should be null if one column has a null ID or this is an aggregate query.
		if(isAtLeastOneColumnIdNull || isAggregate){
			// clear all columnIds.
			for(SelectColumn select: selects){
				select.setId(null);
			}
		}
		return selects;
	}
	
	/**
	 * All select elements match the schema if all elements have a column ID.
	 * 
	 * @param list
	 * @return
	 */
	public static boolean doAllSelectMatchSchema(List<SelectColumn> selectList){
		ValidateArgument.required(selectList, "selectList");
		for(SelectColumn select: selectList){
			if(select.getId() == null){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Given a DerivedColumn extract all data about both the SelectColumn and ColumnModel.
	 * 
	 * @param derivedColumn
	 * @param columnMap
	 * @return
	 */
	public static SelectColumn getSelectColumns(DerivedColumn derivedColumn, Map<String, ColumnModel> columnMap){
		// Extract data about this column.
		String displayName = derivedColumn.getDisplayName();
		// lookup the column referenced by this select.
		HasQuoteValue referencedColumn = derivedColumn.getReferencedColumn();
		// If element has a function get its name.
		FunctionType functionType = derivedColumn.getFunctionType();
		// Select defines the selection
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName(displayName);
		
		ColumnModel model = null;
		if(referencedColumn != null){
			// Does the reference match an actual column name?
			model = columnMap.get(referencedColumn.getValueWithoutQuotes());
		}
		// Lookup the base type starting only with the column referenced.
		ColumnType columnType = getBaseColulmnType(referencedColumn);
		if(model != null){
			// If we have a column model the base type is defined by it.
			columnType = model.getColumnType();
		}
		// If there is a function it can change the type
		if(functionType != null){
			columnType = getColumnTypeForFunction(functionType, columnType);
		}
		selectColumn.setColumnType(columnType);
		// We only set the id on the select column when the display name match the column name.
		if(model != null && model.getName().equals(displayName)){
			selectColumn.setId(model.getId());
		}
		validateSelectColumn(selectColumn, functionType, model, referencedColumn);
		// done
		return selectColumn;
	}
	
	public static void validateSelectColumn(SelectColumn selectColumn, FunctionType functionType,
			ColumnModel model, HasQuoteValue referencedColumn) {
		ValidateArgument.requirement(model != null
				|| functionType != null
				|| rowMetadataColumnNames.contains(selectColumn.getName().toUpperCase())
				|| (referencedColumn != null && referencedColumn instanceof SignedLiteral),
				"Unknown column "+selectColumn.getName());
	}

	/**
	 * Given a referenced column, attempt to determine the type of the column using only
	 * the SQL.
	 * @param derivedColumn
	 * @return
	 */
	public static ColumnType getBaseColulmnType(HasQuoteValue referencedColumn){
		if(referencedColumn == null){
			return null;
		}
		// Get the upper case column name without quotes.
		String columnNameUpper = referencedColumn.getValueWithoutQuotes().toUpperCase();
		if(TableConstants.ROW_ID.equals(columnNameUpper)){
			return ColumnType.INTEGER;
		}
		if(TableConstants.ROW_VERSION.equals(columnNameUpper)){
			return ColumnType.INTEGER;
		}
		if(!referencedColumn.isSurrounedeWithQuotes()){
			return ColumnType.DOUBLE;
		}
		return ColumnType.STRING;
	}
	
	/**
	 * Determine SelectColumn type for a given FunctionType and base ColumnType.
	 * @param derivedColumn
	 * @param model
	 * @return
	 */
	public static ColumnType getColumnTypeForFunction(FunctionType functionType, ColumnType baseType){
		ValidateArgument.required(functionType, "functionType");
		switch(functionType) {
		case COUNT:
		case FOUND_ROWS:
			return ColumnType.INTEGER;
		case AVG:
			if(!isNumericType(baseType)){
				throw new IllegalArgumentException("Cannot calculate "+functionType.name()+" for type: "+baseType);
			}
			// average is always double.
			return ColumnType.DOUBLE;
		case SUM:
			if(!isNumericType(baseType)){
				throw new IllegalArgumentException("Cannot calculate "+functionType.name()+" for type: "+baseType);
			}
			// sum returns the same type as the numeric column.
			return baseType;
		case MAX:
		case MIN:
			ValidateArgument.required(baseType, "columnType");
			// min and max return the same type as the column.
			return baseType;
		default:
			throw new IllegalArgumentException("Unknown type: "+functionType.name());
		}
	}
	
	/**
	 * Is the given ColumnType numeric?
	 * @param columnType
	 * @return
	 */
	public static boolean isNumericType(ColumnType columnType){
		ValidateArgument.required(columnType, "columnType");
		switch(columnType){
		case INTEGER:
		case DOUBLE:
		case DATE:
		case FILEHANDLEID:
		case USERID:
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * Create a select list from a given table schema.
	 * @param schema
	 * @return
	 */
	public static SelectList createSelectListFromSchema(List<ColumnModel> schema){
		ValidateArgument.required(schema, "schema");
		List<DerivedColumn> columns = new ArrayList<DerivedColumn>(schema.size());
		for(ColumnModel cm: schema){
			columns.add(SqlElementUntils.createDoubleQuotedDerivedColumn(cm.getName()));
		}
		return new SelectList(columns);
	}
	
	/**
	 * Create a new SelectList that includes ROW_ID and ROW_VERSION.
	 * 
	 * @param selectList
	 * @return
	 */
	public static SelectList addRowIdAndVersionToSelect(SelectList selectList){
		List<DerivedColumn> selectColumns = Lists.newArrayListWithCapacity(selectList.getColumns().size() + 2);
		selectColumns.addAll(selectList.getColumns());
		selectColumns.add(SqlElementUntils.createNonQuotedDerivedColumn(ROW_ID));
		selectColumns.add(SqlElementUntils.createNonQuotedDerivedColumn(ROW_VERSION));
		return new SelectList(selectColumns);
	}
	
	/**
	 * Create ColumnTypeInfo[] for a given list of SelectColumns.
	 * 
	 * @param columns
	 * @return
	 */
	public static ColumnTypeInfo[] getColumnTypeInfoArray(List<SelectColumn> columns){
		ColumnTypeInfo[] infoArray = new  ColumnTypeInfo[columns.size()];
		for(int i=0; i<columns.size(); i++){
			SelectColumn column = columns.get(i);
			infoArray[i] = ColumnTypeInfo.getInfoForType(column.getColumnType());
		}
		return infoArray;
	}
	
	/**
	 * Read a Row from a ResultSet that was produced with the given query.
	 * @param rs
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	public static Row readRow(ResultSet rs, boolean includesRowIdAndVersion, ColumnTypeInfo[] colunTypes) throws SQLException{
		Row row = new Row();
		List<String> values = new LinkedList<String>();
		row.setValues(values);
		if(includesRowIdAndVersion){
			row.setRowId(rs.getLong(ROW_ID));
			row.setVersionNumber(rs.getLong(ROW_VERSION));
		}
		// Read the select columns.
		for(int i=0; i < colunTypes.length; i++){
			ColumnTypeInfo type = colunTypes[i];
			String value = rs.getString(i+1);
			value = TableModelUtils.translateRowValueFromQuery(value, type);
			values.add(value);
		}
		return row;
	}
	
	

	/**
	 * Translate this query into a form that can be executed against the actual table index.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param transformedModel
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	public static void translateModel(QuerySpecification transformedModel,
			Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		// Select columns
		Iterable<HasReferencedColumn> selectColumns = transformedModel.getSelectList().createIterable(HasReferencedColumn.class);
		for(HasReferencedColumn hasReference: selectColumns){
			translateSelect(hasReference, columnNameToModelMap);
		}
		
		TableExpression tableExpression = transformedModel.getTableExpression();
		if(tableExpression == null){
			// nothing else to do.
			return;
		}
		// First change the table name
		TableReference tableReference = tableExpression.getFromClause().getTableReference();
		translate(tableReference);
		
		// Translate where
		WhereClause whereClause = tableExpression.getWhereClause();
		if(whereClause != null){
			// First we need to replace any boolean functions.
			Iterable<BooleanPrimary> booleanPrimaries = whereClause.createIterable(BooleanPrimary.class);
			for(BooleanPrimary booleanPrimary: booleanPrimaries){
				replaceBooleanFunction(booleanPrimary, columnNameToModelMap);
			}
			// Translate all predicates
			Iterable<HasPredicate> hasPredicates = whereClause.createIterable(HasPredicate.class);
			for(HasPredicate predicate: hasPredicates){
				translate(predicate, parameters, columnNameToModelMap);
			}
		}
		// translate the group by
		GroupByClause groupByClause = tableExpression.getGroupByClause();
		if(groupByClause != null){
			translate(groupByClause, columnNameToModelMap);
		}
		// translate the order by
		OrderByClause orderByClause = tableExpression.getOrderByClause();
		if(orderByClause != null){
			Iterable<HasReferencedColumn> orderByReferences = orderByClause.createIterable(HasReferencedColumn.class);
			for(HasReferencedColumn hasReference: orderByReferences){
				translateOrderBy(hasReference, columnNameToModelMap);
			}
		}
		// translate Pagination
		Pagination pagination = tableExpression.getPagination();
		if(pagination != null){
			translate(pagination, parameters);
		}
	}

	/**
	 * Translate pagination.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param pagination
	 * @param parameters
	 */
	public static void translate(Pagination pagination,
			Map<String, Object> parameters) {
		ValidateArgument.required(pagination, "pagination");
		ValidateArgument.required(parameters, "parameters");
		// limit
		if(pagination.getLimit() != null){
			String key = BIND_PREFIX+parameters.size();
			Long value = pagination.getLimitLong();
			parameters.put(key, value);
			pagination.setLimit(COLON+key);
		}
		// offset
		if(pagination.getOffset() != null){
			String key = BIND_PREFIX+parameters.size();
			Long value = pagination.getOffsetLong();
			parameters.put(key, value);
			pagination.setOffset(COLON+key);
		}
	}

	/**
	 * Translate a GroupByClause.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param groupByClause
	 * @param columnNameToModelMap
	 */
	public static void translate(GroupByClause groupByClause,
			Map<String, ColumnModel> columnNameToModelMap) {
		ValidateArgument.required(groupByClause, "groupByClause");
		ValidateArgument.required(columnNameToModelMap, "columnNameToModelMap");
		Iterable<HasQuoteValue> hasQuotes = groupByClause.createIterable(HasQuoteValue.class);
		if(hasQuotes != null){
			for(HasQuoteValue hasQuoteValue: hasQuotes){
				// Lookup the column
				ColumnModel model = columnNameToModelMap.get(hasQuoteValue.getValueWithoutQuotes());
				if(model != null){
					String newName = SQLUtils.getColumnNameForId(model.getId());
					hasQuoteValue.replaceUnquoted(newName);
				}
			}
		}
	}

	/**
	 * Translate a predicate.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param predicate
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	public static void translate(HasPredicate predicate,
			Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		ValidateArgument.required(predicate, "predicate");
		ValidateArgument.required(parameters, "parameters");
		ValidateArgument.required(columnNameToModelMap, "columnNameToModelMap");
		// Translate the left-hand-side
		ColumnReference leftHandSide = predicate.getLeftHandSide();
		// lookup the column name
		ActualIdentifier actualIdentifier = leftHandSide.getNameRHS().getFirstElementOfType(ActualIdentifier.class);
		ColumnModel model = columnNameToModelMap.get(actualIdentifier.getValueWithoutQuotes());
		if(model != null){
			String newName = SQLUtils.getColumnNameForId(model.getId());
			actualIdentifier.replaceUnquoted(newName);
			// handle the right-hand-side
			Iterable<HasQuoteValue> rightHandSide = predicate.getRightHandSideValues();
			if(rightHandSide != null){
				for(HasQuoteValue hasQuoteValue: rightHandSide){
					translateRightHandeSide(hasQuoteValue, model, parameters);
				}
			}
		}
	}
	
	/**
	 * Translate the right-hand-side of a predicate.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param hasQuoteValue
	 * @param model
	 * @param parameters
	 */
	public static void translateRightHandeSide(HasQuoteValue hasQuoteValue,
			ColumnModel model, Map<String, Object> parameters) {
		ValidateArgument.required(hasQuoteValue, "hasQuoteValue");
		ValidateArgument.required(model, "model");
		ValidateArgument.required(parameters, "parameters");
		
		String key = BIND_PREFIX+parameters.size();
		String value = hasQuoteValue.getValueWithoutQuotes();
		Object valueObject = null;
		try{
			valueObject = SQLUtils.parseValueForDB(model.getColumnType(), value);
		}catch (IllegalArgumentException e){
			// thrown for number format exception.
			valueObject = value;
		}

		parameters.put(key, valueObject);
		hasQuoteValue.replaceUnquoted(COLON+key);
	}

	/**
	 * Replace any BooleanFunctionPredicate with a search condition.
	 * For example: 'isInfinity(DOUBLETYPE)' will be replaced with (_DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ('-Infinity', 'Infinity'))'
	 * @param booleanPrimary
	 * @param columnNameToModelMap
	 */
	public static void replaceBooleanFunction(BooleanPrimary booleanPrimary, Map<String, ColumnModel> columnNameToModelMap){
		if(booleanPrimary.getPredicate() != null && booleanPrimary.getPredicate().getBooleanFunctionPredicate() != null){
			BooleanFunctionPredicate bfp = booleanPrimary.getPredicate().getBooleanFunctionPredicate();
			String columnName = bfp.getColumnReference().getFirstUnquotedValue();
			ColumnModel cm = columnNameToModelMap.get(columnName);
			if(cm == null){
				throw new IllegalArgumentException("Function: "+bfp.getBooleanFunction()+" has unknown reference: "+columnName);
			}
			if(!ColumnType.DOUBLE.equals(cm.getColumnType())){
				throw new IllegalArgumentException("Function: "+bfp.getBooleanFunction()+" can only be used with a column of type DOUBLE.");
			}
			StringBuilder builder = new StringBuilder();
			// Is this a boolean function
			switch(bfp.getBooleanFunction()){
			case ISINFINITY:
				SQLUtils.appendIsInfinity(cm.getId(), "", builder);
				break;
			case ISNAN:
				SQLUtils.appendIsNan(cm.getId(), "", builder);
				break;
			default:
				throw new IllegalArgumentException("Unknown boolean function: "+bfp.getBooleanFunction());
			}
			
			try {
				BooleanPrimary newPrimary = new TableQueryParser(builder.toString()).booleanPrimary();
				booleanPrimary.replaceSearchCondition(newPrimary.getSearchCondition());
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	/**
	 * Translate a HasReferencedColumn for the select clause.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param column
	 * @param columnNameToModelMap
	 */
	public static void translateSelect(HasReferencedColumn column,
			Map<String, ColumnModel> columnNameToModelMap) {
		HasQuoteValue hasQuoteValue = column.getReferencedColumn();
		if(hasQuoteValue != null){
			String unquotedName = hasQuoteValue.getValueWithoutQuotes();
			ColumnModel model = columnNameToModelMap.get(unquotedName);
			String newName = null;
			if(model != null){
				if(!column.isReferenceInFunction() && ColumnType.DOUBLE.equals(model.getColumnType())){
					// non-function doubles are translated into a switch between the enum an double column.
					newName = SQLUtils.createDoubleCluase(model.getId());
				}else{
					newName = SQLUtils.getColumnNameForId(model.getId());
				}
			}
			if(newName != null){
				hasQuoteValue.replaceUnquoted(newName);
			}
		}
	}
	
	/**
	 * Translate HasReferencedColumn for order by clause.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param column
	 * @param columnNameToModelMap
	 */
	public static void translateOrderBy(HasReferencedColumn column,
			Map<String, ColumnModel> columnNameToModelMap) {
		HasQuoteValue hasQuoteValue = column.getReferencedColumn();
		if(hasQuoteValue != null){
			String unquotedName = hasQuoteValue.getValueWithoutQuotes();
			ColumnModel model = columnNameToModelMap.get(unquotedName);
			String newName = null;
			if(model != null){
				newName = SQLUtils.getColumnNameForId(model.getId());
				hasQuoteValue.replaceUnquoted(newName);
			}
		}
	}

	/**
	 * Translate the table name.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param tableReference
	 */
	public static void translate(TableReference tableReference) {
		Long tableId = KeyFactory.stringToKey(tableReference.getTableName());
		tableReference.replaceTableName(SQLUtils.TABLE_PREFIX + tableId);
	}
	
}
