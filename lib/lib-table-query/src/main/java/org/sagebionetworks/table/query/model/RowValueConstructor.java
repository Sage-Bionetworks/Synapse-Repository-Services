package org.sagebionetworks.table.query.model;

/**
 * This matches &ltrow value constructor&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class RowValueConstructor {

	RowValueConstructorElement rowValueConstructorElement;
	RowValueConstructorList rowValueConstructorList;
	public RowValueConstructor(
			RowValueConstructorElement rowValueConstructorElement) {
		super();
		this.rowValueConstructorElement = rowValueConstructorElement;
	}
	public RowValueConstructor(RowValueConstructorList rowValueConstructorList) {
		super();
		this.rowValueConstructorList = rowValueConstructorList;
	}
	public RowValueConstructorElement getRowValueConstructorElement() {
		return rowValueConstructorElement;
	}
	public RowValueConstructorList getRowValueConstructorList() {
		return rowValueConstructorList;
	}
	
}
