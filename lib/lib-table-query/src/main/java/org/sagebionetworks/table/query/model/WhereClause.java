package org.sagebionetworks.table.query.model;

/**
 * This matches &ltwhere clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class WhereClause {

	SearchCondition searchCondition;

	public WhereClause(SearchCondition searchCondition) {
		super();
		this.searchCondition = searchCondition;
	}

	public SearchCondition getSearchCondition() {
		return searchCondition;
	}
	
}
