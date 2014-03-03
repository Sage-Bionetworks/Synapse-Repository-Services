package org.sagebionetworks.table.query.model;

/**
 * This matches &ltcolumn reference&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ColumnReference implements SQLElement{
	
	ColumnName nameLHS;
	ColumnName nameRHS;
	public ColumnReference(ColumnName nameLHS, ColumnName nameRHS) {
		super();
		this.nameLHS = nameLHS;
		this.nameRHS = nameRHS;
	}
	public ColumnName getNameLHS() {
		return nameLHS;
	}
	public ColumnName getNameRHS() {
		return nameRHS;
	}
	@Override
	public void toSQL(StringBuilder builder) {
		this.nameLHS.toSQL(builder);
		if(nameRHS != null){
			builder.append(".");
			this.nameRHS.toSQL(builder);
		}
	}
	
}
