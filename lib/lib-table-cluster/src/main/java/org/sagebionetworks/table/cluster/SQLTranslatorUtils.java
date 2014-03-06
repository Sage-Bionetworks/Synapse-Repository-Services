package org.sagebionetworks.table.cluster;

import java.util.Map;

import org.sagebionetworks.table.query.model.ActualIdentifier;
import org.sagebionetworks.table.query.model.BooleanFactor;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.BooleanTerm;
import org.sagebionetworks.table.query.model.BooleanTest;
import org.sagebionetworks.table.query.model.CharacterFactor;
import org.sagebionetworks.table.query.model.CharacterPrimary;
import org.sagebionetworks.table.query.model.CharacterValueExpression;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.ComparisonPredicate;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.Identifier;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.InValueList;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.model.RowValueConstructorElement;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.StringValueExpression;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.model.UnsignedValueSpecification;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;
import org.sagebionetworks.table.query.model.WhereClause;

/**
 * Helper methods to translate table SQL queries.
 * 
 * @author jmhill
 *
 */
public class SQLTranslatorUtils {

	/**
	 * Translate the passed query model into output SQL.
	 * @param model The model representing a query.
	 * @param outputBuilder
	 * @param parameters
	 */
	public static void translate(QuerySpecification model,
			StringBuilder builder, Map<String, Object> parameters, Map<String, Long> columnNameToIdMap) {
		builder.append("SELECT");
		if(model.getSetQuantifier() != null){
			builder.append(" ");
			builder.append(model.getSetQuantifier().name());
		}
		builder.append(" ");
		translate(model.getSelectList(), builder, parameters,columnNameToIdMap);
		builder.append(" ");
		translate(model.getTableExpression(), builder, parameters, columnNameToIdMap);
	}



	/**
	 * 
	 * @param selectList
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(SelectList selectList, StringBuilder builder, Map<String, Object> parameters,Map<String, Long> columnNameToIdMap) {
		if(selectList == null) throw new IllegalArgumentException("SelectList cannot be null");
		if(selectList.getAsterisk() != null){
			builder.append("*");
		}else if(selectList.getColumns() != null){
			if(selectList.getColumns().size() < 1) throw new IllegalArgumentException("Select list must have at least one element");
			
			// Process each column
			boolean isAggregate = false;
			boolean first = true;
			for(DerivedColumn column: selectList.getColumns()){
				if(!first){
					builder.append(", ");
				}
				if(translate(column, builder, columnNameToIdMap)){
					isAggregate = true;
				}
				first = false;
			}
			// If this is not an aggregate query then we must also fetch the row id and row version
			if(!isAggregate){
				builder.append(", ").append(SQLUtils.ROW_ID).append(", ").append(SQLUtils.ROW_VERSION);
			}			
		}else{
			throw new IllegalArgumentException("Select list must have either an Asterisk or columns");
		}
	}
	
	/**
	 * 
	 * @param column
	 * @param builder
	 * @param columnNameToIdMap
	 * @return true if this column is an aggregation function.
	 */
	static boolean translate(DerivedColumn column,
			StringBuilder builder, Map<String, Long> columnNameToIdMap) {
		if(column == null) throw new IllegalArgumentException("DerivedColumn cannot be null");
		// We only need the ValueExpressionPrimary for this case.
		ValueExpressionPrimary valueExpressionPrimary = getValueExpressionPrimary(column);
		return translate(valueExpressionPrimary, builder, columnNameToIdMap);
	}
	
