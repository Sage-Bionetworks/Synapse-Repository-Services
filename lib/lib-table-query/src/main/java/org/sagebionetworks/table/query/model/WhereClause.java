package org.sagebionetworks.table.query.model;

import java.util.List;


/**
 * This matches &ltwhere clause&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class WhereClause extends SQLElement {

	private final SearchCondition searchCondition;

	public WhereClause(SearchCondition searchCondition) {
		this.searchCondition = searchCondition;
	}

	public SearchCondition getSearchCondition() {
		return searchCondition;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("WHERE ");
		searchCondition.toSql(builder, parameters);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, searchCondition);
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(searchCondition);
	}
}
