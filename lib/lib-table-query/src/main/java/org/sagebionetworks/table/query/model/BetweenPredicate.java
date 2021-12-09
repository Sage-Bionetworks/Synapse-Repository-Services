package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltbetween predicate&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class BetweenPredicate extends SQLElement implements HasPredicate {

	ReplaceableBox<ColumnReference> columnReferenceLHS;
	Boolean not;
	RowValueConstructor betweenRowValueConstructor;
	RowValueConstructor andRowValueConstructorRHS;

	public BetweenPredicate(ColumnReference columnReferenceLHS, Boolean not,
			RowValueConstructor betweenRowValueConstructor, RowValueConstructor andRowValueConstructorRHS) {
		super();
		this.columnReferenceLHS = new ReplaceableBox<ColumnReference>(columnReferenceLHS);
		this.not = not;
		this.betweenRowValueConstructor = betweenRowValueConstructor;
		this.andRowValueConstructorRHS = andRowValueConstructorRHS;
	}

	public Boolean getNot() {
		return not;
	}

	public RowValueConstructor getBetweenRowValueConstructor() {
		return betweenRowValueConstructor;
	}

	public RowValueConstructor getAndRowValueConstructorRHS() {
		return andRowValueConstructorRHS;
	}

	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS.getChild();
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		columnReferenceLHS.toSql(builder, parameters);
		if (not != null) {
			builder.append(" NOT");
		}
		builder.append(" BETWEEN ");
		betweenRowValueConstructor.toSql(builder, parameters);
		builder.append(" AND ");
		andRowValueConstructorRHS.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(columnReferenceLHS, betweenRowValueConstructor, andRowValueConstructorRHS);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS.getChild();
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		List<UnsignedLiteral> results = new LinkedList<UnsignedLiteral>();
		for (UnsignedLiteral value : betweenRowValueConstructor.createIterable(UnsignedLiteral.class)) {
			results.add(value);
		}
		for (UnsignedLiteral value : andRowValueConstructorRHS.createIterable(UnsignedLiteral.class)) {
			results.add(value);
		}
		return results;
	}

}
