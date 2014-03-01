package org.sagebionetworks.table.query.model;

/**
 * This matches &ltoverlaps predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class OverlapsPredicate {

	RowValueConstructor rowValueConstructorLHS;
	RowValueConstructor rowValueConstructorRHS;
	public OverlapsPredicate(RowValueConstructor rowValueConstructorLHS,
			RowValueConstructor rowValueConstructorRHS) {
		super();
		this.rowValueConstructorLHS = rowValueConstructorLHS;
		this.rowValueConstructorRHS = rowValueConstructorRHS;
	}
	public RowValueConstructor getRowValueConstructorLHS() {
		return rowValueConstructorLHS;
	}
	public RowValueConstructor getRowValueConstructorRHS() {
		return rowValueConstructorRHS;
	}
	
}
