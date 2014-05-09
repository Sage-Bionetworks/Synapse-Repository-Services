package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.model.*;

import com.google.common.base.Function;

/**
 * Helper methods to translate table SQL queries.
 * 
 * @author jmhill
 *
 */
public class SQLTranslatorUtils {

	public static final DateTimeFormatter dateParser;
	
	static {
		// DateTimeFormat.forPattern("yyyy-M-D H:m:s.S");
		DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder().appendPattern("yy-M-d").toFormatter();
		DateTimeFormatter microSecondsFormatter = new DateTimeFormatterBuilder().appendLiteral('.').appendPattern("SSS").toFormatter();
		DateTimeFormatter secondsFormatter = new DateTimeFormatterBuilder().appendPattern(":s")
				.appendOptional(microSecondsFormatter.getParser()).toFormatter();
		DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().appendPattern(" H:m")
				.appendOptional(secondsFormatter.getParser()).toFormatter();
		dateParser = new DateTimeFormatterBuilder().append(dateFormatter).appendOptional(timeFormatter.getParser()).toFormatter()
				.withZoneUTC();
	}

	private static final Function<ColumnModel, Long> MODEL_TO_ID = new Function<ColumnModel, Long>() {
		@Override
		public Long apply(ColumnModel input) {
			return Long.parseLong(input.getId());
		}
	};
	
	/**
	 * Translate the passed query model into output SQL.
	 * @param model The model representing a query.
	 * @param outputBuilder
	 * @param parameters
	 */
	public static boolean translate(QuerySpecification model, StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		builder.append("SELECT");
		if(model.getSetQuantifier() != null){
			builder.append(" ");
			builder.append(model.getSetQuantifier().name());
		}
		builder.append(" ");
		boolean isAggregated = translate(model.getSelectList(), builder, parameters, columnNameToModelMap);
		builder.append(" ");
		translate(model.getTableExpression(), builder, parameters, columnNameToModelMap);
		return isAggregated;
	}



	/**
	 * Translate SelectList
	 * 
	 * @param selectList
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static boolean translate(SelectList selectList, StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(selectList == null) throw new IllegalArgumentException("SelectList cannot be null");
		if(selectList.getAsterisk() != null){
			builder.append("*");
			return false;
		}else if(selectList.getColumns() != null){
			if(selectList.getColumns().size() < 1) throw new IllegalArgumentException("Select list must have at least one element");
			
			// Process each column
			boolean isAggregate = false;
			boolean first = true;
			for(DerivedColumn column: selectList.getColumns()){
				if(!first){
					builder.append(", ");
				}
				if (translate(column, builder, columnNameToModelMap)) {
					isAggregate = true;
				}
				first = false;
			}
			// If this is not an aggregate query then we must also fetch the row id and row version
			if(!isAggregate){
				builder.append(", ").append(ROW_ID).append(", ").append(ROW_VERSION);
			}
			return isAggregate;
		}else{
			throw new IllegalArgumentException("Select list must have either an Asterisk or columns");
		}
	}
	
	/**
	 * 
	 * @param column
	 * @param builder
	 * @param columnNameToModelMap
	 * @return true if this column is an aggregation function.
	 */
	static boolean translate(DerivedColumn column, StringBuilder builder, Map<String, ColumnModel> columnNameToModelMap) {
		if(column == null) throw new IllegalArgumentException("DerivedColumn cannot be null");
		// We only need the ValueExpressionPrimary for this case.
		ValueExpressionPrimary valueExpressionPrimary = getValueExpressionPrimary(column);
		return translate(valueExpressionPrimary, builder, columnNameToModelMap);
	}
	
	/**
	 * Translate a ValueExpressionPrimary.
	 * 
	 * @param valueExpressionPrimary
	 * @param builder
	 * @param columnNameToModelMap
	 * @return
	 */
	static boolean translate(ValueExpressionPrimary valueExpressionPrimary, StringBuilder builder,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(valueExpressionPrimary == null) throw new IllegalArgumentException("ValueExpressionPrimary cannot be null");
		if(valueExpressionPrimary.getColumnReference() != null){
			translate(valueExpressionPrimary.getColumnReference(), builder, columnNameToModelMap);
			return false;
		}else if(valueExpressionPrimary.getSetFunctionSpecification() != null){
			translate(valueExpressionPrimary.getSetFunctionSpecification(), builder, columnNameToModelMap);
			return true;
		}else{
			throw new IllegalArgumentException("DerivedColumn must have either a ColumnReference or SetFunctionSpecification");
		}
	}
	
