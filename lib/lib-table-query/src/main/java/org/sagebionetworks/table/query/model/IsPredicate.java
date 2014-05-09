package org.sagebionetworks.table.query.model;

/**
 * This matches &ltis predicate&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public abstract class IsPredicate implements SQLElement {
	
	ColumnReference columnReferenceLHS;
	Boolean not;

	public IsPredicate(ColumnReference columnReferenceLHS, Boolean not) {
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
	}

	public Boolean getNot() {
		return not;
	}

	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	public abstract String getCompareValue();

	@Override
	public void toSQL(StringBuilder builder) {
		columnReferenceLHS.toSQL(builder);
		builder.append(" IS ");
		if(not != null){
			builder.append("NOT ");
		}
		builder.append(getCompareValue());
	}
}
