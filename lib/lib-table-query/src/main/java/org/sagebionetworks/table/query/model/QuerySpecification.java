package org.sagebionetworks.table.query.model;

/**
 * This matches &ltquery specification&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class QuerySpecification {

	SetQuantifier setQuantifier;
	SelectList selectList;
	TableExpression tableExpression;
	
	public QuerySpecification(SetQuantifier setQuantifier,
			SelectList selectList, TableExpression tableExpression) {
		super();
		this.setQuantifier = setQuantifier;
		this.selectList = selectList;
		this.tableExpression = tableExpression;
	}
	public SetQuantifier getSetQuantifier() {
		return setQuantifier;
	}
	public SelectList getSelectList() {
		return selectList;
	}
	public TableExpression getTableExpression() {
		return tableExpression;
	}
	
}
