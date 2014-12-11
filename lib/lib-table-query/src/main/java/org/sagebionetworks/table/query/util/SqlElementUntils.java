package org.sagebionetworks.table.query.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.*;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Utilities for creating SQL elements
 * 
 * @author John
 *
 */
public class SqlElementUntils {
	
	/**
	 * Create a value expression from an input SQL.
	 * @param input
	 * @return
	 * @throws ParseException
	 */
	public static ValueExpression createValueExpression(String sql) throws ParseException{
		return new TableQueryParser(sql).valueExpression();
	}
	
	/**
	 * Create a list of value expressions, one for each SQL passed.
	 * @param sqls
	 * @return
	 * @throws ParseException 
	 */
	public static List<ValueExpression> createValueExpressions(String...sqls) throws ParseException {
		List<ValueExpression> list = new LinkedList<ValueExpression>();
		for(String sql: sqls){
			list.add(createValueExpression(sql));
		}
		return list;
	}
	/**
	 * Create a derived column form input SQL.
	 * @param input
	 * @return
	 * @throws ParseException
	 */
	public static DerivedColumn createDerivedColumn(String sql) throws ParseException{
		return new TableQueryParser(sql).derivedColumn();
	}
	
	/**
	 * Create a list of DerivedColumns, one for each input SQL.
	 * @param inputs
	 * @return
	 * @throws ParseException
	 */
	public static List<DerivedColumn> createDerivedColumns(String...sql) throws ParseException{
		List<DerivedColumn> list = new LinkedList<DerivedColumn>();
		for(String input: sql){
			list.add(createDerivedColumn(input));
		}
		return list;
	}

	/**
	 * Create a TableExpression element from an input SQL.
	 * 
	 * @param input
	 * @return
	 * @throws ParseException
	 */
	public static TableExpression createTableExpression(String sql) throws ParseException {
		return new TableQueryParser(sql).tableExpression();
	}
	
	/**
	 * Create a boolean term from an input SQL.
	 * 
	 * @param input
	 * @return
	 * @throws ParseException
	 */
	public static BooleanTerm createBooleanTerm(String sql) throws ParseException{
		return new TableQueryParser(sql).booleanTerm();
	}
	
	/**
	 * Create a list of boolean terms, one for each input SQL.
	 * @param inputs
	 * @return
	 * @throws ParseException 
	 */
	public static List<BooleanTerm> createBooleanTerms(String...inputs) throws ParseException{
		List<BooleanTerm> list = new LinkedList<BooleanTerm>();
		for(String input: inputs){
			list.add(createBooleanTerm(input));
		}
		return list;
	}
	
	/**
	 * Create a BooleanFactor from input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static BooleanFactor createBooleanFactor(String sql) throws ParseException{
		return new TableQueryParser(sql).booleanFactor();
	}
	
	/**
	 * Create a list of boolean factors, one for each input SQL.
	 * @param inputs
	 * @return
	 * @throws ParseException
	 */
	public static List<BooleanFactor> createBooleanFactors(String...inputs) throws ParseException{
		List<BooleanFactor> list = new LinkedList<BooleanFactor>();
		for(String input: inputs){
			list.add(createBooleanFactor(input));
		}
		return list;
	}

