package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltis predicate&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public abstract class IsPredicate extends SQLElement implements HasPredicate {
	
	ColumnReference columnReferenceLHS;
	Boolean not;

	public IsPredicate(ColumnReference columnReferenceLHS, Boolean not) {
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
	}

	public Boolean getNot() {
		return not;
	}

	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
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
		return columnReferenceLHS;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return null;
	}
	
	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnReferenceLHS);
	}
	
}
