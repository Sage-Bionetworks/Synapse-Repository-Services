package org.sagebionetworks.table.query.model;

/**
 * This matches &ltin predicate&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class InPredicate extends SQLElement implements HasPredicate {

	PredicateLeftHandSide leftHandSide;
	Boolean not;
	InPredicateValue inPredicateValue;
	
	public InPredicate(PredicateLeftHandSide leftHandSide, Boolean not,
			InPredicateValue inPredicateValue) {
		super();
		this.leftHandSide = leftHandSide;
		this.not = not;
		this.inPredicateValue = inPredicateValue;
	}

	public Boolean getNot() {
		return not;
	}
	public InPredicateValue getInPredicateValue() {
		return inPredicateValue;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		leftHandSide.toSql(builder, parameters);
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
		return SQLElement.buildChildren(leftHandSide, inPredicateValue);
	}

	@Override
	public PredicateLeftHandSide getLeftHandSide() {
		return leftHandSide;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return inPredicateValue.createIterable(UnsignedLiteral.class);
	}

}
