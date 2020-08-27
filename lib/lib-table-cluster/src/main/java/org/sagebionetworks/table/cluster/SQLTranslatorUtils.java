package org.sagebionetworks.table.cluster;

import com.google.common.collect.Lists;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReferenceLookup;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ArrayFunctionSpecification;
import org.sagebionetworks.table.query.model.ArrayFunctionType;
import org.sagebionetworks.table.query.model.ArrayHasPredicate;
import org.sagebionetworks.table.query.model.BacktickDelimitedIdentifier;
import org.sagebionetworks.table.query.model.BooleanFunctionPredicate;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnNameReference;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.CurrentUserFunction;
import org.sagebionetworks.table.query.model.DelimitedIdentifier;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.ExactNumericLiteral;
import org.sagebionetworks.table.query.model.FromClause;
import org.sagebionetworks.table.query.model.FunctionReturnType;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.HasFunctionReturnType;
import org.sagebionetworks.table.query.model.HasPredicate;
import org.sagebionetworks.table.query.model.HasReferencedColumn;
import org.sagebionetworks.table.query.model.HasReplaceableChildren;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.IntervalLiteral;
import org.sagebionetworks.table.query.model.JoinCondition;
import org.sagebionetworks.table.query.model.JoinType;
import org.sagebionetworks.table.query.model.NumericValueFunction;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.OuterJoinType;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QualifiedJoin;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.RegularIdentifier;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.StringOverride;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.TableName;
import org.sagebionetworks.table.query.model.TableReference;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.model.UnsignedNumericLiteral;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.ValidateArgument;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

/**
 * Helper methods to translate user generated queries
 * to queries that run against the actual database.
 *
 */
public class SQLTranslatorUtils {

	private static final String COLON = ":";
	public static final String BIND_PREFIX = "b";