	/**
	 * Create a boolean test from input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static BooleanTest createBooleanTest(String sql) throws ParseException {
		return new TableQueryParser(sql).booleanTest();
	}

	/**
	 * Create a boolean primary from input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static BooleanPrimary createBooleanPrimary(String sql) throws ParseException {
		return new TableQueryParser(sql).booleanPrimary();
	}

	/**
	 * Create a predicate from input SQL.
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static Predicate createPredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate();
	}

	/**
	 * Create a search condition from input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static SearchCondition createSearchCondition(String sql) throws ParseException {
		return new TableQueryParser(sql).searchCondition();
	}

	/**
	 * Create a comparison predicate from the input SQL.
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static ComparisonPredicate createComparisonPredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate().getComparisonPredicate();
	}

	/**
	 * Create a between predicate from the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static BetweenPredicate createBetweenPredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate().getBetweenPredicate();
	}

	/**
	 * Create an in predicate from the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static InPredicate createInPredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate().getInPredicate();
	}

	/**
	 * Create a like predicate form the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static LikePredicate createLikePredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate().getLikePredicate();
	}

	/**
	 * Create boolean predicate from the input SQL.
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static BooleanPredicate createBooleanPredicate(String sql) throws ParseException {
		return (BooleanPredicate) new TableQueryParser(sql).predicate().getIsPredicate();
	}

	/**
	 * Create null predicate from the input SQL.
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static NullPredicate createNullPredicate(String sql) throws ParseException {
		return (NullPredicate) new TableQueryParser(sql).predicate().getIsPredicate();
	}

	/**
	 * Create a row value constructor from the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static RowValueConstructor createRowValueConstructor(String sql) throws ParseException {
		return new TableQueryParser(sql).rowValueConstructor();
	}

	/**
	 * Create a row value constructor element from the input SQL
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static RowValueConstructorElement createRowValueConstructorElement(String sql) throws ParseException {
		return new TableQueryParser(sql).rowValueConstructorElement();
	}

	/**
	 * Create a list of row value constructor elements, one for each passed SQL.
	 * 
	 * @param sqls
	 * @return
	 * @throws ParseException
	 */
	public static List<RowValueConstructorElement> createRowValueConstructorElements(String...sqls) throws ParseException {
		List<RowValueConstructorElement> list = new LinkedList<RowValueConstructorElement>();
		for(String sql: sqls){
			list.add(createRowValueConstructorElement(sql));
		}
		return list;
	}
	/**
	 * Create a row value constructor list from input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static RowValueConstructorList createRowValueConstructorList(String sql) throws ParseException {
		return new TableQueryParser(sql).rowValueConstructorList();
	}

	/**
	 * Create an in-predicate-value from the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static InPredicateValue createInPredicateValue(String sql) throws ParseException {
		return new TableQueryParser(sql).inPredicateValue();
	}

	/**
	 * Create a match value for the given SQL
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static MatchValue createMatchValue(String sql) throws ParseException {
		return new TableQueryParser(sql).matchValue();
	}

	/**
	 * Create a pattern for the given SQL.
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static Pattern createPattern(String sql) throws ParseException {
		return new TableQueryParser(sql).pattern();
	}

	/**
	 * Create escape character from the the passed SQL
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static EscapeCharacter createEscapeCharacter(String sql) throws ParseException {
		return new TableQueryParser(sql).escapeCharacter();
	}

	/**
	 * Create a column reference for the passed SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static ColumnReference createColumnReference(String sql) throws ParseException {
		return new TableQueryParser(sql).columnReference();
	}

	/**
	 * Create a select list from the passed SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static SelectList createSelectList(String sql) throws ParseException {
		return new TableQueryParser(sql).selectList();
	}

	/**
	 * Create a grouping column reference from the passed SQL
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static GroupingColumnReference createGroupingColumnReference(String sql) throws ParseException {
		return new TableQueryParser(sql).groupingColumnReference();
	}

	/**
	 * Create a list of GroupingColumnReference, one for each passed SQL string.
	 * @param sqls
	 * @return
	 * @throws ParseException
	 */
	public static List<GroupingColumnReference> createGroupingColumnReferences(String...sqls) throws ParseException{
		List<GroupingColumnReference> list = new LinkedList<GroupingColumnReference>();
		for(String sql: sqls){
			list.add(createGroupingColumnReference(sql));
		}
		return list;
	}

	/**
	 * Create a GroupingColumnReferenceList from the passed SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static GroupingColumnReferenceList createGroupingColumnReferenceList(String sql) throws ParseException {
		return new TableQueryParser(sql).groupingColumnReferenceList();
	}

	/**
	 * Create a SortKey from the passed SQL.
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static SortKey createSortKey(String sql) throws ParseException {
		return new TableQueryParser(sql).sortKey();
	}
	
	/**
	 * Create a SortSpecification from the passed SQL. 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static SortSpecification createSortSpecification(String sql) throws ParseException{
		return new TableQueryParser(sql).sortSpecification();
	}
	
	/**
	 * Create a list of SortSpecification one for each string passed.
	 * @param sqls
	 * @return
	 * @throws ParseException
	 */
	public static List<SortSpecification> createSortSpecifications(String...sqls) throws ParseException{
		List<SortSpecification> list = new LinkedList<SortSpecification>();
		for(String sql: sqls){
			list.add(createSortSpecification(sql));
		}
		return list;
	}

	/**
	 * Create a SortSpecificationList from the passed SQL.
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static SortSpecificationList createSortSpecificationList(String sql) throws ParseException {
		return new TableQueryParser(sql).sortSpecificationList();
	}
	
	/**
	 * Get the tableId from a QuerySpecification 
	 * @param model
	 * @return
	 */
	public static String getTableId(QuerySpecification querySpecification){
		if (querySpecification == null)
			throw new IllegalArgumentException("QuerySpecification cannot be null");
		return getTableId(querySpecification.getTableExpression());
	}
	
	/**
	 * Get the tableId from the passed SQL string.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static String getTableId(String sql) throws ParseException {
		if (sql == null)
			throw new IllegalArgumentException("SQL cannot be null");
		return getTableId(new TableQueryParser(sql).querySpecification());
	}

	/**
	 * Get the tableId from a TableExpression
	 * @param tableExpression
	 * @return
	 */
	public static String getTableId(TableExpression tableExpression) {
		if (tableExpression == null)
			throw new IllegalArgumentException("TableExpression cannot be null");
		return getTableId(tableExpression.getFromClause());
	}

