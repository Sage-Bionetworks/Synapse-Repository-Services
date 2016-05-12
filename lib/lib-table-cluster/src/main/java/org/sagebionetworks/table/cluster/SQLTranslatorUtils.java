package org.sagebionetworks.table.cluster;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.model.CharacterFactor;
import org.sagebionetworks.table.query.model.CharacterPrimary;
import org.sagebionetworks.table.query.model.CharacterValueExpression;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.Factor;
import org.sagebionetworks.table.query.model.FunctionType;
import org.sagebionetworks.table.query.model.HasQuoteValue;
import org.sagebionetworks.table.query.model.Identifier;
import org.sagebionetworks.table.query.model.MysqlFunction;
import org.sagebionetworks.table.query.model.NumericPrimary;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.NumericValueFunction;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.StringValueExpression;
import org.sagebionetworks.table.query.model.Term;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;
import org.sagebionetworks.table.query.model.visitors.ToTranslatedSqlVisitor;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.collect.Lists;

/**
 * Helper methods to translate table SQL queries.
 * 
 *
 */
public class SQLTranslatorUtils {

	/**
	 * Translate the passed query model into output SQL.
	 * 
	 * @param model The model representing a query.
	 * @param outputBuilder
	 * @param parameters
	 */
	public static String translate(QuerySpecification model, final Map<String, Object> parameters,
			final Map<String, ColumnModel> columnNameToModelMap) {
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		model.doVisit(visitor);
		return visitor.getSql();
	}

	/**
	 * Get the list of column IDs that are referenced in the select calsue.
	 * 
	 * @param allColumns
	 * @param selectList
	 * @param isAggregatedResult
	 * @return
	 */
	public static List<SelectColumn> getSelectColumns(SelectList selectList, LinkedHashMap<String, ColumnModel> columnNameToModelMap,
			boolean isAggregatedResult) {
		ValidateArgument.required(columnNameToModelMap, "all columns");
		ValidateArgument.required(selectList, "selectList");
		if (selectList.getAsterisk() != null) {
			throw new IllegalStateException("The columns should have been expanded before getting here");
		} else {
			List<SelectColumn> select = Lists.newArrayListWithCapacity(selectList.getColumns().size());
			for (DerivedColumn dc : selectList.getColumns()) {
				SelectColumn model = getSelectColumns(dc, columnNameToModelMap);
				if(isAggregatedResult){
					// never pass an ID for an aggregate query.
					model.setId(null);
				}
				select.add(model);
			}
			return select;
		}
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
		// done
		return selectColumn;
	}
	
	/**
	 * Get the base column type for a 
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
			return true;
		default:
			return false;
		}
	}
	

	public static DerivedColumn createDerivedColumn(String columnName) {
		return new DerivedColumn(new ValueExpression(new StringValueExpression(new CharacterValueExpression(new CharacterFactor(
				new CharacterPrimary(new ValueExpressionPrimary(new ColumnReference(null, new ColumnName(new Identifier(
						SqlElementUntils.createActualIdentifier(columnName)))))))))), null);
	}

	public static DerivedColumn createDerivedColumn(MysqlFunction mysqlFunction) {
		return new DerivedColumn(new ValueExpression(new NumericValueExpression(new Term(new Factor(new NumericPrimary(
				new NumericValueFunction(mysqlFunction)))))), null);
	}
}
