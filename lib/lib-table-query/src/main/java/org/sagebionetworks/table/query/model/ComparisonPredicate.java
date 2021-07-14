package org.sagebionetworks.table.query.model;

/**
 * This matches &ltcomparison predicate&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class ComparisonPredicate extends SQLElement implements HasPredicate {

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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		columnReferenceLHS.toSql(builder, parameters);
		builder.append(" ");
		builder.append(compOp.toSQL());
		builder.append(" ");
		rowValueConstructorRHS.toSql(builder, parameters);
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(columnReferenceLHS, rowValueConstructorRHS);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {	
		return rowValueConstructorRHS.createIterable(UnsignedLiteral.class);
	}

}
