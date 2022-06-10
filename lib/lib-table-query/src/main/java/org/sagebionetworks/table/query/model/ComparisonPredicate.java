package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * This matches &ltcomparison predicate&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class ComparisonPredicate extends SQLElement implements HasPredicate {

	ReplaceableBox<ColumnReference> columnReferenceLHS;
	CompOp compOp;
	RowValueConstructor rowValueConstructorRHS;
	public ComparisonPredicate(ColumnReference columnReferenceLHS,
			CompOp compOp, RowValueConstructor rowValueConstructorRHS) {
		super();
		this.columnReferenceLHS = new ReplaceableBox<ColumnReference>(columnReferenceLHS);
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
		return columnReferenceLHS.getChild();
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
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(columnReferenceLHS, rowValueConstructorRHS);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS.getChild();
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {	
		return rowValueConstructorRHS.createIterable(UnsignedLiteral.class);
	}

	@Override
	public Optional<ColumnReference> getRightHandSideColumn() {
		ColumnReference columnRef = null;
		if(rowValueConstructorRHS != null) {
			columnRef = rowValueConstructorRHS.getFirstElementOfType(ColumnReference.class);
		}
		return Optional.ofNullable(columnRef);
	}
	
	

}
