package org.sagebionetworks.table.query.model;

/**
 * This matches &ltcomparison predicate&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ComparisonPredicate implements SQLElement {

	ColumnReference columnReferenceLHS;
	CompOp compOp;
	RowValueConstructor rowValueConstructorRHS;
	public ComparisonPredicate(ColumnReference columnReferenceLHS,
			CompOp compOp, RowValueConstructor rowValueConstructorRHS) {
		super();
		this.columnReferenceLHS = columnReferenceLHS;
		this.compOp = compOp;
		this.rowValueConstructorRHS = rowValueConstructorRHS;
	}

	public CompOp getCompOp() {
		return compOp;
	}
	public RowValueConstructor getRowValueConstructorRHS() {
		return rowValueConstructorRHS;
	}
	
	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		columnReferenceLHS.toSQL(builder);
		builder.append(" ").append(compOp.toSQL()).append(" ");
		rowValueConstructorRHS.toSQL(builder);
	}
	
}
