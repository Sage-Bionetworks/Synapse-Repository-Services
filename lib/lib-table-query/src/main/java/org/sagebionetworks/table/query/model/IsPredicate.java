package org.sagebionetworks.table.query.model;

/**
 * This matches &ltis predicate&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public abstract class IsPredicate extends SQLElement implements HasPredicate {

	PredicateLeftHandSide leftHandSide;
	Boolean not;

	public IsPredicate(PredicateLeftHandSide leftHandSide, Boolean not) {
		this.leftHandSide = leftHandSide;
		this.not = not;
	}

	public Boolean getNot() {
		return not;
	}

	public abstract String getCompareValue();

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		leftHandSide.toSql(builder, parameters);
		builder.append(" IS ");
		if (not != null) {
			builder.append("NOT ");
		}
		builder.append(getCompareValue());
	}

	@Override
	public PredicateLeftHandSide getLeftHandSide() {
		return leftHandSide;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return null;
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(leftHandSide);
	}

}
