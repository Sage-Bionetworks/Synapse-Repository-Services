package org.sagebionetworks.table.query.model;

/**
 * This matches &ltin predicate&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class InPredicate extends SQLElement implements HasPredicate {

	ReplaceableBox<ColumnReference> columnReferenceLHS;
	Boolean not;
	InPredicateValue inPredicateValue;
	
	public InPredicate(ColumnReference columnReferenceLHS, Boolean not,
			InPredicateValue inPredicateValue) {
		super();
		this.columnReferenceLHS = new ReplaceableBox<ColumnReference>(columnReferenceLHS);
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
		return columnReferenceLHS.getChild();
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		columnReferenceLHS.toSql(builder, parameters);
		builder.append(" ");
		if (this.not != null && this.not) {
			builder.append("NOT ");
		}
		builder.append("IN ( ");
		inPredicateValue.toSql(builder, parameters);
		builder.append(" )");
	}
	
	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(columnReferenceLHS, inPredicateValue);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS.getChild();
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return inPredicateValue.createIterable(UnsignedLiteral.class);
	}

}
