package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableExpression {

	FromClause fromClause;
	
	public TableExpression(FromClause fromClause) {
		super();
		this.fromClause = fromClause;
	}

	public FromClause getFromClause() {
		return fromClause;
	}

}
