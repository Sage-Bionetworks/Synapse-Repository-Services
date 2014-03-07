package org.sagebionetworks.table.query.model;

/**
 * This matches &ltnull predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class NullPredicate implements SQLElement {
	
	ColumnReference columnReferenceLHS;
	Boolean not;

	public NullPredicate(ColumnReference columnReferenceLHS, Boolean not) {
		super();
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
	}

	public Boolean getNot() {
		return not;
	}

	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		columnReferenceLHS.toSQL(builder);
		builder.append(" IS");
		if(not != null){
			builder.append(" NOT");
		}
		builder.append(" NULL");
	}
	
}
