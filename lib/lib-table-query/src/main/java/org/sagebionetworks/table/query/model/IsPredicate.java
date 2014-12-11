package org.sagebionetworks.table.query.model;


/**
 * This matches &ltis predicate&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public abstract class IsPredicate extends SQLElement {
	
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
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		columnReferenceLHS.toSQL(builder, columnConvertor);
		builder.append(" IS ");
		if(not != null){
			builder.append("NOT ");
		}
		builder.append(getCompareValue());
	}
}