	/**
	 * Translate a ValueExpressionPrimary.
	 * @param valueExpressionPrimary
	 * @param builder
	 * @param columnNameToIdMap
	 * @return
	 */
	static boolean translate(ValueExpressionPrimary valueExpressionPrimary,
			StringBuilder builder, Map<String, Long> columnNameToIdMap) {
		if(valueExpressionPrimary == null) throw new IllegalArgumentException("ValueExpressionPrimary cannot be null");
		if(valueExpressionPrimary.getColumnReference() != null){
			translate(valueExpressionPrimary.getColumnReference(), builder, columnNameToIdMap);
			return false;
		}else if(valueExpressionPrimary.getSetFunctionSpecification() != null){
			translate(valueExpressionPrimary.getSetFunctionSpecification(), builder, columnNameToIdMap);
			return true;
		}else{
			throw new IllegalArgumentException("DerivedColumn must have either a ColumnReference or SetFunctionSpecification");
		}
	}
	
	/**
	 * 
	 * @param setFunctionSpecification
	 * @param builder
	 * @param columnNameToIdMap
	 */
	static void translate(
			SetFunctionSpecification setFunctionSpecification,
			StringBuilder builder, Map<String, Long> columnNameToIdMap) {
		if(setFunctionSpecification == null) throw new IllegalArgumentException("SetFunctionSpecification cannot be null");
		if(setFunctionSpecification.getCountAsterisk() != null){
			builder.append("COUNT(*)");
		}else{
			if(setFunctionSpecification.getSetFunctionType() == null) throw new IllegalArgumentException("SetFunctionType cannot be null");
			builder.append(setFunctionSpecification.getSetFunctionType().name());
			builder.append("(");
			if(setFunctionSpecification.getSetQuantifier() != null){
				builder.append(setFunctionSpecification.getSetQuantifier().name()).append(" ");
			}
			ValueExpressionPrimary primary = getValueExpressionPrimary(setFunctionSpecification.getValueExpression());
			translate(primary, builder, columnNameToIdMap);
			builder.append(")");
		}
	}



	/**
	 * Translate a ColumnReference
	 * @param columnReference
	 * @param builder
	 * @param columnNameToIdMap
	 */
	static void translate(ColumnReference columnReference, StringBuilder builder, Map<String, Long> columnNameToIdMap){
		if(columnReference == null) throw new IllegalArgumentException("ColumnReference cannot be null");
		String columnName = getStringValueOf(columnReference.getNameLHS());
		// Lookup the ID for this column
		Long columnId = columnNameToIdMap.get(columnName);
		if(columnId == null) throw new IllegalArgumentException("Unknown column name: "+columnName);
		builder.append(SQLUtils.COLUMN_PREFIX).append(columnId);
		if(columnReference.getNameRHS() != null){
			String subName = getStringValueOf(columnReference.getNameRHS());
			// Remove double quotes if they are included.
			subName = subName.replaceAll("\"", "");
			builder.append("_").append(subName);
		}
	}

	/**
	 * Get the string value of a ColumnName.
	 * @param columnName
	 * @return
	 */
	public static String getStringValueOf(ColumnName columnName){
		if(columnName == null) throw new IllegalArgumentException("ColumName cannot be null");
		return getStringValueOf(columnName.getIdentifier());
	}
	
	/**
	 * Get the string value of an Identifier.
	 * @param identifier
	 * @return
	 */
	public static String getStringValueOf(Identifier identifier){
		if(identifier == null) throw new IllegalArgumentException("Identifier cannot be null");
		return getStringValueOf(identifier.getActualIdentifier());
	}
	
	/**
	 * Get the string value of an ActualIdentifier.
	 * @param actualIdentifier
	 * @return
	 */
	public static String getStringValueOf(ActualIdentifier actualIdentifier) {
		if(actualIdentifier == null) throw new IllegalArgumentException("ActualIdentifier cannot be null");
		if(actualIdentifier.getDelimitedIdentifier() != null){
			return actualIdentifier.getDelimitedIdentifier();
		}else{
			return actualIdentifier.getRegularIdentifier();
		}
	}



