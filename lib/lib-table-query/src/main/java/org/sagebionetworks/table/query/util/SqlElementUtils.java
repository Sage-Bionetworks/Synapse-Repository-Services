package org.sagebionetworks.table.query.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.BetweenPredicate;
import org.sagebionetworks.table.query.model.BooleanFactor;
import org.sagebionetworks.table.query.model.BooleanPredicate;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.BooleanTerm;
import org.sagebionetworks.table.query.model.BooleanTest;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.ComparisonPredicate;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.EscapeCharacter;
import org.sagebionetworks.table.query.model.FromClause;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.GroupingColumnReference;
import org.sagebionetworks.table.query.model.GroupingColumnReferenceList;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.LikePredicate;
import org.sagebionetworks.table.query.model.MatchValue;
import org.sagebionetworks.table.query.model.NullPredicate;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.OrderingSpecification;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.Pattern;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.model.RowValueConstructorElement;
import org.sagebionetworks.table.query.model.RowValueConstructorList;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.SortKey;
import org.sagebionetworks.table.query.model.SortSpecification;
import org.sagebionetworks.table.query.model.SortSpecificationList;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Utilities for creating SQL elements
 * 
 * @author John
 *
 */
public class SqlElementUtils {
	
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
		Predicate predicate =  new TableQueryParser(sql).predicate();
		predicate.recursiveSetParent();
		return predicate;
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
		return new TableQueryParser(sql).predicate().getFirstElementOfType(ComparisonPredicate.class);
	}

	/**
	 * Create a between predicate from the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static BetweenPredicate createBetweenPredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate().getFirstElementOfType(BetweenPredicate.class);
	}

	/**
	 * Create an in predicate from the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static InPredicate createInPredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate().getFirstElementOfType(InPredicate.class);
	}

	/**
	 * Create a like predicate form the input SQL.
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public static LikePredicate createLikePredicate(String sql) throws ParseException {
		return new TableQueryParser(sql).predicate().getFirstElementOfType(LikePredicate.class);
	}

	/**
	 * Create boolean predicate from the input SQL.
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static BooleanPredicate createBooleanPredicate(String sql) throws ParseException {
		return (BooleanPredicate) new TableQueryParser(sql).predicate().getFirstElementOfType(BooleanPredicate.class);
	}

	/**
	 * Create null predicate from the input SQL.
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static NullPredicate createNullPredicate(String sql) throws ParseException {
		return (NullPredicate) new TableQueryParser(sql).predicate().getFirstElementOfType(NullPredicate.class);
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
	 * Creates a column name for the passed SQL.
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static ColumnName createColumnName(String sql) throws ParseException {
		return new TableQueryParser(sql).columnName();
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


	public static TableExpression removeOrderByClause(TableExpression tableExpression) {
		return new TableExpression(tableExpression.getFromClause(), tableExpression.getWhereClause(), tableExpression.getGroupByClause(),
				null, tableExpression.getPagination());
	}

	/**
	 * If given a nonempty sortList, create a new OrderByClause that is the the union of the currentOrderBy and the 
	 * sortList.
	 * @param currentOrderBy
	 * @param sortList
	 * @return
	 * @throws ParseException
	 */
	public static OrderByClause convertToSortedQuery(final OrderByClause currentOrderBy, List<SortItem> sortList) throws ParseException {
		if(sortList == null || sortList.isEmpty()) {
			return currentOrderBy;
		}
		Map<String, SortSpecification> originalSortSpecifications;
		if (currentOrderBy == null) {
			originalSortSpecifications = Collections.emptyMap();
		} else {
			// need to preserve order, so use linked hash map
			originalSortSpecifications = Maps.newLinkedHashMap();
			for (SortSpecification spec : currentOrderBy.getSortSpecificationList().getSortSpecifications()) {
				String columnName = spec.getSortKey().toSql();
				originalSortSpecifications.put(columnName, spec);
			}
		}

		List<SortSpecification> sortSpecifications = Lists.newArrayListWithCapacity(originalSortSpecifications.size() + sortList.size());

		for (SortItem sortItem : sortList) {
			// no sortItem.getDirection() will become order ASC
			OrderingSpecification direction = sortItem.getDirection() == SortDirection.DESC ? OrderingSpecification.DESC
					: OrderingSpecification.ASC;
			originalSortSpecifications.remove(sortItem.getColumn());
			sortSpecifications.add(new SortSpecification(createSortKey(sortItem.getColumn()), direction));
		}
		sortSpecifications.addAll(originalSortSpecifications.values());
		return new OrderByClause(new SortSpecificationList(sortSpecifications));
	}

	/**
	 * Override pagination on the given query using the provided limit and offset.
	 * 
	 * @param model
	 * @param offset
	 * @param limit
	 * @return
	 */
	public static Pagination overridePagination(Pagination pagination, Long offset, Long limit) {
		if(offset == null && limit == null){
			// there is nothing to do.
			return pagination;
		}
		long limitFromRequest = (limit != null) ? limit : Long.MAX_VALUE;
		long offsetFromRequest = (offset != null) ? offset : 0L;
		
		long limitFromQuery = Long.MAX_VALUE;
		long offsetFromQuery = 0L;
		
		if (pagination != null) {
			if (pagination.getLimitLong() != null) {
				limitFromQuery = pagination.getLimitLong();
			}
			if (pagination.getOffsetLong() != null) {
				offsetFromQuery = pagination.getOffsetLong();
			}
		}
		
		long paginatedOffset = offsetFromQuery + offsetFromRequest;
		// adjust the limit from the query based on the additional offset (assume Long.MAX_VALUE - offset is still
		// always large enough)
		limitFromQuery = Math.max(0, limitFromQuery - offsetFromRequest);
		
		long paginatedLimit = Math.min(limitFromRequest, limitFromQuery);
		
		return new Pagination(paginatedLimit, paginatedOffset);
	}
		
	/**
	 * Limit pagination based on the provided maxRowsPerPage 
	 * @param pagination
	 * @param maxRowsPerPage
	 * @return
	 */
	public static Pagination limitMaxRowsPerPage(Pagination pagination, Long maxRowsPerPage) {
		if(maxRowsPerPage == null){
			return pagination;
		}
		if(pagination == null) {
			return new Pagination(maxRowsPerPage, 0L);
		}
		if(pagination.getLimitLong() > maxRowsPerPage) {
			return new Pagination(maxRowsPerPage, pagination.getOffsetLong());
		}
		return pagination;
	}
	
	/**
	 * Modify the provided {@link QuerySpecification} to be a "count" query.  This involves
	 * modifying the select and removing grouping, ordering and pagination.
	 * 
	 * @param model
	 * @return True if select of the provided model was modified to be count query.  False, when
	 * the model cannot be modified (already an aggregate query that would return only one row).
	 * 
	 */
	public static boolean createCountSql(QuerySpecification model) {
		StringBuilder builder = new StringBuilder();
		// There are three cases
		if (model.hasAnyAggregateElements()) {
			// does this have a group by clause?
			if (model.getTableExpression().getGroupByClause() != null) {
				// group by query
				builder.append("COUNT(DISTINCT ");
				builder.append(
						createSelectFromGroupBy(model.getSelectList(), model.getTableExpression().getGroupByClause()));
				builder.append(")");
			} else if (SetQuantifier.DISTINCT.equals(model.getSetQuantifier())) {
				// distinct select
				builder.append("COUNT(DISTINCT ");
				builder.append(createSelectWithoutAs(model.getSelectList()));
				builder.append(")");
			} else {
				return false;
			}
		} else {
			// simple count *
			builder.append("COUNT(*)");
		}
		try {
			model.replaceSelectList(new TableQueryParser(builder.toString()).selectList(), null);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		model.getTableExpression().replacePagination(null);
		model.getTableExpression().replaceGroupBy(null);
		model.getTableExpression().replaceOrderBy(null);
		return true;
	}
	
	/**
	 * Build the SQL to select the ROW_ID and ROW_VERSION for the given query with the provided limit.
	 * 
	 * @param model
	 * @param limit
	 * @return
	 * @throws SimpleAggregateQueryException
	 */
	public static Optional<String> buildSqlSelectRowIdAndVersions(QuerySpecification model, long maxLimit) {
		TableExpression tableExpression = null;
		FromClause fromClause = model.getTableExpression().getFromClause();
		WhereClause whereClause = model.getTableExpression().getWhereClause();
		GroupByClause groupByClause = null;
		OrderByClause orderByClause = null;
		Pagination pagination = null;

		// There are three cases
		if (model.hasAnyAggregateElements()) {
			return Optional.empty();
		}

		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		builder.append(TableConstants.ROW_ID);
		builder.append(", ");
		builder.append(TableConstants.ROW_VERSION);
		// clear pagination, group by, order by
		tableExpression = new TableExpression(fromClause, whereClause, groupByClause, orderByClause, pagination);
		builder.append(" ");
		builder.append(tableExpression.toSql());
		builder.append(" LIMIT ");
		builder.append(maxLimit);
		return Optional.of(builder.toString());
	}
	
	/**
	 * Create a select clause from a group by clause.
	 * A group by column reference can be the value of an 'AS' from the select
	 * column.  When this occurs the original ValueExpression from the select must
	 * replace the a
	 * 
	 * @param originalSelect The original select of the query.
	 * @param groupBy
	 * @return
	 */
	public static String createSelectFromGroupBy(SelectList originalSelect, GroupByClause groupBy){
		Map<String, ValueExpression> asMapping = new HashMap<String, ValueExpression>();
		// build a mapping for each as clause
		for(DerivedColumn dc: originalSelect.getColumns()){
			if(dc.getAsClause() != null){
				String asValue = dc.getAsClause().getColumnName().toSqlWithoutQuotes();
				asMapping.put(asValue, dc.getValueExpression());
			}
		}
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for(GroupingColumnReference gcr: groupBy.getGroupingColumnReferenceList().getGroupingColumnReferences()){
			if(!isFirst){
				builder.append(", ");
			}
			String unQuoted = gcr.toSqlWithoutQuotes();
			ValueExpression selectValue = asMapping.get(unQuoted);
			if(selectValue != null){
				// replace ass with value expression
				builder.append(selectValue.toSql());
			}else{
				builder.append(gcr.toSql());
			}
			isFirst = false;
		}
		return builder.toString();
	}
	
	/**
	 * Create a select statement excluding any 'AS' clause.
	 * @param originalSelect
	 * @return
	 */
	public static String createSelectWithoutAs(SelectList originalSelect){
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for(DerivedColumn dc: originalSelect.getColumns()){
			if(!isFirst){
				builder.append(", ");
			}
			builder.append(dc.getValueExpression().toSql());
			isFirst = false;
		}
		return builder.toString();
	}
	
	/**
	 * Create delimited sort key from a column name.
	 * 
	 * @param columnName
	 * @return
	 * @throws ParseException 
	 */
	public static SortKey createSortKey(String columnName) throws ParseException {
		try {
			/*
			 * For aggregate functions we can use this ValueExpressionPrimary to
			 * create the SortKey. For non-aggregate functions the name must be
			 * bracketed in quotes.
			 */
			TableQueryParser parser = new TableQueryParser(columnName);
			ValueExpression primary = parser.valueExpression();
			String nextToken = parser.getNextToken().toString();
			if(nextToken.length() > 1) {
				// The entire value was not consumed so it must be wrapped
				return new TableQueryParser(wrapInDoubleQuotes(columnName)).sortKey();
			}
			if (primary.hasAnyAggregateElements()) {
				return new SortKey(primary);
			} else {
				// Put non-aggregate column names in quotes.
				return new TableQueryParser(wrapInDoubleQuotes(columnName)).sortKey();
			}
		} catch (ParseException e) {
			// the column will need to be in quotes.
			return new TableQueryParser(wrapInDoubleQuotes(columnName)).sortKey();
		}
	}
	
	/**
	 * Create a non-quoted derived column.
	 * @param columnName
	 * @return
	 */
	public static DerivedColumn createNonQuotedDerivedColumn(String columnName){
		try {
			return new TableQueryParser(columnName).derivedColumn();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Wrap the given string in double quotes.
	 * 
	 * @param toWrap
	 * @return
	 */
	public static String wrapInDoubleQuotes(String toWrap){
		return "\"" + toWrap.replaceAll("\"", "\"\"") + "\"";
	}

	/**
	 * Appends a WHERE clause to the String Builder if necessary
	 * @param builder StringBuilder to append to
	 * @param searchConditionString SearchCondition string to append. pass null if none to append.
	 * @param originalWhereClause the WHERE clause that was in the original query. pass null if not exist.
	 */
	public static void appendCombinedWhereClauseToStringBuilder(StringBuilder builder, String searchConditionString,
																WhereClause originalWhereClause) {
		ValidateArgument.required(builder, "builder");

		if(searchConditionString != null || originalWhereClause != null){
			builder.append(" WHERE ");
			if(originalWhereClause != null){
				if(searchConditionString != null){
					builder.append("(");
				}
				builder.append(originalWhereClause.getSearchCondition().toSql());
				if(searchConditionString != null){
					builder.append(")");
				}
			}
			if(searchConditionString != null){
				if(originalWhereClause != null){
					builder.append(" AND ");
					builder.append("(");
				}
				builder.append(searchConditionString);
				if(originalWhereClause != null){
					builder.append(")");
				}
			}
		}
	}
}