	/**
	 * Get the tableId from a FromClause
	 * @param fromClause
	 * @return
	 */
	public static String getTableId(FromClause fromClause) {
		if (fromClause == null)
			throw new IllegalArgumentException("FromClause cannot be null");
		return getTableId(fromClause.getTableReference());
	}

	/**
	  * Get the tableId from a TableReference
	 * @param tableReference
	 * @return
	 */
	public static String getTableId(TableReference tableReference) {
		if (tableReference == null)
			throw new IllegalArgumentException("TableReference cannot be null");
		return tableReference.getTableName();
	}
	
	/**
	 * Convert the passed query into a count query.
	 * @param model
	 * @return
	 * @throws ParseException 
	 */
	public static QuerySpecification convertToCountQuery(QuerySpecification model) {
		ValidateArgument.required(model, "QuerySpecification");
		TableExpression currentTableExpression = model.getTableExpression();
		ValidateArgument.required(currentTableExpression, "TableExpression");

		// Clear the select list
		SelectList count;
		try {
			count = new SelectList(createDerivedColumns("count(*)"));
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		TableExpression tableExpression = new TableExpression(currentTableExpression.getFromClause(),
				currentTableExpression.getWhereClause(), currentTableExpression.getGroupByClause(), null, null);
		return new QuerySpecification(null, null, count, tableExpression);
	}
	
	/**
	 * Convert the passed query into a count query.
	 * 
	 * @param model
	 * @return
	 * @throws ParseException
	 */
	public static QuerySpecification convertToPaginatedQuery(QuerySpecification model, Long offset, Long limit) throws ParseException {
		if (model == null)
			throw new IllegalArgumentException("QuerySpecification cannot be null");
		TableExpression currentTableExpression = model.getTableExpression();
		if (currentTableExpression == null)
			throw new IllegalArgumentException("TableExpression cannot be null");
		// add pagination
		TableExpression tableExpression = new TableExpression(currentTableExpression.getFromClause(),
				currentTableExpression.getWhereClause(), currentTableExpression.getGroupByClause(),
				currentTableExpression.getOrderByClause(), new Pagination(limit, offset));
		return new QuerySpecification(model.getSetQuantifier(), model.getSelectList(), tableExpression);
	}

	public static TableExpression removeOrderByClause(TableExpression tableExpression) {
		return new TableExpression(tableExpression.getFromClause(), tableExpression.getWhereClause(), tableExpression.getGroupByClause(),
				null, tableExpression.getPagination());
	}

	public static QuerySpecification convertToSortedQuery(QuerySpecification model, List<SortItem> sortList) {
		ValidateArgument.required(model, "QuerySpecification");
		ValidateArgument.required(sortList, "sortList");
		TableExpression currentTableExpression = model.getTableExpression();
		ValidateArgument.required(currentTableExpression, "TableExpression");

		Map<String, SortSpecification> originalSortSpecifications;
		OrderByClause orderByClause = currentTableExpression.getOrderByClause();
		if (orderByClause == null) {
			originalSortSpecifications = Collections.emptyMap();
		} else {
			// need to preserve order, so use linked hash map
			originalSortSpecifications = Maps.newLinkedHashMap();
			for (SortSpecification spec : orderByClause.getSortSpecificationList().getSortSpecifications()) {
				StringBuilder columnName = new StringBuilder();
				spec.getSortKey().getColumnReference().toSQL(columnName, null);
				originalSortSpecifications.put(columnName.toString(), spec);
			}
		}

		List<SortSpecification> sortSpecifications = Lists.newArrayListWithCapacity(originalSortSpecifications.size() + sortList.size());

		for (SortItem sortItem : sortList) {
			// no sortItem.getDirection() will become order ASC
			OrderingSpecification direction = sortItem.getDirection() == SortDirection.DESC ? OrderingSpecification.DESC
					: OrderingSpecification.ASC;
			originalSortSpecifications.remove(sortItem.getColumn());
			sortSpecifications.add(new SortSpecification(new SortKey(new ColumnReference(new ColumnName(new Identifier(new ActualIdentifier(
					sortItem.getColumn(), null))), null)), direction));
		}
		sortSpecifications.addAll(originalSortSpecifications.values());
		orderByClause = new OrderByClause(new SortSpecificationList(sortSpecifications));

		// add pagination
		TableExpression tableExpression = new TableExpression(currentTableExpression.getFromClause(),
				currentTableExpression.getWhereClause(), currentTableExpression.getGroupByClause(), orderByClause,
				currentTableExpression.getPagination());
		return new QuerySpecification(model.getSetQuantifier(), model.getSelectList(), tableExpression);
	}
}