	/**
	 * Get a ValueExpressionPrimary from a ValueExpression
	 * @param valueExpression
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(DerivedColumn derivedColumn){
		if(derivedColumn == null) throw new IllegalArgumentException("DerivedColumn cannot be null");
		return getValueExpressionPrimary(derivedColumn.getValueExpression());
	}
	
	/**
	 * Get a ValueExpressionPrimary from a ValueExpression
	 * @param valueExpression
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(ValueExpression valueExpression){
		if(valueExpression == null) throw new IllegalArgumentException("ValueExpression cannot be null");
		return getValueExpressionPrimary(valueExpression.getStringValueExpression());
	}
	/**
	 * Get a ValueExpressionPrimary from a StringValueExpression
	 * @param stringValueExpression
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(StringValueExpression stringValueExpression){
		if(stringValueExpression == null) throw new IllegalArgumentException("StringValueExpression cannot be null");
		return getValueExpressionPrimary(stringValueExpression.getCharacterValueExpression());
	}

	/**
	 * Get a ValueExpressionPrimary from a CharacterValueExpression
	 * @param characterValueExpression
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(
			CharacterValueExpression characterValueExpression) {
		if(characterValueExpression == null) throw new IllegalArgumentException("CharacterValueExpression cannot be null");
		return getValueExpressionPrimary(characterValueExpression.getCharacterFactor());
	}

	/**
	 * Get a ValueExpressionPrimary from a CharacterFactor
	 * @param characterFactor
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(
			CharacterFactor characterFactor) {
		if(characterFactor == null) throw new IllegalArgumentException("CharacterFactor cannot be null");
		return getValueExpressionPrimary(characterFactor.getCharacterPrimary());
	}

	/**
	 * Get a ValueExpressionPrimary from a CharacterPrimary
	 * @param characterPrimary
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(
			CharacterPrimary characterPrimary) {
		if(characterPrimary == null) throw new IllegalArgumentException("CharacterPrimary cannot be null");
		return characterPrimary.getValueExpressionPrimary();
	}

	/**
	 * 
	 * @param tableExpression
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(TableExpression tableExpression,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(tableExpression == null) throw new IllegalArgumentException("TableExpression cannot be null");
		if(tableExpression.getFromClause() == null) throw new IllegalArgumentException("FromClause cannot be null");
		if(tableExpression.getFromClause().getTableReference() == null) throw new IllegalArgumentException("TableReference cannot be null");
		builder.append("FROM ");
		Long tableId = Long.parseLong(tableExpression.getFromClause().getTableReference().getTableName());
		builder.append(SQLUtils.TABLE_PREFIX).append(tableId);
		if(tableExpression.getWhereClause() != null){
			builder.append(" ");
			translate(tableExpression.getWhereClause(), builder, parameters, columnNameToIdMap);
		}
	}

	/**
	 * Translate WhereClause.
	 * @param whereClause
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(WhereClause whereClause,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(whereClause == null) throw new IllegalArgumentException("WhereClause cannot be null");
		builder.append("WHERE ");
		translate(whereClause.getSearchCondition(), builder, parameters, columnNameToIdMap);
	}

	/**
	 * Translate a SearchCondition
	 * @param searchCondition
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(SearchCondition searchCondition,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(searchCondition == null) throw new IllegalArgumentException("SearchCondition cannot be null");
		if(searchCondition.getOrBooleanTerms() == null) throw new IllegalArgumentException("BooleanTerms cannot be null");
		if(searchCondition.getOrBooleanTerms().size() < 1) throw new IllegalArgumentException("There must be at least one BooleanTerm");
		boolean first = true;
		for(BooleanTerm booleanTerm: searchCondition.getOrBooleanTerms()){
			if(!first){
				builder.append("OR");
			}
			translate(booleanTerm, builder, parameters, columnNameToIdMap);
			first = false;
		}
	}

	/**
	 * Translate BooleanTerm.
	 * 
	 * @param booleanTerm
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(BooleanTerm booleanTerm,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(booleanTerm == null) throw new IllegalArgumentException("BooleanTerm cannot be null");
		if(booleanTerm.getAndBooleanFactors() == null) throw new IllegalArgumentException("BooleanFactors cannot be null");
		if(booleanTerm.getAndBooleanFactors().size() < 1) throw new IllegalArgumentException("There must be at least one BooleanFactor");
		boolean first = true;
		for(BooleanFactor booleanFactor: booleanTerm.getAndBooleanFactors()){
			if(!first){
				builder.append("AND");
			}
			translate(booleanFactor, builder, parameters, columnNameToIdMap);
			first = false;
		}
	}

	/**
	 * Translate a BooleanFactor
	 * @param booleanFactor
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(BooleanFactor booleanFactor,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(booleanFactor == null) throw new IllegalArgumentException("BooleanFactor cannot be null");
		if(booleanFactor.getNot() != null){
			builder.append("NOT");
		}
		translate(booleanFactor.getBooleanTest(), builder, parameters, columnNameToIdMap);
	}

	/**
	 * Translate a BooleanTest
	 * @param booleanTest
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(BooleanTest booleanTest,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(booleanTest == null) throw new IllegalArgumentException("BooleanTest cannot be null");
		translate(booleanTest.getBooleanPrimary(), builder, parameters, columnNameToIdMap);
		if(booleanTest.getIs() != null){
			if(booleanTest.getTruthValue() == null) throw new IllegalArgumentException("TruthValue cannot be null");
			builder.append(" IS ");
			if(booleanTest.getNot() != null){
				builder.append("NOT ");
			}
			builder.append(booleanTest.getTruthValue().name());
		}
	}

	/**
	 * Translate BooleanPrimary
	 * @param booleanPrimary
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(BooleanPrimary booleanPrimary,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(booleanPrimary == null) throw new IllegalArgumentException("BooleanPrimary cannot be null");
		if(booleanPrimary.getPredicate() != null){
			translate(booleanPrimary.getPredicate(), builder, parameters, columnNameToIdMap);
		}else{
			builder.append("(");
			translate(booleanPrimary.getSearchCondition(), builder, parameters, columnNameToIdMap);
			builder.append(")");
		}
	}

	/**
	 * Translate a Predicate
	 * @param predicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(Predicate predicate, StringBuilder builder,
			Map<String, Object> parameters, Map<String, Long> columnNameToIdMap) {
		if(predicate == null) throw new IllegalArgumentException("Predicate cannot be null");
		if(predicate.getComparisonPredicate() != null){
			translate(predicate.getComparisonPredicate(), builder, parameters, columnNameToIdMap);
		}else if(predicate.getInPredicate() != null){
			translate(predicate.getInPredicate(), builder, parameters, columnNameToIdMap);
		}else{
			throw new IllegalArgumentException("Unknown Predicate type");
		}
	}


	/**
	 * Translate ComparisonPredicate
	 * @param comparisonPredicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(ComparisonPredicate comparisonPredicate,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(comparisonPredicate == null) throw new IllegalArgumentException("ComparisonPredicate cannot be null");
		if(comparisonPredicate.getCompOp() == null) throw new IllegalArgumentException("CompOp cannot be null");
		// first left-hand-side
		translate(comparisonPredicate.getColumnReferenceLHS(), builder, columnNameToIdMap);
		builder.append(" ").append(comparisonPredicate.getCompOp().toSQL()).append(" ");
		translate(comparisonPredicate.getRowValueConstructorRHS(), builder, parameters);		
	}
	
	/**
	 * Translate an InPredicate
	 * @param inPredicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToIdMap
	 */
	static void translate(InPredicate inPredicate,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, Long> columnNameToIdMap) {
		if(inPredicate == null) throw new IllegalArgumentException("InPredicate cannot be null");
		translate(inPredicate.getColumnReferenceLHS(), builder, columnNameToIdMap);
		builder.append(" IN (");
		translate(inPredicate.getInPredicateValue(), builder, parameters);
		builder.append(")");
	}
	