	/**
	 * 
	 * @param setFunctionSpecification
	 * @param builder
	 * @param columnNameToModelMap
	 */
	static void translate(SetFunctionSpecification setFunctionSpecification, StringBuilder builder,
			Map<String, ColumnModel> columnNameToModelMap) {
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
			translate(primary, builder, columnNameToModelMap);
			builder.append(")");
		}
	}



	/**
	 * Translate a ColumnReference
	 * 
	 * @param columnReference
	 * @param builder
	 * @param columnNameToModelMap
	 * @return
	 */
	static ColumnModel translate(ColumnReference columnReference, StringBuilder builder, Map<String, ColumnModel> columnNameToModelMap) {
		if(columnReference == null) throw new IllegalArgumentException("ColumnReference cannot be null");
		String columnName = getStringValueOf(columnReference.getNameLHS());
		// Is this a reserved column name like ROW_ID or ROW_VERSION?
		if(TableConstants.isReservedColumnName(columnName)){
			// use the returned reserve name in destination SQL.
			builder.append(columnName.toUpperCase());
			return null;
		}else{
			// Not a reserved column name.
			// Lookup the ID for this column
			ColumnModel column = columnNameToModelMap.get(columnName.trim());
			if(column == null) throw new IllegalArgumentException("Unknown column name: "+columnName);
			builder.append(SQLUtils.COLUMN_PREFIX).append(column.getId());
			if(columnReference.getNameRHS() != null){
				String subName = getStringValueOf(columnReference.getNameRHS());
				// Remove double quotes if they are included.
				subName = subName.replaceAll("\"", "");
				builder.append("_").append(subName);
			}
			return column;
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
	 * Get a ValueExpressionPrimary from a Pattern.
	 * @param pattern
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(Pattern pattern) {
		if(pattern == null) throw new IllegalArgumentException("Pattern cannot be null");
		return getValueExpressionPrimary(pattern.getCharacterValueExpression());
	}
	
	/**
	 * Get a ValueExpressionPrimary from an EscapeCharacter.
	 * @param pattern
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(EscapeCharacter escape) {
		if(escape == null) throw new IllegalArgumentException("EscapeCharacter cannot be null");
		return getValueExpressionPrimary(escape.getCharacterValueExpression());
	}

	/**
	 * 
	 * @param tableExpression
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(TableExpression tableExpression,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(tableExpression == null) throw new IllegalArgumentException("TableExpression cannot be null");
		if(tableExpression.getFromClause() == null) throw new IllegalArgumentException("FromClause cannot be null");
		if(tableExpression.getFromClause().getTableReference() == null) throw new IllegalArgumentException("TableReference cannot be null");
		builder.append("FROM ");
		String tableName = tableExpression.getFromClause().getTableReference().getTableName();
		Long tableId = Long.parseLong(tableName.substring(3, tableName.length()));
		builder.append(SQLUtils.TABLE_PREFIX).append(tableId);
		if(tableExpression.getWhereClause() != null){
			builder.append(" ");
			translate(tableExpression.getWhereClause(), builder, parameters, columnNameToModelMap);
		}
		if(tableExpression.getGroupByClause() != null){
			builder.append(" ");
			translate(tableExpression.getGroupByClause(), builder, columnNameToModelMap);
		}
		if(tableExpression.getOrderByClause() != null){
			builder.append(" ");
			translate(tableExpression.getOrderByClause(), builder, columnNameToModelMap);
		}
		if(tableExpression.getPagination() != null){
			builder.append(" ");
			translate(tableExpression.getPagination(), builder, parameters);
		}
	}

	/**
	 * Translate Pagination
	 * 
	 * @param pagination
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(Pagination pagination, StringBuilder builder,
			Map<String, Object> parameters) {
		if(pagination == null) throw new IllegalArgumentException("Pagination cannot be null");
		if(pagination.getLimit() == null) throw new IllegalArgumentException("Limit cannot be null");
		builder.append("LIMIT ");
		// The bind key is used in the SQL and parameter map.
		String bindKey = "b"+parameters.size();
		builder.append(":").append(bindKey);
		parameters.put(bindKey, pagination.getLimit());
		if(pagination.getOffset() != null){
			builder.append(" OFFSET ");
			bindKey = "b"+parameters.size();
			builder.append(":").append(bindKey);
			parameters.put(bindKey, pagination.getOffset());
		}
	}



	/**
	 * Translate OrderByClause
	 * 
	 * @param orderByClause
	 * @param builder
	 * @param columnNameToModelMap
	 */
	static void translate(OrderByClause orderByClause, StringBuilder builder, Map<String, ColumnModel> columnNameToModelMap) {
		if(orderByClause == null) throw new IllegalArgumentException("OrderByClause cannot be null");
		builder.append("ORDER BY ");
		translate(orderByClause.getSortSpecificationList(), builder, columnNameToModelMap);
	}
	
	/**
	 * Translate SortSpecificationList
	 * 
	 * @param sortSpecificationList
	 * @param builder
	 * @param columnNameToModelMap
	 */
	static void translate(SortSpecificationList sortSpecificationList, StringBuilder builder, Map<String, ColumnModel> columnNameToModelMap) {
		if(sortSpecificationList == null) throw new IllegalArgumentException("SortSpecificationList cannot be null");
		if(sortSpecificationList.getSortSpecifications() == null) throw new IllegalArgumentException("SortSpecifications cannot be null");
		if(sortSpecificationList.getSortSpecifications().size() < 1) throw new IllegalArgumentException("Must have at least one SortSpecification");
		boolean first = true;
		for(SortSpecification sortSpecfication: sortSpecificationList.getSortSpecifications()){
			if(!first){
				builder.append(", ");
			}
			translate(sortSpecfication, builder, columnNameToModelMap);
			first = false;
		}
	}
	
	/**
	 * Translate SortSpecification
	 * 
	 * @param sortSpecfication
	 * @param builder
	 * @param columnNameToModelMap
	 */
	static void translate(SortSpecification sortSpecfication, StringBuilder builder, Map<String, ColumnModel> columnNameToModelMap) {
		if(sortSpecfication == null) throw new IllegalArgumentException("SortSpecification cannot be null");
		translate(sortSpecfication.getSortKey(), builder, columnNameToModelMap);
		if(sortSpecfication.getOrderingSpecification() != null){
			builder.append(" ").append(sortSpecfication.getOrderingSpecification().name());
		}
	}

	/**
	 * Translate SortKey
	 * 
	 * @param sortKey
	 * @param builder
	 * @param columnNameToModelMap
	 */
	static void translate(SortKey sortKey, StringBuilder builder, Map<String, ColumnModel> columnNameToModelMap) {
		if(sortKey == null) throw new IllegalArgumentException("SortKey cannot be null");
		translate(sortKey.getColumnReference(), builder, columnNameToModelMap);
	}

	/**
	 * Translate a GroupByClause.
	 * 
	 * @param groupByClause
	 * @param builder
	 * @param columnNameToModelMap
	 */
	static void translate(GroupByClause groupByClause, StringBuilder builder, Map<String, ColumnModel> columnNameToModelMap) {
		if(groupByClause == null) throw new IllegalArgumentException("GroupByClause cannot be null");
		builder.append("GROUP BY ");
		translate(groupByClause.getGroupingColumnReferenceList(), builder, columnNameToModelMap);
	}

	/**
	 * Translate GroupingColumnReferenceList
	 * 
	 * @param groupingColumnReferenceList
	 * @param builder
	 * @param columnNameToModelMap
	 */
	static void translate(GroupingColumnReferenceList groupingColumnReferenceList, StringBuilder builder,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(groupingColumnReferenceList == null) throw new IllegalArgumentException("GroupingColumnReferenceList cannot be null");
		if(groupingColumnReferenceList.getGroupingColumnReferences() == null) throw new IllegalArgumentException("GroupingColumnReferences cannot be null");
		if(groupingColumnReferenceList.getGroupingColumnReferences().size() <1 ) throw new IllegalArgumentException("Must have at least one GroupingColumnReference");
		boolean first = true;
		for(GroupingColumnReference groupingColumnReference: groupingColumnReferenceList.getGroupingColumnReferences()){
			if(!first){
				builder.append(", ");
			}
			translate(groupingColumnReference, builder, columnNameToModelMap);
			first = false;
		}
	}

	/**
	 * Translate GroupingColumnReference
	 * 
	 * @param groupingColumnReference
	 * @param builder
	 * @param columnNameToModelMap
	 */
	static void translate(GroupingColumnReference groupingColumnReference, StringBuilder builder,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(groupingColumnReference == null) throw new IllegalArgumentException("GroupingColumnReference cannot be null");
		translate(groupingColumnReference.getColumnReference(), builder, columnNameToModelMap);
	}



	/**
	 * Translate WhereClause.
	 * 
	 * @param whereClause
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(WhereClause whereClause,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(whereClause == null) throw new IllegalArgumentException("WhereClause cannot be null");
		builder.append("WHERE ");
		translate(whereClause.getSearchCondition(), builder, parameters, columnNameToModelMap);
	}

	/**
	 * Translate a SearchCondition
	 * 
	 * @param searchCondition
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(SearchCondition searchCondition,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(searchCondition == null) throw new IllegalArgumentException("SearchCondition cannot be null");
		if(searchCondition.getOrBooleanTerms() == null) throw new IllegalArgumentException("BooleanTerms cannot be null");
		if(searchCondition.getOrBooleanTerms().size() < 1) throw new IllegalArgumentException("There must be at least one BooleanTerm");
		boolean first = true;
		for(BooleanTerm booleanTerm: searchCondition.getOrBooleanTerms()){
			if(!first){
				builder.append(" OR ");
			}
			translate(booleanTerm, builder, parameters, columnNameToModelMap);
			first = false;
		}
	}

	/**
	 * Translate BooleanTerm.
	 * 
	 * @param booleanTerm
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(BooleanTerm booleanTerm,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(booleanTerm == null) throw new IllegalArgumentException("BooleanTerm cannot be null");
		if(booleanTerm.getAndBooleanFactors() == null) throw new IllegalArgumentException("BooleanFactors cannot be null");
		if(booleanTerm.getAndBooleanFactors().size() < 1) throw new IllegalArgumentException("There must be at least one BooleanFactor");
		boolean first = true;
		for(BooleanFactor booleanFactor: booleanTerm.getAndBooleanFactors()){
			if(!first){
				builder.append(" AND ");
			}
			translate(booleanFactor, builder, parameters, columnNameToModelMap);
			first = false;
		}
	}

	/**
	 * Translate a BooleanFactor
	 * 
	 * @param booleanFactor
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(BooleanFactor booleanFactor,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(booleanFactor == null) throw new IllegalArgumentException("BooleanFactor cannot be null");
		if(booleanFactor.getNot() != null){
			builder.append("NOT");
		}
		translate(booleanFactor.getBooleanTest(), builder, parameters, columnNameToModelMap);
	}

	/**
	 * Translate a BooleanTest
	 * 
	 * @param booleanTest
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(BooleanTest booleanTest,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(booleanTest == null) throw new IllegalArgumentException("BooleanTest cannot be null");
		translate(booleanTest.getBooleanPrimary(), builder, parameters, columnNameToModelMap);
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
	 * 
	 * @param booleanPrimary
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(BooleanPrimary booleanPrimary,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(booleanPrimary == null) throw new IllegalArgumentException("BooleanPrimary cannot be null");
		if(booleanPrimary.getPredicate() != null){
			translate(booleanPrimary.getPredicate(), builder, parameters, columnNameToModelMap);
		}else{
			builder.append("(");
			translate(booleanPrimary.getSearchCondition(), builder, parameters, columnNameToModelMap);
			builder.append(")");
		}
	}

	/**
	 * Translate a Predicate
	 * 
	 * @param predicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(Predicate predicate, StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(predicate == null) throw new IllegalArgumentException("Predicate cannot be null");
		if(predicate.getComparisonPredicate() != null){
			translate(predicate.getComparisonPredicate(), builder, parameters, columnNameToModelMap);
		}else if(predicate.getInPredicate() != null){
			translate(predicate.getInPredicate(), builder, parameters, columnNameToModelMap);
		}else if(predicate.getBetweenPredicate() != null){
			translate(predicate.getBetweenPredicate(), builder, parameters, columnNameToModelMap);
		}else if(predicate.getLikePredicate() != null){
			translate(predicate.getLikePredicate(), builder, parameters, columnNameToModelMap);
		} else if (predicate.getIsPredicate() != null) {
			translate(predicate.getIsPredicate(), builder, parameters, columnNameToModelMap);
		}else{
			throw new IllegalArgumentException("Unknown Predicate type");
		}
	}
	
	/**
	 * Translate NullPredicate
	 * 
	 * @param nullPredicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(IsPredicate isPredicate,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if (isPredicate == null)
			throw new IllegalArgumentException("IsPredicate cannot be null");
		// RHS
		translate(isPredicate.getColumnReferenceLHS(), builder, columnNameToModelMap);
		builder.append(" IS ");
		if (isPredicate.getNot() != null) {
			builder.append("NOT ");
		}
		builder.append(isPredicate.getCompareValue());
	}

	/**
	 * Translate a LikePredicate
	 * 
	 * @param likePredicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(LikePredicate likePredicate,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(likePredicate == null) throw new IllegalArgumentException("LikePredicate cannot be null");
		// RHS
		translate(likePredicate.getColumnReferenceLHS(), builder, columnNameToModelMap);
		if(likePredicate.getNot() != null){
			builder.append(" NOT");
		}
		builder.append(" LIKE ");
		ValueExpressionPrimary patternPrimary = getValueExpressionPrimary(likePredicate.getPattern());
		translateRHS(patternPrimary, builder, parameters, null);
		if(likePredicate.getEscapeCharacter() != null){
			builder.append(" ESCAPE ");
			ValueExpressionPrimary escapePrimary = getValueExpressionPrimary(likePredicate.getEscapeCharacter());
			translateRHS(escapePrimary, builder, parameters, null);
		}
	}

	/**
	 * Translate BetweenPredicate.
	 * 
	 * @param betweenPredicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(BetweenPredicate betweenPredicate,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(betweenPredicate == null) throw new IllegalArgumentException("BetweenPredicate cannot be null");
		// RHS
		ColumnModel lhsColumnModel = translate(betweenPredicate.getColumnReferenceLHS(), builder, columnNameToModelMap);
		if(betweenPredicate.getNot() != null){
			builder.append(" NOT");
		}
		builder.append(" BETWEEN ");
		translate(betweenPredicate.getBetweenRowValueConstructor(), builder, parameters, lhsColumnModel);
		builder.append(" AND ");
		translate(betweenPredicate.getAndRowValueConstructorRHS(), builder, parameters, lhsColumnModel);
	}

	/**
	 * Translate ComparisonPredicate
	 * 
	 * @param comparisonPredicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(ComparisonPredicate comparisonPredicate,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(comparisonPredicate == null) throw new IllegalArgumentException("ComparisonPredicate cannot be null");
		if(comparisonPredicate.getCompOp() == null) throw new IllegalArgumentException("CompOp cannot be null");
		// first left-hand-side
		ColumnModel lhsColumnModel = translate(comparisonPredicate.getColumnReferenceLHS(), builder, columnNameToModelMap);
		builder.append(" ").append(comparisonPredicate.getCompOp().toSQL()).append(" ");
		translate(comparisonPredicate.getRowValueConstructorRHS(), builder, parameters, lhsColumnModel);
	}
	
	/**
	 * Translate an InPredicate
	 * 
	 * @param inPredicate
	 * @param builder
	 * @param parameters
	 * @param columnNameToModelMap
	 */
	static void translate(InPredicate inPredicate,
			StringBuilder builder, Map<String, Object> parameters,
			Map<String, ColumnModel> columnNameToModelMap) {
		if(inPredicate == null) throw new IllegalArgumentException("InPredicate cannot be null");
		ColumnModel lhsColumnModel = translate(inPredicate.getColumnReferenceLHS(), builder, columnNameToModelMap);
		builder.append(" IN (");
		translate(inPredicate.getInPredicateValue(), builder, parameters, lhsColumnModel);
		builder.append(")");
	}
	
	/**
	 * Translate an InPredicateValue
	 * @param inPredicateValue
	 * @param builder
	 * @param parameters
	 */
	static void translate(InPredicateValue inPredicateValue, StringBuilder builder, Map<String, Object> parameters, ColumnModel lhsColumnModel) {
		if(inPredicateValue == null) throw new IllegalArgumentException("InPredicateValue cannot be null");
		translate(inPredicateValue.getInValueList(), builder, parameters, lhsColumnModel);
	}

	/**
	 * Translate an InValueList
	 * @param inValueList
	 * @param builder
	 * @param parameters
	 */
	static void translate(InValueList inValueList, StringBuilder builder, Map<String, Object> parameters, ColumnModel lhsColumnModel) {
		if(inValueList == null) throw new IllegalArgumentException("InValueList cannot be null");
		if(inValueList.getValueExpressions() == null) throw new IllegalArgumentException("ValueExpressions cannot be null");
		if(inValueList.getValueExpressions().size() < 1) throw new IllegalArgumentException("There must be at least one ValueExpression");
		boolean first = true;
		for(ValueExpression valueExpresssion: inValueList.getValueExpressions()){
			if(!first){
				builder.append(", ");
			}
			translateRHS(valueExpresssion, builder, parameters, lhsColumnModel);
			first = false;
		}
	}

	/**
	 * Translate RowValueConstructor
	 * @param rowValueConstructor
	 * @param builder
	 * @param parameters
	 */
	static void translate(RowValueConstructor rowValueConstructor, StringBuilder builder, Map<String, Object> parameters,
			ColumnModel lhsColumnModel) {
		// Put the value in the map.
		if(rowValueConstructor == null) throw new IllegalArgumentException("RowValueConstructor cannot be null");
		if(rowValueConstructor.getRowValueConstructorElement() != null){
			translate(rowValueConstructor.getRowValueConstructorElement(), builder, parameters, lhsColumnModel);
		}
	}
	
	/**
	 * Translate RowValueConstructorElement.
	 * 
	 * @param rowValueConstructorElement
	 * @param builder
	 * @param parameters
	 */
	static void translate(RowValueConstructorElement rowValueConstructorElement, StringBuilder builder, Map<String, Object> parameters,
			ColumnModel lhsColumnModel) {
		if(rowValueConstructorElement == null) throw new IllegalArgumentException("RowValueConstructorElement cannot be null");
		if(rowValueConstructorElement.getNullSpecification() != null){
			builder.append("NULL");
		}else if(rowValueConstructorElement.getDefaultSpecification() != null){
			builder.append("DEFAULT");
		} else if (rowValueConstructorElement.getTruthSpecification() != null) {
			builder.append(rowValueConstructorElement.getTruthSpecification().name());
		}else{
			translateRHS(rowValueConstructorElement.getValueExpression(), builder, parameters, lhsColumnModel);
		}
	}
	
	/**
	 * Translate a ValueExpression that appears on the right-hand-side of a predicate.
	 * @param valueExpression
	 * @param builder
	 * @param parameters
	 */
	static void translateRHS(ValueExpression valueExpression, StringBuilder builder, Map<String, Object> parameters,
			ColumnModel lhsColumnModel) {
		if(valueExpression == null) throw new IllegalArgumentException("ValueExpression cannot be null");
		// To primary
		ValueExpressionPrimary primary = getValueExpressionPrimary(valueExpression);
		translateRHS(primary, builder, parameters, lhsColumnModel);
	}
	
	/**
	 * Translate a ValueExpression that appears on the right-hand-side of a predicate.
	 * @param valueExpression
	 * @param builder
	 * @param parameters
	 */
	static void translateRHS(ValueExpressionPrimary primary, StringBuilder builder, Map<String, Object> parameters, ColumnModel lhsColumnModel) {
		if(primary == null) throw new IllegalArgumentException("ValueExpression cannot be null");
		// The bind key is used in the SQL and parameter map.
		String bindKey = "b"+parameters.size();
		String value = getStringValueOf(primary.getUnsignedValueSpecification());
		switch (lhsColumnModel == null ? ColumnType.STRING : lhsColumnModel.getColumnType()) {
		case DATE:
			if (!isNumber(primary.getUnsignedValueSpecification())) {
				DateTime parsedDateTime = dateParser.parseDateTime(value);
				value = Long.toString(parsedDateTime.getMillis());
			}
			break;
		default:
			break;
		}
		builder.append(":").append(bindKey);
		parameters.put(bindKey, value);
	}

	/**
	 * Get the string value from UnsignedValueSpecification
	 * @param unsignedValueSpecification
	 * @return
	 */
	public static String getStringValueOf(UnsignedValueSpecification unsignedValueSpecification) {
		if(unsignedValueSpecification == null) throw new IllegalArgumentException("UnsignedValueSpecification cannot be null");
		return getStringValueOf(unsignedValueSpecification.getUnsignedLiteral());
	}

	/**
	 * Get the string value from UnsignedLiteral
	 * @param unsignedLiteral
	 * @return
	 */
	public static String getStringValueOf(UnsignedLiteral unsignedLiteral) {
		if(unsignedLiteral.getGeneralLiteral() != null) return unsignedLiteral.getGeneralLiteral();
		if(unsignedLiteral.getUnsignedNumericLiteral() != null) return unsignedLiteral.getUnsignedNumericLiteral();
		throw new IllegalArgumentException("UnsignedLiteral must have either a GeneralLiteral or UnsignedNumericLiteral");
	}
	
	/**
	 * Is the UnsignedValueSpecification a number
	 * 
	 * @param unsignedValueSpecification
	 * @return
	 */
	public static boolean isNumber(UnsignedValueSpecification unsignedValueSpecification) {
		if (unsignedValueSpecification == null)
			throw new IllegalArgumentException("UnsignedValueSpecification cannot be null");
		return isNumber(unsignedValueSpecification.getUnsignedLiteral());
	}

	/**
	 * Is the UnsignedValueSpecification a number
	 * 
	 * @param unsignedLiteral
	 * @return
	 */
	public static boolean isNumber(UnsignedLiteral unsignedLiteral) {
		if (unsignedLiteral.getUnsignedNumericLiteral() != null) {
			if (unsignedLiteral.getGeneralLiteral() != null) {
				throw new IllegalArgumentException("UnsignedLiteral must have either a GeneralLiteral or UnsignedNumericLiteral but not both");
			}
			return true;
		}
		return false;
	}

	/**
	 * Get the list of column IDs that are referenced in the select calsue.
	 * 
	 * @param allColumns
	 * @param selectList
	 * @return
	 */
	public static List<Long> getSelectColumns(SelectList selectList, Map<String, ColumnModel> columnNameToModelMap) {
		if (columnNameToModelMap == null)
			throw new IllegalArgumentException("All columns cannot be null");
		if(selectList == null) throw new IllegalArgumentException();
		if(selectList.getAsterisk() != null){
			// All of the columns will be returned.
			return Transform.toList(columnNameToModelMap.values(), MODEL_TO_ID);
		}else{
			List<Long> selectIds = new LinkedList<Long>();
			for(DerivedColumn dc: selectList.getColumns()){
				ValueExpressionPrimary primary = getValueExpressionPrimary(dc.getValueExpression());
				if(primary.getColumnReference() != null){
					String key = getStringValueOf(primary.getColumnReference().getNameLHS());
					ColumnModel column = columnNameToModelMap.get(key);
					if (column != null) {
						selectIds.add(MODEL_TO_ID.apply(column));
					}
				}
			}
			return selectIds;
		}
	}
}
