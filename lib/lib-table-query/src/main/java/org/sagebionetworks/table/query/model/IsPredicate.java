package org.sagebionetworks.table.query.model;

/**
 * This matches &ltis predicate&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public abstract class IsPredicate extends SQLElement implements HasPredicate {

	ReplaceableBox<ColumnReference> columnReferenceLHS;
	Boolean not;

	public IsPredicate(ColumnReference columnReferenceLHS, Boolean not) {
		this.columnReferenceLHS = new ReplaceableBox<ColumnReference>(columnReferenceLHS);
		this.not = not;
	}

	public Boolean getNot() {
		return not;
	}

	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS.getChild();
	}

	public abstract String getCompareValue();

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		columnReferenceLHS.toSql(builder, parameters);
		builder.append(" IS ");
		if (not != null) {
			builder.append("NOT ");
		}
		builder.append(getCompareValue());
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS.getChild();
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return null;
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(columnReferenceLHS);
	}

}