	/**
	 * Translate an InPredicateValue
	 * @param inPredicateValue
	 * @param builder
	 * @param parameters
	 */
	static void translate(InPredicateValue inPredicateValue,
			StringBuilder builder, Map<String, Object> parameters) {
		if(inPredicateValue == null) throw new IllegalArgumentException("InPredicateValue cannot be null");
		translate(inPredicateValue.getInValueList(), builder, parameters);
	}

	/**
	 * Translate an InValueList
	 * @param inValueList
	 * @param builder
	 * @param parameters
	 */
	static void translate(InValueList inValueList,
			StringBuilder builder, Map<String, Object> parameters) {
		if(inValueList == null) throw new IllegalArgumentException("InValueList cannot be null");
		if(inValueList.getValueExpressions() == null) throw new IllegalArgumentException("ValueExpressions cannot be null");
		if(inValueList.getValueExpressions().size() < 1) throw new IllegalArgumentException("There must be at least one ValueExpression");
		boolean first = true;
		for(ValueExpression valueExpresssion: inValueList.getValueExpressions()){
			if(!first){
				builder.append(", ");
			}
			translateRHS(valueExpresssion, builder, parameters);
			first = false;
		}
	}



	/**
	 * Translate RowValueConstructor
	 * @param rowValueConstructor
	 * @param builder
	 * @param parameters
	 */
	static void translate(RowValueConstructor rowValueConstructor,
			StringBuilder builder, Map<String, Object> parameters) {
		// Put the value in the map.
		if(rowValueConstructor == null) throw new IllegalArgumentException("RowValueConstructor cannot be null");
		if(rowValueConstructor.getRowValueConstructorElement() != null){
			translate(rowValueConstructor.getRowValueConstructorElement(), builder, parameters);
		}
	}
	
