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
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.LikePredicate;
import org.sagebionetworks.table.query.model.MatchValue;
import org.sagebionetworks.table.query.model.NullPredicate;
import org.sagebionetworks.table.query.model.Pattern;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.model.RowValueConstructorElement;
import org.sagebionetworks.table.query.model.RowValueConstructorList;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.model.TableExpression;
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


}
