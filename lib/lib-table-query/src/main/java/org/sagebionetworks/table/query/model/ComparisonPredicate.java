package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * This matches &ltcomparison predicate&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class ComparisonPredicate extends SQLElement implements HasPredicate {

	PredicateLeftHandSide leftHandSide;
	CompOp compOp;
	RowValueConstructor rowValueConstructorRHS;
	public ComparisonPredicate(PredicateLeftHandSide leftHandSide,
			CompOp compOp, RowValueConstructor rowValueConstructorRHS) {
		super();
		this.leftHandSide = leftHandSide;
		this.compOp = compOp;
		this.rowValueConstructorRHS = rowValueConstructorRHS;
	}

	public CompOp getCompOp() {
		return compOp;
	}
	
	public RowValueConstructor getRowValueConstructorRHS() {
		return rowValueConstructorRHS;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		leftHandSide.toSql(builder, parameters);
		builder.append(" ");
		builder.append(compOp.toSQL());
		builder.append(" ");
		rowValueConstructorRHS.toSql(builder, parameters);
	}
	
	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(leftHandSide, rowValueConstructorRHS);
	}

	@Override
	public PredicateLeftHandSide getLeftHandSide() {
		return leftHandSide;
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