	/**
	 * Translate RowValueConstructorElement.
	 * 
	 * @param rowValueConstructorElement
	 * @param builder
	 * @param parameters
	 */
	private static void translate(
			RowValueConstructorElement rowValueConstructorElement,
			StringBuilder builder, Map<String, Object> parameters) {
		if(rowValueConstructorElement == null) throw new IllegalArgumentException("RowValueConstructorElement cannot be null");
		if(rowValueConstructorElement.getNullSpecification() != null){
			builder.append("NULL");
		}else if(rowValueConstructorElement.getDefaultSpecification() != null){
			builder.append("DEFAULT");
		}else{
			translateRHS(rowValueConstructorElement.getValueExpression(), builder, parameters);
		}
	}
	
	/**
	 * Translate a ValueExpression that appears on the right-hand-side of a predicate.
	 * @param valueExpression
	 * @param builder
	 * @param parameters
	 */
	static void translateRHS(ValueExpression valueExpression, StringBuilder builder, Map<String, Object> parameters){
		if(valueExpression == null) throw new IllegalArgumentException("ValueExpression cannot be null");
		// The bind key is used in the SQL and parameter map.
		String bindKey = "b"+parameters.size();
		builder.append(":").append(bindKey);
		// Add the value to the map.
		ValueExpressionPrimary primary = getValueExpressionPrimary(valueExpression);
		String value = getStringValueOf(primary.getUnsignedValueSpecification());
		parameters.put(bindKey, value);
	}

	public static String getStringValueOf(UnsignedValueSpecification unsignedValueSpecification) {
		if(unsignedValueSpecification == null) throw new IllegalArgumentException("UnsignedValueSpecification cannot be null");
		return getStringValueOf(unsignedValueSpecification.getUnsignedLiteral());
	}

	public static String getStringValueOf(UnsignedLiteral unsignedLiteral) {
		if(unsignedLiteral.getGeneralLiteral() != null) return unsignedLiteral.getGeneralLiteral();
		if(unsignedLiteral.getUnsignedNumericLiteral() != null) return unsignedLiteral.getUnsignedNumericLiteral();
		throw new IllegalArgumentException("UnsignedLiteral must have either a GeneralLiteral or UnsignedNumericLiteral");
	}
}