	/**
	 * Get the list of column IDs that are referenced in the select clause.
	 * 
	 * @param selectList
	 * @param columnTranslationReferenceLookup
	 * @param isAggregate
	 * @return
	 */
	public static List<SelectColumn> getSelectColumns(SelectList selectList, ColumnTranslationReferenceLookup columnTranslationReferenceLookup, boolean isAggregate) {
		ValidateArgument.required(columnTranslationReferenceLookup, "all columns");
		ValidateArgument.required(selectList, "selectList");
		if (selectList.getAsterisk() != null) {
			throw new IllegalStateException("The columns should have been expanded before getting here");
		} 
		List<SelectColumn> selects = Lists.newArrayListWithCapacity(selectList.getColumns().size());
		boolean isAtLeastOneColumnIdNull = false;
		for (DerivedColumn dc : selectList.getColumns()) {
			SelectColumn model = getSelectColumns(dc, columnTranslationReferenceLookup);
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
	 * Given a DerivedColumn extract all data about both the SelectColumn and ColumnModel.
	 * 
	 * @param derivedColumn
	 * @param columnTranslationReferenceLookup
	 * @return
	 */
	public static SelectColumn getSelectColumns(DerivedColumn derivedColumn, ColumnTranslationReferenceLookup columnTranslationReferenceLookup){
		// Extract data about this column.
		String displayName = derivedColumn.getDisplayName();
		// lookup the column referenced by this select.
		ColumnNameReference referencedColumn = derivedColumn.getReferencedColumn();
		// Select defines the selection
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName(displayName);
		
		ColumnTranslationReference translationReference = null;
		if(referencedColumn != null){
			// Does the reference match an actual column name?
			translationReference = columnTranslationReferenceLookup.forUserQueryColumnName(referencedColumn.toSqlWithoutQuotes()).orElse(null);
		}
		// Lookup the base type starting only with the column referenced.
		ColumnType columnType = getBaseColulmnType(referencedColumn);
		if(translationReference != null){
			// If we have a column model the base type is defined by it.
			columnType = translationReference.getColumnType();

			// We only set the id on the select column when the display name match the column name.
			if(translationReference.getUserQueryColumnName().equals(displayName)
					//must be a column defined in the schema, not a metadata column
					&& translationReference instanceof SchemaColumnTranslationReference){
				selectColumn.setId(((SchemaColumnTranslationReference) translationReference).getId());
			}
		}
		FunctionReturnType functionReturnType = null;
		// if this is a function it will have a return type
		HasFunctionReturnType hasReturnType = derivedColumn.getFirstElementOfType(HasFunctionReturnType.class);
		if(hasReturnType != null){
			functionReturnType = hasReturnType.getFunctionReturnType();
			if(functionReturnType != null) {
				columnType = functionReturnType.getColumnType(columnType);
			}
		}
		selectColumn.setColumnType(columnType);
		validateSelectColumn(selectColumn, functionReturnType, translationReference, referencedColumn);
		// done
		return selectColumn;
	}
	
	public static void validateSelectColumn(SelectColumn selectColumn, FunctionReturnType functionReturnType,
											ColumnTranslationReference columnTranslationReference, ColumnNameReference referencedColumn) {
		ValidateArgument.requirement(columnTranslationReference != null
				|| functionReturnType != null
				|| (referencedColumn instanceof UnsignedLiteral),
				"Unknown column "+selectColumn.getName());
	}

	/**
	 * Given a referenced column, attempt to determine the type of the column using only
	 * the SQL.
	 * @param referencedColumn
	 * @return
	 */
	public static ColumnType getBaseColulmnType(ColumnNameReference referencedColumn){
		if(referencedColumn == null){
			return null;
		}
		// Get the upper case column name without quotes.
		if(!referencedColumn.hasQuotesRecursive()){
			return ColumnType.DOUBLE;
		}
		return ColumnType.STRING;
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
		case SUBMISSIONID:
		case EVALUATIONID:
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
	public static SelectList addMetadataColumnsToSelect(SelectList selectList, boolean includeEtag){
		List<DerivedColumn> selectColumns = Lists.newArrayListWithCapacity(selectList.getColumns().size() + 2);
		selectColumns.addAll(selectList.getColumns());
		selectColumns.add(SqlElementUntils.createNonQuotedDerivedColumn(ROW_ID));
		selectColumns.add(SqlElementUntils.createNonQuotedDerivedColumn(ROW_VERSION));
		if(includeEtag){
			selectColumns.add(SqlElementUntils.createNonQuotedDerivedColumn(ROW_ETAG));
		}
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
	 * @param includesRowIdAndVersion Is ROW_ID and ROW_VERSION included in the result set?
	 * @param includeEtag Is the read row an EntityRow?
	 * @return
	 * @throws SQLException
	 */
	public static Row readRow(ResultSet rs, boolean includesRowIdAndVersion, boolean includeEtag, ColumnTypeInfo[] colunTypes) throws SQLException{
		Row row = new Row();
		List<String> values = new LinkedList<String>();
		row.setValues(values);
		if(includesRowIdAndVersion){
			row.setRowId(rs.getLong(ROW_ID));
			row.setVersionNumber(rs.getLong(ROW_VERSION));
			if(includeEtag){
				row.setEtag(rs.getString(ROW_ETAG));
			}
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
	 * @param columnTranslationReferenceLookup
	 */
	public static void translateModel(QuerySpecification transformedModel,
			Map<String, Object> parameters,
		  ColumnTranslationReferenceLookup columnTranslationReferenceLookup, Long userId) {

		translateSynapseFunctions(transformedModel, userId);

		// Select columns
		Iterable<HasReferencedColumn> selectColumns = transformedModel.getSelectList().createIterable(HasReferencedColumn.class);
		for(HasReferencedColumn hasReference: selectColumns){
			translateSelect(hasReference, columnTranslationReferenceLookup);
		}
		
		TableExpression tableExpression = transformedModel.getTableExpression();
		if(tableExpression == null){
			// nothing else to do.
			return;
		}

		//save the original syn### id since we will need it for any HAS predicates
		IdAndVersion originalSynId = translate(tableExpression.getFromClause());

		// Translate where
		WhereClause whereClause = tableExpression.getWhereClause();

		if(whereClause != null) {
			// Translate all predicates
			Iterable<HasPredicate> hasPredicates = whereClause.createIterable(HasPredicate.class);
			for (HasPredicate predicate : hasPredicates) {
				translate(predicate, parameters, columnTranslationReferenceLookup);
			}

			for (BooleanPrimary booleanPrimary : whereClause.createIterable(BooleanPrimary.class)) {
				replaceBooleanFunction(booleanPrimary, columnTranslationReferenceLookup);
				replaceArrayHasPredicate(booleanPrimary, columnTranslationReferenceLookup, originalSynId);
			}
		}
		// translate the group by
		GroupByClause groupByClause = tableExpression.getGroupByClause();
		if(groupByClause != null){
			translate(groupByClause, columnTranslationReferenceLookup);
		}
		// translate the order by
		OrderByClause orderByClause = tableExpression.getOrderByClause();
		if(orderByClause != null){
			Iterable<HasReferencedColumn> orderByReferences = orderByClause.createIterable(HasReferencedColumn.class);
			for(HasReferencedColumn hasReference: orderByReferences){
				translateOrderBy(hasReference, columnTranslationReferenceLookup);
			}
		}
		// translate Pagination
		Pagination pagination = tableExpression.getPagination();
		if(pagination != null){
			translate(pagination, parameters);
		}

		//handle array functions which requires appending a join on another table
		try {
			translateArrayFunctions(transformedModel, columnTranslationReferenceLookup, originalSynId);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}

		/*
		 *  By this point anything all remaining DelimitedIdentifier should be treated as a column
		 *  reference and therefore should be enclosed in backticks.
		 */
		translateUnresolvedDelimitedIdentifiers(transformedModel);
	}


	/**
	 * Translates FROM clause and returns the original Synapse IdAndVersion that was translated
	 * @param fromClause
	 * @return
	 */
	static IdAndVersion translate(FromClause fromClause) {
		IdAndVersion originalSynId = IdAndVersion.parse(fromClause.getTableReference().getTableName());
		//replace from clause
		fromClause.setTableReference(tableReferenceForName(SQLUtils.getTableNameForId(originalSynId, TableType.INDEX)));
		return originalSynId;
	}

	static void translateArrayFunctions(QuerySpecification transformedModel, ColumnTranslationReferenceLookup lookup, IdAndVersion idAndVersion) throws ParseException {
		// UNNEST(columnName) for the same columnName
		// may appear in multiple places (select clause ,group by, order by, etc.)
		// but should only join the unnested index table for that column once
		Set<String> columnIdsToJoin = new HashSet<>();

		// iterate over all ValueExpressionPrimary since its may hold a ArrayFunctionSpecification.
		// Its held element then may need to be replaced with a different child element.
		for(ValueExpressionPrimary valueExpressionPrimary : transformedModel.createIterable(ValueExpressionPrimary.class)){
			//ignore valueExpressionPrimary that don't use an ArrayFunctionSpecification
			if(!(valueExpressionPrimary.getChild() instanceof ArrayFunctionSpecification)){
				continue;
			}
			ArrayFunctionSpecification arrayFunctionSpecification = (ArrayFunctionSpecification) valueExpressionPrimary.getChild();

			//handle UNNEST() functions
			if(arrayFunctionSpecification.getListFunctionType() == ArrayFunctionType.UNNEST){
				ColumnReference referencedColumn = arrayFunctionSpecification.getColumnReference();

				SchemaColumnTranslationReference columnTranslationReference = lookupAndRequireListColumn(lookup, referencedColumn.toSqlWithoutQuotes(), "UNNEST()");

				//add column id to be joined
				columnIdsToJoin.add(columnTranslationReference.getId());

				//replace "UNNEST(_C123_)" with column "_C123__UNNEST"
				ColumnReference replacementColumn = SqlElementUntils.createColumnReference(
						SQLUtils.getUnnestedColumnNameForId(columnTranslationReference.getId())
				);
				valueExpressionPrimary.replaceChildren(replacementColumn);
			}
		}

		appendJoinsToFromClause(idAndVersion, transformedModel.getTableExpression().getFromClause(), columnIdsToJoin);
	}

	private static void appendJoinsToFromClause(IdAndVersion idAndVersion, FromClause fromClause, Set<String> columnIdsToJoin) throws ParseException {
		TableReference currentTableReference = fromClause.getTableReference();
		String mainTableName = currentTableReference.toSql();

		//chain additional tables to join via right-recursion
		for(String columnId : columnIdsToJoin){
			String joinTableName = SQLUtils.getTableNameForMultiValueColumnIndex(idAndVersion, columnId);

			TableReference joinedTableRef = tableReferenceForName(joinTableName);
			JoinCondition joinOnRowId = new JoinCondition(new TableQueryParser(
				mainTableName + "." + ROW_ID + "=" + joinTableName + "." + SQLUtils.getRowIdRefColumnNameForId(columnId)
			).searchCondition());
			JoinType leftOuterJoin = new JoinType(OuterJoinType.LEFT);
			currentTableReference = new TableReference(new QualifiedJoin(
					currentTableReference, leftOuterJoin, joinedTableRef, joinOnRowId
			));
		}
		fromClause.setTableReference(currentTableReference);
	}

	/**
	 * Any DelimitedIdentifier remaining in the query after translation should be
	 * treated as a column reference, which for MySQL, means the value must be
	 * within backticks. Therefore, this function will translate any
	 * DoubleQuoteDelimitedIdentifier into a BacktickDelimitedIdentifier. any
	 * 
	 * @param element
	 */
	public static void translateUnresolvedDelimitedIdentifiers(Element element) {
		Iterable<DelimitedIdentifier> delimitedIdentifierIt = element.createIterable(DelimitedIdentifier.class);
		for(DelimitedIdentifier identifier: delimitedIdentifierIt) {
			String value = identifier.toSqlWithoutQuotes();
			identifier.replaceChildren(new BacktickDelimitedIdentifier(value));
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
	 * @param columnTranslationReferenceLookup
	 */
	public static void translate(GroupByClause groupByClause,
			ColumnTranslationReferenceLookup columnTranslationReferenceLookup) {
		ValidateArgument.required(groupByClause, "groupByClause");
		ValidateArgument.required(columnTranslationReferenceLookup, "columnTranslationReferenceLookup");
		Iterable<ColumnName> references = groupByClause.createIterable(ColumnName.class);
		for(ColumnName reference: references){
			// Lookup the column
			columnTranslationReferenceLookup.forUserQueryColumnName(reference.toSqlWithoutQuotes()).ifPresent(
					(ColumnTranslationReference columnTranslationReference) ->{
						reference.replaceChildren(new RegularIdentifier(columnTranslationReference.getTranslatedColumnName()));
					}
			);
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
	 * @param columnTranslationReferenceLookup
	 */
	public static void translate(HasPredicate predicate,
			Map<String, Object> parameters,
			ColumnTranslationReferenceLookup columnTranslationReferenceLookup) {
		ValidateArgument.required(predicate, "predicate");
		ValidateArgument.required(parameters, "parameters");
		ValidateArgument.required(columnTranslationReferenceLookup, "columnTranslationReferenceLookup");
		// lookup the column name from the left-hand-side
		String columnName = predicate.getLeftHandSide().toSqlWithoutQuotes();

		ColumnTranslationReference columnTranslationReference = columnTranslationReferenceLookup.forUserQueryColumnName(columnName)
				.orElseThrow(() ->  new IllegalArgumentException("Column does not exist: " + columnName) );

		// handle the right-hand-side values
		Iterable<UnsignedLiteral> rightHandSide = predicate.getRightHandSideValues();
		if(rightHandSide != null){
			ColumnType columnType = columnTranslationReference.getColumnType();
			//for the ArrayHasPredicate, we want its corresponding non-list type to be used
			if(predicate instanceof ArrayHasPredicate && ColumnTypeListMappings.isList(columnType)){
				columnType = ColumnTypeListMappings.nonListType(columnType);
			}
			for(UnsignedLiteral element: rightHandSide){
				translateRightHandeSide(element, columnType, parameters);
			}
		}
		
		// replace all column names in the predicate
		Iterable<ColumnName> columnNameReferences = predicate.createIterable(ColumnName.class);
		for (ColumnName columnNameRef : columnNameReferences) {
			String refColumnName = columnNameRef.toSqlWithoutQuotes();

			// is this a reference to a column?
			columnTranslationReferenceLookup.forUserQueryColumnName(refColumnName)
				.ifPresent((ColumnTranslationReference referencedColumn) ->{
					columnNameRef.replaceChildren(new RegularIdentifier(referencedColumn.getTranslatedColumnName()));
				});
		}
	}

	/**
	 * Translate instances of Synapse functions not supported by SQL
	 *
	 * @param transformedModel
	 * @param userId
	 */
	public static void translateSynapseFunctions(QuerySpecification transformedModel, Long userId){
		// Insert userId if needed
		Iterable<NumericValueFunction> hasUser = transformedModel.createIterable(NumericValueFunction.class);
		for(NumericValueFunction pred: hasUser){
			if(pred.getChild() instanceof CurrentUserFunction){
				// UnsignedLiterals are needed in order for the value to be bound as a parameter
				pred.replaceChildren(new UnsignedLiteral(new UnsignedNumericLiteral(new ExactNumericLiteral(userId))));
			}
		}
	}

	/**
	 * Translate the right-hand-side of a predicate.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param element
	 * @param type
	 * @param parameters
	 */
	public static void translateRightHandeSide(HasReplaceableChildren element,
			ColumnType type, Map<String, Object> parameters) {
		ValidateArgument.required(element, "element");
		ValidateArgument.required(type, "type");
		ValidateArgument.required(parameters, "parameters");
		if(element.getFirstElementOfType(IntervalLiteral.class) != null){
			// intervals should not be replaced.
			return;
		}
		
		String key = BIND_PREFIX+parameters.size();
		String value = element.toSqlWithoutQuotes();
		Object valueObject = null;
		try{
			valueObject = SQLUtils.parseValueForDB(type, value);
		}catch (IllegalArgumentException e){
			// thrown for number format exception.
			valueObject = value;
		}

		parameters.put(key, valueObject);
		element.replaceChildren(new StringOverride(COLON+key));
	}

	/**
	 * Replace any BooleanFunctionPredicate with a search condition.
	 * For example: 'isInfinity(DOUBLETYPE)' will be replaced with (_DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ('-Infinity', 'Infinity'))'
	 * @param booleanPrimary
	 * @param columnTranslationReferenceLookup
	 */
	public static void replaceBooleanFunction(BooleanPrimary booleanPrimary, ColumnTranslationReferenceLookup columnTranslationReferenceLookup){
		if(booleanPrimary.getPredicate() != null){
			BooleanFunctionPredicate bfp = booleanPrimary.getPredicate().getFirstElementOfType(BooleanFunctionPredicate.class);
			if(bfp != null){
				String columnName = bfp.getColumnReference().toSqlWithoutQuotes();
				ColumnTranslationReference columnTranslationReference = columnTranslationReferenceLookup.forTranslatedColumnName(columnName)
						.orElseThrow(() -> new IllegalArgumentException("Function: "+bfp.getBooleanFunction()+" has unknown reference: "+columnName));

				if( !(columnTranslationReference instanceof SchemaColumnTranslationReference) ){
					throw new IllegalArgumentException("(double boolean-functions can only be used on columns defined in the schema");
				}
				if(columnTranslationReference.getColumnType() != ColumnType.DOUBLE){
					throw new IllegalArgumentException("Function: "+bfp.getBooleanFunction()+" can only be used with a column of type DOUBLE.");
				}

				StringBuilder builder = new StringBuilder();
				// Is this a boolean function
				switch(bfp.getBooleanFunction()){
				case ISINFINITY:
					SQLUtils.appendIsInfinity(((SchemaColumnTranslationReference) columnTranslationReference).getId(), builder);
					break;
				case ISNAN:
					SQLUtils.appendIsNan(((SchemaColumnTranslationReference) columnTranslationReference).getId(), builder);
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
	}

	public static void replaceArrayHasPredicate(BooleanPrimary booleanPrimary, ColumnTranslationReferenceLookup columnTranslationReferenceLookup, IdAndVersion idAndVersion){
		if(booleanPrimary.getPredicate() == null) {
			return; // "HAS" should always be under a Predicate
		}
		ArrayHasPredicate arrayHasPredicate = booleanPrimary.getPredicate().getFirstElementOfType(ArrayHasPredicate.class);
		if (arrayHasPredicate == null) {
			return; // no ArrayHasPredicate to replace
		}

		String columnName = arrayHasPredicate.getLeftHandSide().toSqlWithoutQuotes();

		SchemaColumnTranslationReference schemaColumnTranslationReference = lookupAndRequireListColumn(columnTranslationReferenceLookup, columnName, "The HAS keyword");

		//build up subquery against the flattened index table
		String columnFlattenedIndexTable = SQLUtils.getTableNameForMultiValueColumnIndex(idAndVersion, schemaColumnTranslationReference.getId());
		try {
			QuerySpecification subquery = TableQueryParser.parserQuery("SELECT " + SQLUtils.getRowIdRefColumnNameForId(schemaColumnTranslationReference.getId()) +
					" FROM " + columnFlattenedIndexTable +
					" WHERE "
					//use a placeholder predicate because the colons in bind variables (e.g. ":b1") are not accepted by the parser
					+ " placeholder IN ( placeholder )");

			//create a "IN" predicate that has the same right hand side as the "HAS" predicate for the subquery
			ColumnReference unnestedColumn = SqlElementUntils.createColumnReference(SQLUtils.getUnnestedColumnNameForId(schemaColumnTranslationReference.getId()));
			InPredicate subqueryInPredicate = new InPredicate(unnestedColumn, null, arrayHasPredicate.getInPredicateValue());
			subquery.getFirstElementOfType(Predicate.class).replaceChildren(subqueryInPredicate);

			//replace the "HAS" with "IN" predicate containing the subquery
			Predicate replacementPredicate = new Predicate(new InPredicate(
					SqlElementUntils.createColumnReference(ROW_ID),
					arrayHasPredicate.getNot(),
					new InPredicateValue(subquery)));

			booleanPrimary.getPredicate().replaceChildren(replacementPredicate);
		}catch (ParseException e){
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Translate a HasReferencedColumn for the select clause.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param column
	 * @param columnTranslationReferenceLookup
	 */
	public static void translateSelect(HasReferencedColumn column,
			ColumnTranslationReferenceLookup columnTranslationReferenceLookup) {
		ColumnNameReference columnNameReference = column.getReferencedColumn();
		if(columnNameReference != null){
			String unquotedName = columnNameReference.toSqlWithoutQuotes();
			columnTranslationReferenceLookup.forUserQueryColumnName(unquotedName)
				.ifPresent((ColumnTranslationReference translationReference) -> {
					String newName;
					if((translationReference instanceof SchemaColumnTranslationReference) && !column.isReferenceInFunction() && ColumnType.DOUBLE.equals(translationReference.getColumnType())){
						// non-function doubles are translated into a switch between the enum an double column.
						newName = SQLUtils.createDoubleCase(((SchemaColumnTranslationReference) translationReference).getId());
					}else{
						newName = translationReference.getTranslatedColumnName();
					}
					columnNameReference.replaceChildren(new RegularIdentifier(newName));
				}
			);
		}
	}
	
	/**
	 * Translate HasReferencedColumn for order by clause.
	 * 
	 * Translate user generated queries to queries that can
	 * run against the actual database.
	 * 
	 * @param column
	 * @param columnTranslationReferenceLookup
	 */
	public static void translateOrderBy(HasReferencedColumn column,
			ColumnTranslationReferenceLookup columnTranslationReferenceLookup) {
		ColumnNameReference columnNameReference = column.getReferencedColumn();
		if(columnNameReference != null){
			String unquotedName = columnNameReference.toSqlWithoutQuotes();
			columnTranslationReferenceLookup.forUserQueryColumnName(unquotedName).ifPresent(
					(ColumnTranslationReference columnTranslationReference) -> {
						columnNameReference.replaceChildren(new RegularIdentifier(columnTranslationReference.getTranslatedColumnName()));
					}
			);
		}
	}

	/**
	 * Wraps string table name inside a TableReference
	 * @param tableName
	 * @return
	 */
	private static TableReference tableReferenceForName(String tableName){
		return new TableReference(new TableName(new RegularIdentifier(tableName)));
	}

	/**
	 *
	 * @param columnTranslationReferenceLookup lookup table for ColumnTranslationReferences
	 * @param columnName column name for which to
	 * @param errorMessageFunctionName name of the function that requires a list column type
	 * @throws IllegalArgumentException if the column is not defined in the schema or does not have a _LIST ColumnType
	 * @return SchemaColumnTranslationReference associated with the columnName
	 */
	private static SchemaColumnTranslationReference lookupAndRequireListColumn(ColumnTranslationReferenceLookup columnTranslationReferenceLookup, String columnName, String errorMessageFunctionName){
		ColumnTranslationReference columnTranslationReference = columnTranslationReferenceLookup.forTranslatedColumnName(columnName)
				.orElseThrow(() ->  new IllegalArgumentException("Unknown column reference: " + columnName));
		if( !(columnTranslationReference instanceof SchemaColumnTranslationReference) ){
			throw new IllegalArgumentException(errorMessageFunctionName + " may only be used on columns defined in the schema");
		}
		SchemaColumnTranslationReference schemaColumnTranslationReference = (SchemaColumnTranslationReference) columnTranslationReference;

		if( !ColumnTypeListMappings.isList(columnTranslationReference.getColumnType()) ){
			throw new IllegalArgumentException(errorMessageFunctionName + " only works for columns that hold list values");
		}
		return schemaColumnTranslationReference;
	}

	public static String translateQueryFilters(List<QueryFilter> additionalFilters){
		ValidateArgument.requiredNotEmpty(additionalFilters, "additionalFilters");

		StringBuilder additionalSearchConditionBuilder = new StringBuilder();

		boolean firstVal = true;

		for(QueryFilter filter : additionalFilters){
			if(!firstVal){
				additionalSearchConditionBuilder.append(" AND ");
			}
			translateQueryFilters(additionalSearchConditionBuilder, filter);
			firstVal=false;
		}

		return additionalSearchConditionBuilder.toString();
	}

	static void translateQueryFilters(StringBuilder builder, QueryFilter filter){
		if(filter instanceof ColumnSingleValueQueryFilter){
			translateSingleValueFilters(builder, (ColumnSingleValueQueryFilter) filter);
		}else{
			throw new IllegalArgumentException("Unknown QueryFilter type");
		}
	}

	static void translateSingleValueFilters(StringBuilder builder, ColumnSingleValueQueryFilter filter){
		ValidateArgument.required(filter.getOperator(), "ColumnSingleValueQueryFilter.operator");
		switch (filter.getOperator()){
			case LIKE:
				appendLikeFilter(builder, filter);
				break;
			default:
				throw new IllegalArgumentException("Unexpected operator: " + filter.getOperator());
		}
	}

	static void appendLikeFilter(StringBuilder builder, ColumnSingleValueQueryFilter filter){
		ValidateArgument.requiredNotEmpty(filter.getColumnName(), "ColumnSingleValueQueryFilter.columnName");
		ValidateArgument.requiredNotEmpty(filter.getValues(), "ColumnSingleValueQueryFilter.likeValues");

		builder.append("(");
		boolean firstVal = true;
		String columnName = filter.getColumnName();
		for (String likeValue: filter.getValues()){
			if(!firstVal){
				builder.append(" OR ");
			}
			builder.append("\"")
					.append(columnName)
					.append("\"")
					.append(" LIKE ");
			appendSingleQuotedValueToStringBuilder(builder, likeValue);

			firstVal = false;
		}
		builder.append(")");
	}

	/**
	 * Appends a value to the string builder
	 * and places single quotes (') around it if the column type is String
	 */
	static void appendSingleQuotedValueToStringBuilder(StringBuilder builder, String value){
		builder.append("'");
		builder.append(value.replaceAll("'", "''"));
		builder.append("'");
	}
}
