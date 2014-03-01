package org.sagebionetworks.table.query.model;


/**
 * This matches &ltcolumn reference&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ColumnReference {
	
	String nameLHS;
	String nameRHS;
	public ColumnReference(String nameLHS, String nameRHS) {
		super();
		this.nameLHS = nameLHS;
		this.nameRHS = nameRHS;
	}
	public String getNameLHS() {
		return nameLHS;
	}
	public String getNameRHS() {
		return nameRHS;
	}
	
}
