package org.sagebionetworks.table.query.util;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.BetweenPredicate;
import org.sagebionetworks.table.query.model.BooleanFactor;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.BooleanTerm;
import org.sagebionetworks.table.query.model.BooleanTest;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.ComparisonPredicate;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.EscapeCharacter;
import org.sagebionetworks.table.query.model.FromClause;
import org.sagebionetworks.table.query.model.GroupingColumnReference;
import org.sagebionetworks.table.query.model.GroupingColumnReferenceList;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.LikePredicate;
import org.sagebionetworks.table.query.model.MatchValue;
import org.sagebionetworks.table.query.model.NullPredicate;
import org.sagebionetworks.table.query.model.Pattern;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.model.RowValueConstructorElement;
import org.sagebionetworks.table.query.model.RowValueConstructorList;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SortKey;
import org.sagebionetworks.table.query.model.SortSpecification;
import org.sagebionetworks.table.query.model.SortSpecificationList;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.TableReference;
import org.sagebionetworks.table.query.model.ValueExpression;

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
	 * Create null predicate from the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static NullPredicate createNullPredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate().getNullPredicate();
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
		if(querySpecification == null) throw new IllegalArgumentException("QuerySpecification cannot be null");
		return getTableId(querySpecification.getTableExpression());
	}

	/**
	 * Get the tableId from a TableExpression
	 * @param tableExpression
	 * @return
	 */
	public static String getTableId(TableExpression tableExpression) {
		if(tableExpression == null) throw new IllegalArgumentException("TableExpression cannot be null");
		return getTableId(tableExpression.getFromClause());
	}

	/**
	 * Get the tableId from a FromClause
	 * @param fromClause
	 * @return
	 */
	public static String getTableId(FromClause fromClause) {
		if(fromClause == null) throw new IllegalArgumentException("FromClause cannot be null");
		return getTableId(fromClause.getTableReference());
	}

	/**
	  * Get the tableId from a TableReference
	 * @param tableReference
	 * @return
	 */
	public static String getTableId(TableReference tableReference) {
		if(tableReference == null) throw new IllegalArgumentException("TableReference cannot be null");
		return "syn"+tableReference.getTableName();
	}
	
	/**
	 * Convert the passed query into a count query.
	 * @param model
	 * @return
	 * @throws ParseException 
	 */
	public static QuerySpecification convertToCountQuery(QuerySpecification model) throws ParseException{
		if(model == null) throw new IllegalArgumentException("QuerySpecification cannot be null");
		TableExpression currentTableExpression = model.getTableExpression();
		if(currentTableExpression == null) throw new IllegalArgumentException("TableExpression cannot be null");
		// Clear the select list
		SelectList count = new SelectList(createDerivedColumns("count(*)"));
		TableExpression tableExpression = new TableExpression(currentTableExpression.getFromClause(), currentTableExpression.getWhereClause(), null, null, null);
		return new QuerySpecification(null, count, tableExpression);
	}
	
}
