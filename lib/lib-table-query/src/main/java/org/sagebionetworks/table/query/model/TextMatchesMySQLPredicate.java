package org.sagebionetworks.table.query.model;

import org.sagebionetworks.repo.model.table.TableConstants;

/**
 * Replacement Predicate for {@link TextMatchesPredicate} that is used internally when translating to a MySQL query. Should not be exposed in the parser.
 */
public class TextMatchesMySQLPredicate extends SQLElement implements HasPredicate {

	private TextMatchesPredicate inputPredicate;
	
	public TextMatchesMySQLPredicate(TextMatchesPredicate inputPredicate) {
		this.inputPredicate = inputPredicate;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("MATCH(").append(TableConstants.ROW_SEARCH_CONTENT).append(") AGAINST(");
		inputPredicate.getValueExpression().toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return inputPredicate.getChildren();
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return inputPredicate.getLeftHandSide();
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return inputPredicate.getRightHandSideValues();
	}

}
