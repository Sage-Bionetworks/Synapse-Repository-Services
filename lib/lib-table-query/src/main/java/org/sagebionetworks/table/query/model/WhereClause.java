package org.sagebionetworks.table.query.model;

/**
 * This matches &ltwhere clause&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class WhereClause extends SQLElement implements HasSearchCondition {

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
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(searchCondition);
	}
}
