package org.sagebionetworks.table.query.model;

import java.util.List;


/**
 * This matches &ltin predicate&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class InPredicate extends SQLElement implements HasPredicate {

	ColumnReference columnReferenceLHS;
	Boolean not;
	InPredicateValue inPredicateValue;
	
	public InPredicate(ColumnReference columnReferenceLHS, Boolean not,
			InPredicateValue inPredicateValue) {
		super();
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
		this.inPredicateValue = inPredicateValue;
	}

	public Boolean getNot() {
		return not;
	}
	public InPredicateValue getInPredicateValue() {
		return inPredicateValue;
	}
	
	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		columnReferenceLHS.toSql(builder, parameters);
		builder.append(" ");
		if (this.not != null) {
			builder.append("NOT ");
		}
		builder.append("IN ( ");
		inPredicateValue.toSql(builder, parameters);
		builder.append(" )");
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnReferenceLHS);
		checkElement(elements, type, inPredicateValue);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return inPredicateValue.createIterable(UnsignedLiteral.class);
	}

}
